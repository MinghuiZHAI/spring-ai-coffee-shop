package com.zmh.atlantic.coffee.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主单（表名 orders：order 为 MySQL 保留字，决策 #45）。
 * 状态流转唯一入口 = OrderService.transit（转换表校验 + @Version 乐观锁，详细设计 §2.2）。
 */
@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long storeId;

    /** 状态枚举名（OrderStatus 8 值，见详细设计 §2.1）。 */
    private String status;

    private String pickupMethod;

    private String pickupCode;

    /** 预计完成时间（催单降级查询的数据源，PRD 决策 #15）。 */
    private LocalDateTime expectedFinishTime;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;

    private Long userCouponId;

    private Integer rewardPoints;

    @Version
    private Integer version;

    private String cancelReason;

    private LocalDateTime paidAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
