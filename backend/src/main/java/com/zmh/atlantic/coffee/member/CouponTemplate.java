package com.zmh.atlantic.coffee.member;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 优惠券模板（MVP 仅满减类型）。 */
@Data
@TableName("coupon_template")
public class CouponTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String type;

    /** 使用门槛，0 = 无门槛。 */
    private BigDecimal thresholdAmount;

    private BigDecimal discountAmount;

    /** 领取后 N 天内有效。 */
    private Integer validDays;

    /** 发放总量，-1 不限。 */
    private Integer totalCount;

    private Integer status;

    @TableLogic
    private Integer deleted;
}
