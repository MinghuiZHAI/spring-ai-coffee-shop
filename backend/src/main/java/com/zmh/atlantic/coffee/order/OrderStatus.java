package com.zmh.atlantic.coffee.order;

import java.util.Map;

/**
 * 订单状态机（详细设计 §2.1；PRD 3.4 全量 8 节点 = 6 主状态 + 退款流转 2 态）。
 *
 * <p>转换表是"能不能取消/退款"类问答与 transit() 的唯一依据；写操作（取消/退款）
 * 永远不由 AI 执行，仅业务系统按此表放行（PRD 决策 #12）。
 * 取消后自动建全额退款单、24h 售后窗口等业务规则属于服务层钩子，不在状态机内。</p>
 */
public enum OrderStatus {

    PENDING_PAYMENT("待支付"),
    PAID_TODO("待制作"),
    MAKING("制作中"),
    READY("待取餐"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    REFUNDING("退款中"),
    REFUNDED("已退款");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 当前状态 × 操作 → 目标状态；终态映射为空表（详细设计 §2.1 转换表）。 */
    private static final Map<OrderStatus, Map<OrderAction, OrderStatus>> TRANSITIONS = Map.of(
            PENDING_PAYMENT, Map.of(
                    OrderAction.PAY, PAID_TODO,
                    OrderAction.CANCEL, CANCELLED),
            PAID_TODO, Map.of(
                    OrderAction.START_MAKING, MAKING,
                    OrderAction.CANCEL, CANCELLED),
            MAKING, Map.of(
                    OrderAction.FINISH_MAKING, READY),
            READY, Map.of(
                    OrderAction.PICKUP, COMPLETED),
            COMPLETED, Map.of(
                    OrderAction.APPLY_REFUND, REFUNDING),
            REFUNDING, Map.of(
                    OrderAction.REFUND_SUCCESS, REFUNDED),
            CANCELLED, Map.of(),
            REFUNDED, Map.of());

    /** 业务侧第一层防护：该状态是否允许此操作（详细设计 §2.2）。 */
    public boolean can(OrderAction action) {
        return TRANSITIONS.getOrDefault(this, Map.of()).containsKey(action);
    }

    /** 仅在 can(action) 通过后调用；防御性抛错对应"转换表与调用方不一致"的程序缺陷。 */
    public OrderStatus next(OrderAction action) {
        OrderStatus target = TRANSITIONS.getOrDefault(this, Map.of()).get(action);
        if (target == null) {
            throw new IllegalStateException("非法状态转换：" + this + " × " + action);
        }
        return target;
    }
}
