package com.zmh.atlantic.coffee.wallet;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包流水（审计源）：amount 有符号（正=入账 RECHARGE/REFUND，负=出账 CONSUME），
 * 与 point_record 的 change_value 口径一致；CONSUME 为后续版本预留，MVP 不产生数据。
 */
@Data
@TableName("wallet_transaction")
public class WalletTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String type;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    /** 关联订单（CONSUME/REFUND 用，MVP 预留 NULL）。 */
    private Long relatedOrderId;

    private String remark;

    private LocalDateTime createdAt;
}
