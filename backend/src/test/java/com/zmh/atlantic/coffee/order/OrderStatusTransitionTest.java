package com.zmh.atlantic.coffee.order;

import org.junit.jupiter.api.Test;

import static com.zmh.atlantic.coffee.order.OrderAction.APPLY_REFUND;
import static com.zmh.atlantic.coffee.order.OrderAction.CANCEL;
import static com.zmh.atlantic.coffee.order.OrderAction.FINISH_MAKING;
import static com.zmh.atlantic.coffee.order.OrderAction.PAY;
import static com.zmh.atlantic.coffee.order.OrderAction.PICKUP;
import static com.zmh.atlantic.coffee.order.OrderAction.REFUND_SUCCESS;
import static com.zmh.atlantic.coffee.order.OrderAction.START_MAKING;
import static com.zmh.atlantic.coffee.order.OrderStatus.CANCELLED;
import static com.zmh.atlantic.coffee.order.OrderStatus.COMPLETED;
import static com.zmh.atlantic.coffee.order.OrderStatus.MAKING;
import static com.zmh.atlantic.coffee.order.OrderStatus.PAID_TODO;
import static com.zmh.atlantic.coffee.order.OrderStatus.PENDING_PAYMENT;
import static com.zmh.atlantic.coffee.order.OrderStatus.READY;
import static com.zmh.atlantic.coffee.order.OrderStatus.REFUNDING;
import static com.zmh.atlantic.coffee.order.OrderStatus.REFUNDED;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 订单状态机转换表全覆盖（编码阶段必测项之一，详细设计 §2.1/§7）。 */
class OrderStatusTransitionTest {

    @Test
    void 待支付_可支付或取消_不可申请退款() {
        assertAll(
                () -> assertTrue(PENDING_PAYMENT.can(PAY)),
                () -> assertTrue(PENDING_PAYMENT.can(CANCEL)),
                () -> assertFalse(PENDING_PAYMENT.can(APPLY_REFUND)),
                () -> assertEquals(PAID_TODO, PENDING_PAYMENT.next(PAY)),
                () -> assertEquals(CANCELLED, PENDING_PAYMENT.next(CANCEL)));
    }

    @Test
    void 待制作_可开始制作或取消_取消后自动退款由服务层钩子触发() {
        assertAll(
                () -> assertTrue(PAID_TODO.can(START_MAKING)),
                () -> assertTrue(PAID_TODO.can(CANCEL)),
                () -> assertFalse(PAID_TODO.can(APPLY_REFUND)),
                () -> assertEquals(MAKING, PAID_TODO.next(START_MAKING)),
                () -> assertEquals(CANCELLED, PAID_TODO.next(CANCEL)));
    }

    @Test
    void 制作中_仅可完成制作_不可取消退款() {
        assertAll(
                () -> assertTrue(MAKING.can(FINISH_MAKING)),
                () -> assertFalse(MAKING.can(CANCEL)),
                () -> assertFalse(MAKING.can(APPLY_REFUND)),
                () -> assertEquals(READY, MAKING.next(FINISH_MAKING)));
    }

    @Test
    void 待取餐_仅可取餐核销() {
        assertAll(
                () -> assertTrue(READY.can(PICKUP)),
                () -> assertFalse(READY.can(CANCEL)),
                () -> assertEquals(COMPLETED, READY.next(PICKUP)));
    }

    @Test
    void 已完成_仅可申请退款_24h售后窗口由服务层校验() {
        assertAll(
                () -> assertTrue(COMPLETED.can(APPLY_REFUND)),
                () -> assertFalse(COMPLETED.can(CANCEL)),
                () -> assertEquals(REFUNDING, COMPLETED.next(APPLY_REFUND)));
    }

    @Test
    void 退款中_仅可退款到账() {
        assertAll(
                () -> assertTrue(REFUNDING.can(REFUND_SUCCESS)),
                () -> assertEquals(REFUNDED, REFUNDING.next(REFUND_SUCCESS)));
    }

    @Test
    void 终态_已取消与已退款_无任何可用操作() {
        for (OrderAction action : OrderAction.values()) {
            assertFalse(CANCELLED.can(action), "已取消不应允许操作：" + action);
            assertFalse(REFUNDED.can(action), "已退款不应允许操作：" + action);
        }
    }

    @Test
    void 非法转换_next_抛IllegalStateException() {
        assertThrows(IllegalStateException.class, () -> MAKING.next(CANCEL));
        assertThrows(IllegalStateException.class, () -> CANCELLED.next(PAY));
    }
}
