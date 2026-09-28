package com.zmh.atlantic.coffee.member;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /** 超时未支付自动取消的订单：归还所用优惠券（OrderSimulator 调用）。 */
    @Update("UPDATE user_coupon uc JOIN orders o ON o.id = uc.used_order_id " +
            "SET uc.status = 'UNUSED', uc.used_order_id = NULL, uc.used_at = NULL, uc.updated_at = NOW() " +
            "WHERE o.status = 'CANCELLED' AND o.cancel_reason = '超时未支付自动取消' AND uc.status = 'USED'")
    int restoreForTimeoutCancelled();
}
