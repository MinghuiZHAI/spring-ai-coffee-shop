package com.zmh.atlantic.coffee.member.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 会员域视图/请求结构（积分/优惠券，总体设计 §5.2 会员域契约）。 */
public final class MemberDtos {

    private MemberDtos() {
    }

    public record PointsView(Integer balance, List<PointItem> records) {
    }

    public record PointItem(LocalDateTime createdAt, Integer changeValue, String type, Long relatedOrderId) {
    }

    public record ClaimRequest(Long templateId) {
    }

    public record CouponView(Long id, String name, BigDecimal thresholdAmount, BigDecimal discountAmount,
                             LocalDateTime expireAt, String status) {
    }
}
