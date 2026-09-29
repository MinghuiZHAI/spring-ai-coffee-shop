package com.zmh.atlantic.coffee.wallet;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 钱包视图/请求结构（03 v1.4 追加接口契约草案）。 */
public final class WalletDtos {

    private WalletDtos() {
    }

    /** 余额视图。 */
    public record WalletView(BigDecimal balance) {
    }

    /** 流水项（游标分页，created_at 倒序）。 */
    public record WalletTransactionView(Long id, String type, BigDecimal amount, BigDecimal balanceAfter,
                                        Long relatedOrderId, String remark, LocalDateTime createdAt) {

        public static WalletTransactionView from(WalletTransaction tx) {
            return new WalletTransactionView(tx.getId(), tx.getType(), tx.getAmount(), tx.getBalanceAfter(),
                    tx.getRelatedOrderId(), tx.getRemark(), tx.getCreatedAt());
        }
    }

    /** 模拟充值请求：单次 1~1000 元，最多两位小数（01 v1.3 追加 MVP 范围）。 */
    public record RechargeRequest(
            @NotNull(message = "充值金额不能为空")
            @DecimalMin(value = "1.00", message = "单次充值金额需在 1~1000 元之间")
            @DecimalMax(value = "1000.00", message = "单次充值金额需在 1~1000 元之间")
            BigDecimal amount) {
    }
}
