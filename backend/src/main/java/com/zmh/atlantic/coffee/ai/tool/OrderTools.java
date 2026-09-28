package com.zmh.atlantic.coffee.ai.tool;

import com.zmh.atlantic.coffee.ai.log.ToolCallLogger;
import com.zmh.atlantic.coffee.ai.tool.ToolDtos.OrderBrief;
import com.zmh.atlantic.coffee.ai.tool.ToolDtos.PickupInfo;
import com.zmh.atlantic.coffee.ai.tool.ToolDtos.RefundProgress;
import com.zmh.atlantic.coffee.order.Order;
import com.zmh.atlantic.coffee.order.OrderService;
import com.zmh.atlantic.coffee.order.OrderStatus;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.OrderDetail;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.RefundView;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 订单域只读工具 ×3（详细设计 §3.3）：仅包装 OrderService 查询，用户身份一律取
 * ToolContext（决策 #53，模型回调线程 ThreadLocal 已失效、模型不可伪造身份）。
 */
@Component
@RequiredArgsConstructor
public class OrderTools {

    private final OrderService orderService;
    private final ToolCallLogger toolCallLogger;

    @Tool(description = "查询当前登录用户的订单列表，返回订单号、状态、金额、商品摘要、下单时间")
    public List<OrderBrief> queryMyOrders(
            ToolContext toolContext,
            @ToolParam(required = false, description = "可选：状态过滤（待支付/待制作/制作中/待取餐/已完成/已退款），不传查全部") String status,
            @ToolParam(required = false, description = "返回条数上限，默认 3，最大 10") Integer limit) {
        return toolCallLogger.record(toolContext, "queryMyOrders", ToolCallLogger.params("status", status, "limit", limit), () -> {
            long userId = currentUserId(toolContext);
            int capped = limit == null ? 3 : Math.min(limit, 10);
            String statusName = toStatusName(status);
            return orderService.list(userId, statusName, null, capped).list().stream()
                    .map(b -> new OrderBrief(b.orderNo(), b.status(), b.payAmount(), b.itemSummary(), b.createdAt()))
                    .toList();
        });
    }

    @Tool(description = "查询指定订单的取餐码、当前状态与预计完成时间")
    public PickupInfo queryEstimatedTime(
            ToolContext toolContext,
            @ToolParam(description = "订单号，如 AC202609270001") String orderNo) {
        return toolCallLogger.record(toolContext, "queryEstimatedTime", ToolCallLogger.params("orderNo", orderNo), () -> {
            long userId = currentUserId(toolContext);
            Order order = orderService.requireOwnedByOrderNo(userId, orderNo);
            return new PickupInfo(order.getOrderNo(), OrderStatus.valueOf(order.getStatus()).label(),
                    order.getPickupCode(), order.getExpectedFinishTime());
        });
    }

    @Tool(description = "查询指定订单的退款进度，返回退款单号、状态与金额")
    public RefundProgress queryRefundProgress(
            ToolContext toolContext,
            @ToolParam(description = "订单号") String orderNo) {
        return toolCallLogger.record(toolContext, "queryRefundProgress", ToolCallLogger.params("orderNo", orderNo), () -> {
            long userId = currentUserId(toolContext);
            Order order = orderService.requireOwnedByOrderNo(userId, orderNo);
            RefundView refund = orderService.latestRefund(order.getId());
            if (refund == null) {
                return new RefundProgress(order.getOrderNo(), null, "无退款记录", null, null, null);
            }
            return new RefundProgress(order.getOrderNo(), refund.refundNo(), refund.status(),
                    refund.amount(), refund.applyAt(), refund.finishedAt());
        });
    }

    // ===== 内部 =====

    private long currentUserId(ToolContext toolContext) {
        Object v = toolContext.getContext().get("userId");
        if (v == null) {
            throw new com.zmh.atlantic.coffee.common.exception.BizException(40301, "缺少用户上下文");
        }
        return ((Number) v).longValue();
    }

    /** 状态中文标签 → 枚举名（订单状态以枚举名落库）；非法标签按无过滤处理。 */
    private String toStatusName(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        return Arrays.stream(OrderStatus.values())
                .filter(s -> s.label().equals(label.trim()) || s.name().equals(label.trim()))
                .map(OrderStatus::name).findFirst().orElse(null);
    }
}
