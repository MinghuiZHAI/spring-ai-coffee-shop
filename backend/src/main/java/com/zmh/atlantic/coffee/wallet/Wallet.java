package com.zmh.atlantic.coffee.wallet;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包账户（03 v1.4 追加 / 04 v1.6 §8.2）：每用户有且仅有一条（uk_wallet_user）。
 * 余额以本表 balance 为准、wallet_transaction 为审计源；并发充值走行锁
 * （SELECT ... FOR UPDATE，WalletService.recharge），version 随更新递增供审计。
 */
@Data
@TableName("wallet_account")
public class Wallet {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private BigDecimal balance;

    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
