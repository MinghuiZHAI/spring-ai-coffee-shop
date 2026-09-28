package com.zmh.atlantic.coffee.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单（状态：APPLYING/APPROVED/SUCCESS/REJECTED）。
 * AI 只做查询与规则解答，发起仅来自用户订单页（apply_channel=USER_PAGE，PRD 决策 #12）。
 * 到账节奏：APPLYING →(T+30s) APPROVED →(T+60s) SUCCESS，由 RefundScheduler 驱动（决策 #47）。
 */
@Data
@TableName("refund")
public class Refund {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private Long orderId;

    private Long userId;

    private BigDecimal amount;

    private String reason;

    private String status;

    private String applyChannel;

    private LocalDateTime applyAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
