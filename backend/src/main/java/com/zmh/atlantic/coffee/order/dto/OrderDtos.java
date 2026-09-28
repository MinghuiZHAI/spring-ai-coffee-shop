package com.zmh.atlantic.coffee.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 订单视图/请求结构（总体设计 §5.2 订单域 + §5.4 示例契约）。 */
public final class OrderDtos {

    private OrderDtos() {
    }

    public record CreateOrderRequest(Long storeId, String pickupMethod,
                                     List<Long> cartItemIds, Long userCouponId) {
    }

    public record OrderCreatedResponse(Long orderId, String orderNo, String status,
                                       BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal payAmount,
                                       LocalDateTime expireAt) {
    }

    public record OrderBrief(Long id, String orderNo, String storeName, String status,
                             BigDecimal payAmount, String itemSummary, LocalDateTime createdAt) {
    }

    public record OrderItemView(Long productId, String productName, String specSnapshot,
                                BigDecimal unitPrice, Integer quantity, BigDecimal lineAmount) {
    }

    public record RefundView(String refundNo, BigDecimal amount, String status,
                             LocalDateTime applyAt, LocalDateTime finishedAt) {
    }

    public record OrderDetail(Long id, String orderNo, String storeName, String status, String cancelReason,
                              String pickupMethod, String pickupCode, LocalDateTime expectedFinishTime,
                              BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal payAmount,
                              Integer rewardPoints, List<OrderItemView> items, RefundView refund,
                              LocalDateTime createdAt) {
    }

    /** 模拟支付响应（总体设计 §5.4）。 */
    public record PayResponse(Long orderId, String orderNo, String status, String pickupCode) {
    }

    public record ApplyRefundRequest(String reason) {
    }

    public record CancelOrderRequest(String reason) {
    }
}
