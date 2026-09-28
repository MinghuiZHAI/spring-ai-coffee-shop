package com.zmh.atlantic.coffee.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmh.atlantic.coffee.auth.UserContext;
import com.zmh.atlantic.coffee.cart.CartItem;
import com.zmh.atlantic.coffee.cart.CartItemMapper;
import com.zmh.atlantic.coffee.common.exception.BizException;
import com.zmh.atlantic.coffee.common.web.CursorPage;
import com.zmh.atlantic.coffee.common.web.ResultCode;
import com.zmh.atlantic.coffee.member.CouponPolicy;
import com.zmh.atlantic.coffee.member.CouponService;
import com.zmh.atlantic.coffee.member.CouponTemplate;
import com.zmh.atlantic.coffee.member.CouponTemplateMapper;
import com.zmh.atlantic.coffee.member.PointService;
import com.zmh.atlantic.coffee.member.UserCoupon;
import com.zmh.atlantic.coffee.member.UserCouponMapper;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.ApplyRefundRequest;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.CreateOrderRequest;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.OrderBrief;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.OrderCreatedResponse;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.OrderDetail;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.OrderItemView;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.PayResponse;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.RefundView;
import com.zmh.atlantic.coffee.product.Product;
import com.zmh.atlantic.coffee.product.ProductMapper;
import com.zmh.atlantic.coffee.store.Store;
import com.zmh.atlantic.coffee.store.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单服务：状态流转唯一入口 = transit（转换表校验 + @Version 乐观锁，详细设计 §2.2）。
 * 写操作（支付/取消/退款）全部由用户在业务系统发起，AI 只读（PRD 决策 #12）。
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int PAY_TIMEOUT_MINUTES = 30;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final StoreMapper storeMapper;
    private final ProductMapper productMapper;
    private final CartItemMapper cartItemMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final PointService pointService;
    private final CouponService couponService;
    private final RefundService refundService;
    private final StringRedisTemplate redis;

    // ===== 创建订单 =====

    @Transactional
    public OrderCreatedResponse create(Long userId, CreateOrderRequest request) {
        if (request.cartItemIds() == null || request.cartItemIds().isEmpty()) {
            throw new BizException(ResultCode.PARAM_INVALID, "购物车为空");
        }
        Store store = storeMapper.selectById(request.storeId());
        if (store == null || store.getStatus() != 1) {
            throw new BizException(ResultCode.NOT_FOUND, "门店不存在");
        }
        List<CartItem> items = cartItemMapper.selectBatchIds(request.cartItemIds()).stream()
                .filter(item -> item.getUserId().equals(userId)).toList();
        if (items.size() != request.cartItemIds().size()) {
            throw new BizException(ResultCode.FORBIDDEN, "购物车项不存在");
        }
        Map<Long, Product> products = productMapper.selectBatchIds(
                        items.stream().map(CartItem::getProductId).distinct().toList()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new java.util.ArrayList<>();
        for (CartItem item : items) {
            Product product = products.get(item.getProductId());
            if (product == null || product.getStatus() != 1) {
                throw new BizException(ResultCode.PARAM_INVALID, "商品已下架：" + item.getProductId());
            }
            BigDecimal unitPrice = product.getBasePrice().add(item.getSpecPriceDelta());
            BigDecimal lineAmount = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(lineAmount);
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setSpecSnapshot(item.getSpecSnapshot());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setLineAmount(lineAmount);
            orderItems.add(orderItem);
        }

        BigDecimal discount = BigDecimal.ZERO;
        UserCoupon coupon = null;
        if (request.userCouponId() != null) {
            coupon = userCouponMapper.selectById(request.userCouponId());
            if (coupon == null || !coupon.getUserId().equals(userId)) {
                throw new BizException(ResultCode.FORBIDDEN, "优惠券不存在");
            }
            CouponTemplate template = couponTemplateMapper.selectById(coupon.getTemplateId());
            discount = CouponPolicy.checkUsable(coupon, template, total);
        }
        BigDecimal payAmount = total.subtract(discount).max(BigDecimal.ZERO);

        Order order = new Order();
        order.setOrderNo(genOrderNo());
        order.setUserId(userId);
        order.setStoreId(request.storeId());
        order.setStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setPickupMethod(request.pickupMethod() == null ? "STORE_PICKUP" : request.pickupMethod());
        order.setTotalAmount(total);
        order.setDiscountAmount(discount);
        order.setPayAmount(payAmount);
        order.setUserCouponId(request.userCouponId());
        order.setRewardPoints(0);
        orderMapper.insert(order);
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
            orderItemMapper.insert(orderItem);
        }
        if (coupon != null) {
            couponService.markUsed(coupon, order.getId());
        }
        cartItemMapper.deleteBatchIds(request.cartItemIds());
        return new OrderCreatedResponse(order.getId(), order.getOrderNo(),
                OrderStatus.PENDING_PAYMENT.label(), total, discount, payAmount,
                LocalDateTime.now().plusMinutes(PAY_TIMEOUT_MINUTES));
    }

    // ===== 状态流转唯一入口 =====

    @Transactional
    public Order transit(Long orderId, OrderAction action) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return transit(order, action);
    }

    private Order transit(Order order, OrderAction action) {
        OrderStatus current = OrderStatus.valueOf(order.getStatus());
        if (!current.can(action)) {
            throw new BizException(ResultCode.STATE_CONFLICT,
                    "当前订单状态不允许该操作：" + current.label());
        }
        OrderStatus target = current.next(action);
        order.setStatus(target.name());
        int reward = 0;
        if (action == OrderAction.PAY) {
            order.setPaidAt(LocalDateTime.now());
            order.setPickupCode(String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
            order.setExpectedFinishTime(LocalDateTime.now().plusMinutes(4));
            reward = order.getPayAmount().intValue() * 10;      // 实付 1 元 = 10 分
            order.setRewardPoints(reward);
        }
        if (action == OrderAction.PICKUP) {
            order.setCompletedAt(LocalDateTime.now());
        }
        int rows = orderMapper.updateById(order);      // UPDATE ... WHERE id=? AND version=?（乐观锁）
        if (rows == 0) {
            throw new BizException(ResultCode.STATE_CONFLICT, "订单状态已被其他操作变更，请刷新后重试");
        }
        // 动作后置钩子（详细设计 §2.2）
        if (action == OrderAction.PAY) {
            pointService.earn(order.getUserId(), reward, order.getId());
        }
        if (action == OrderAction.CANCEL && order.getPaidAt() != null) {
            refundService.createFullRefund(order);     // 待制作取消 → 自动全额退款单（APPLYING）
        }
        return order;
    }

    // ===== 模拟支付 =====

    @Transactional
    public PayResponse pay(Long userId, Long orderId) {
        Order order = getOwn(userId, orderId);
        transit(order, OrderAction.PAY);
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setPayNo("PAY" + order.getOrderNo().substring(2));
        payment.setChannel("MOCK");
        payment.setAmount(order.getPayAmount());
        payment.setStatus("SUCCESS");
        payment.setPaidAt(LocalDateTime.now());
        paymentMapper.insert(payment);
        return new PayResponse(order.getId(), order.getOrderNo(),
                OrderStatus.valueOf(order.getStatus()).label(), order.getPickupCode());
    }

    // ===== 确认取餐（用户核销，状态机 PICKUP 动作）=====

    @Transactional
    public PayResponse pickup(Long userId, Long orderId) {
        Order order = getOwn(userId, orderId);
        transit(order, OrderAction.PICKUP);
        return new PayResponse(order.getId(), order.getOrderNo(),
                OrderStatus.valueOf(order.getStatus()).label(), order.getPickupCode());
    }

    // ===== 取消 =====

    @Transactional
    public void cancel(Long userId, Long orderId, String reason) {
        Order order = getOwn(userId, orderId);
        order.setCancelReason(reason == null || reason.isBlank() ? "用户主动取消" : reason);
        transit(order, OrderAction.CANCEL);
        couponService.restore(order.getUserCouponId());
    }

    // ===== 退款申请（已完成订单 24h 内，订单页自助）=====

    @Transactional
    public RefundView applyRefund(Long userId, Long orderId, String reason) {
        Order order = getOwn(userId, orderId);
        if (order.getCompletedAt() == null || order.getCompletedAt().isBefore(LocalDateTime.now().minusHours(24))) {
            throw new BizException(ResultCode.STATE_CONFLICT, "仅已完成订单在 24 小时内可申请退款");
        }
        Long exists = refundMapper.selectCount(new LambdaQueryWrapper<Refund>().eq(Refund::getOrderId, orderId));
        if (exists > 0) {
            throw new BizException(ResultCode.STATE_CONFLICT, "该订单已存在退款记录");
        }
        transit(order, OrderAction.APPLY_REFUND);
        Refund refund = new Refund();
        refund.setRefundNo("RF" + LocalDate.now().format(DATE) + String.format("%06d", nextSeq("refund")));
        refund.setOrderId(order.getId());
        refund.setUserId(userId);
        refund.setAmount(order.getPayAmount());
        refund.setReason(reason == null || reason.isBlank() ? "用户申请退款" : reason);
        refund.setStatus("APPLYING");
        refund.setApplyChannel("USER_PAGE");
        refund.setApplyAt(LocalDateTime.now());
        refundMapper.insert(refund);
        return new RefundView(refund.getRefundNo(), refund.getAmount(), refund.getStatus(),
                refund.getApplyAt(), refund.getFinishedAt());
    }

    // ===== 查询 =====

    /**
     * 按订单号查询本人订单（AI 工具 queryEstimatedTime/queryRefundProgress 的入口，
     * 详细设计 §3.3）：订单号全局唯一，越权防线在归属校验闭合（40401 不泄露存在性差异）。
     */
    public Order requireOwnedByOrderNo(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    /** 指定订单的最近一笔退款单（退款进度查询，无退款单返回 null）。 */
    public RefundView latestRefund(Long orderId) {
        return refundMapper.selectList(new LambdaQueryWrapper<Refund>()
                        .eq(Refund::getOrderId, orderId).orderByDesc(Refund::getId).last("LIMIT 1"))
                .stream().findFirst()
                .map(r -> new RefundView(r.getRefundNo(), r.getAmount(), r.getStatus(),
                        r.getApplyAt(), r.getFinishedAt()))
                .orElse(null);
    }

    public CursorPage<OrderBrief> list(Long userId, String status, Long cursor, int limit) {
        var wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .lt(cursor != null, Order::getId, cursor)
                .orderByDesc(Order::getId)
                .last("LIMIT " + Math.min(limit, 50));
        if (status != null && !status.isBlank()) {
            wrapper.eq(Order::getStatus, status);
        }
        List<Order> orders = orderMapper.selectList(wrapper);
        List<OrderBrief> briefs = assembleBriefs(orders);
        Long nextCursor = orders.size() == Math.min(limit, 50) && !orders.isEmpty()
                ? orders.get(orders.size() - 1).getId() : null;
        return new CursorPage<>(briefs, nextCursor);
    }

    public OrderDetail detail(Long userId, Long orderId) {
        Order order = getOwn(userId, orderId);
        Store store = storeMapper.selectById(order.getStoreId());
        List<OrderItemView> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId).orderByAsc(OrderItem::getId)).stream()
                .map(i -> new OrderItemView(i.getProductId(), i.getProductName(), i.getSpecSnapshot(),
                        i.getUnitPrice(), i.getQuantity(), i.getLineAmount()))
                .toList();
        Refund latest = refundMapper.selectList(new LambdaQueryWrapper<Refund>()
                        .eq(Refund::getOrderId, orderId).orderByDesc(Refund::getId).last("LIMIT 1"))
                .stream().findFirst().orElse(null);
        RefundView refund = latest == null ? null : new RefundView(latest.getRefundNo(),
                latest.getAmount(), latest.getStatus(), latest.getApplyAt(), latest.getFinishedAt());
        return new OrderDetail(order.getId(), order.getOrderNo(),
                store != null ? store.getName() : null,
                OrderStatus.valueOf(order.getStatus()).label(), order.getCancelReason(),
                order.getPickupMethod(), order.getPickupCode(), order.getExpectedFinishTime(),
                order.getTotalAmount(), order.getDiscountAmount(), order.getPayAmount(),
                order.getRewardPoints(), items, refund, order.getCreatedAt());
    }

    // ===== 内部 =====

    private List<OrderBrief> assembleBriefs(List<Order> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, String> storeNames = storeMapper.selectBatchIds(
                        orders.stream().map(Order::getStoreId).distinct().toList()).stream()
                .collect(Collectors.toMap(Store::getId, Store::getName));
        Map<Long, List<OrderItem>> itemsByOrder = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId,
                                orders.stream().map(Order::getId).toList())).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        return orders.stream().map(o -> {
            String summary = itemsByOrder.getOrDefault(o.getId(), List.of()).stream()
                    .map(i -> i.getProductName() + "×" + i.getQuantity())
                    .collect(Collectors.joining("、"));
            return new OrderBrief(o.getId(), o.getOrderNo(), storeNames.get(o.getStoreId()),
                    OrderStatus.valueOf(o.getStatus()).label(), o.getPayAmount(), summary, o.getCreatedAt());
        }).toList();
    }

    private Order getOwn(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private String genOrderNo() {
        String date = LocalDate.now().format(DATE);
        String key = "seq:order:" + date;
        Long seq = redis.opsForValue().increment(key);
        if (seq != null && seq == 1L) {
            redis.expire(key, Duration.ofDays(2));
        }
        return "AC" + date + String.format("%06d", seq);
    }

    private Long nextSeq(String prefix) {
        String date = LocalDate.now().format(DATE);
        String key = "seq:" + prefix + ":" + date;
        Long seq = redis.opsForValue().increment(key);
        if (seq != null && seq == 1L) {
            redis.expire(key, Duration.ofDays(2));
        }
        return seq;
    }
}
