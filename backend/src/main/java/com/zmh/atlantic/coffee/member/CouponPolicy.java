package com.zmh.atlantic.coffee.member;

import com.zmh.atlantic.coffee.common.exception.BizException;
import com.zmh.atlantic.coffee.common.web.ResultCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券使用策略（纯函数，无依赖，可单测——详细设计 §3.3 CouponPolicy）。
 * 校验顺序：状态 → 有效期 → 门槛；通过返回面额。
 */
public final class CouponPolicy {

    private CouponPolicy() {
    }

    public static BigDecimal checkUsable(UserCoupon userCoupon, CouponTemplate template, BigDecimal totalAmount) {
        if (!"UNUSED".equals(userCoupon.getStatus())) {
            throw new BizException(ResultCode.PARAM_INVALID, "优惠券不可用（已使用或已过期）");
        }
        if (userCoupon.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.PARAM_INVALID, "优惠券已过期");
        }
        if (totalAmount.compareTo(template.getThresholdAmount()) < 0) {
            throw new BizException(ResultCode.PARAM_INVALID,
                    "未满足券使用门槛：满 " + template.getThresholdAmount().toPlainString() + " 元可用");
        }
        return template.getDiscountAmount();
    }
}
