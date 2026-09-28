package com.zmh.atlantic.coffee.member;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmh.atlantic.coffee.common.exception.BizException;
import com.zmh.atlantic.coffee.common.web.ResultCode;
import com.zmh.atlantic.coffee.member.dto.MemberDtos.ClaimRequest;
import com.zmh.atlantic.coffee.member.dto.MemberDtos.CouponView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 优惠券：领取/列表/下单占用/取消归还。 */
@Service
@RequiredArgsConstructor
public class CouponService {

    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;

    public List<CouponView> list(Long userId, String status) {
        var wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId).orderByDesc(UserCoupon::getId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(UserCoupon::getStatus, status);
        }
        List<UserCoupon> coupons = userCouponMapper.selectList(wrapper);
        Map<Long, CouponTemplate> templates = coupons.isEmpty() ? Map.of()
                : couponTemplateMapper.selectBatchIds(coupons.stream().map(UserCoupon::getTemplateId).distinct().toList())
                        .stream().collect(Collectors.toMap(CouponTemplate::getId, Function.identity()));
        return coupons.stream().map(uc -> {
            CouponTemplate t = templates.get(uc.getTemplateId());
            return new CouponView(uc.getId(), t != null ? t.getName() : "（券模板已下架）",
                    t != null ? t.getThresholdAmount() : null, t != null ? t.getDiscountAmount() : null,
                    uc.getExpireAt(), uc.getStatus());
        }).toList();
    }

    public CouponView claim(Long userId, ClaimRequest request) {
        CouponTemplate template = couponTemplateMapper.selectById(request.templateId());
        if (template == null || template.getStatus() != 1) {
            throw new BizException(ResultCode.NOT_FOUND, "券活动不存在或已结束");
        }
        if (template.getTotalCount() >= 0) {
            Long claimed = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getTemplateId, template.getId()));
            if (claimed >= template.getTotalCount()) {
                throw new BizException(ResultCode.PARAM_INVALID, "该券已被领完");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        UserCoupon coupon = new UserCoupon();
        coupon.setUserId(userId);
        coupon.setTemplateId(template.getId());
        coupon.setStatus("UNUSED");
        coupon.setReceivedAt(now);
        coupon.setExpireAt(now.plusDays(template.getValidDays()));
        userCouponMapper.insert(coupon);
        return new CouponView(coupon.getId(), template.getName(), template.getThresholdAmount(),
                template.getDiscountAmount(), coupon.getExpireAt(), coupon.getStatus());
    }

    /** 创建订单时占用券（USED）；取消/超时取消由 restore 归还。 */
    public void markUsed(UserCoupon coupon, Long orderId) {
        coupon.setStatus("USED");
        coupon.setUsedOrderId(orderId);
        coupon.setUsedAt(LocalDateTime.now());
        userCouponMapper.updateById(coupon);
    }

    /** 取消订单归还券（幂等：非 USED 状态不动）。 */
    public void restore(Long userCouponId) {
        if (userCouponId == null) {
            return;
        }
        UserCoupon coupon = userCouponMapper.selectById(userCouponId);
        if (coupon != null && "USED".equals(coupon.getStatus())) {
            coupon.setStatus("UNUSED");
            coupon.setUsedOrderId(null);
            coupon.setUsedAt(null);
            userCouponMapper.updateById(coupon);
        }
    }
}
