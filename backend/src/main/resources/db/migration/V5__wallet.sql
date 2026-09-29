-- V5__wallet.sql · 钱包（03 v1.4 追加：wallet → user 单向依赖；MVP 仅余额展示/模拟充值/查流水）
CREATE TABLE wallet_account (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT UNSIGNED NOT NULL COMMENT '所属用户（每用户有且仅有一条）',
    balance    DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '余额（元），以本字段为准、流水为审计源',
    version    INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（并发充值/扣减）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_wallet_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包账户（每用户一条，乐观锁）';

CREATE TABLE wallet_transaction (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT UNSIGNED NOT NULL,
    type             VARCHAR(20) NOT NULL COMMENT 'RECHARGE/CONSUME/REFUND/MANUAL（CONSUME 后续版本预留）',
    amount           DECIMAL(10, 2) NOT NULL COMMENT '变动金额：正=入账（RECHARGE/REFUND），负=出账（CONSUME），与 point_record 口径一致',
    balance_after    DECIMAL(10, 2) NOT NULL COMMENT '变动后余额',
    related_order_id BIGINT UNSIGNED NULL COMMENT '关联订单（CONSUME/REFUND 用，MVP 预留 NULL）',
    remark           VARCHAR(200) NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_wallet_tx_user_created (user_id, created_at),
    KEY idx_wallet_tx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包流水（审计源）';

-- 种子：演示用户张三（user_id=2，V2 种子 13800000001）余额 128.50 + 一条 RECHARGE 流水
INSERT INTO wallet_account (user_id, balance, version) VALUES (2, 128.50, 1);

INSERT INTO wallet_transaction (user_id, type, amount, balance_after, related_order_id, remark) VALUES
(2, 'RECHARGE', 128.50, 128.50, NULL, '演示充值');
