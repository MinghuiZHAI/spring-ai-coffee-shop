package com.zmh.atlantic.coffee.member;

import com.zmh.atlantic.coffee.common.exception.BizException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 优惠券使用策略单测（门槛/有效期/状态）。 */
class CouponPolicyTest {

    private UserCoupon coupon(String status, LocalDateTime expireAt) {
        UserCoupon uc = new UserCoupon();
        uc.setStatus(status);
        uc.setExpireAt(expireAt);
        return uc;
    }

    private CouponTemplate template(String threshold, String discount) {
        CouponTemplate t = new CouponTemplate();
        t.setThresholdAmount(new BigDecimal(threshold));
        t.setDiscountAmount(new BigDecimal(discount));
        return t;
    }

    @Test
    void 未使用且未过期且满足门槛_返回面额() {
        var uc = coupon("UNUSED", LocalDateTime.now().plusDays(1));
        var t = template("20", "5");
        assertEquals(new BigDecimal("5"), CouponPolicy.checkUsable(uc, t, new BigDecimal("25")));
    }

    @Test
    void 无门槛券_任意金额可用() {
        var uc = coupon("UNUSED", LocalDateTime.now().plusDays(1));
        var t = template("0", "3");
        assertEquals(new BigDecimal("3"), CouponPolicy.checkUsable(uc, t, new BigDecimal("1")));
    }

    @Test
    void 未满足门槛_抛业务异常() {
        var uc = coupon("UNUSED", LocalDateTime.now().plusDays(1));
        var t = template("20", "5");
        assertThrows(BizException.class, () -> CouponPolicy.checkUsable(uc, t, new BigDecimal("19.99")));
    }

    @Test
    void 已过期_抛业务异常() {
        var uc = coupon("UNUSED", LocalDateTime.now().minusMinutes(1));
        var t = template("0", "3");
        assertThrows(BizException.class, () -> CouponPolicy.checkUsable(uc, t, new BigDecimal("30")));
    }

    @Test
    void 已使用_抛业务异常() {
        var uc = coupon("USED", LocalDateTime.now().plusDays(1));
        var t = template("0", "3");
        assertThrows(BizException.class, () -> CouponPolicy.checkUsable(uc, t, new BigDecimal("30")));
    }
}
