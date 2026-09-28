package com.zmh.atlantic.coffee.order;

/** 订单状态机的操作集（详细设计 §2.1 转换表的列维度）。 */
public enum OrderAction {
    PAY,
    START_MAKING,
    FINISH_MAKING,
    PICKUP,
    CANCEL,
    APPLY_REFUND,
    REFUND_SUCCESS
}
