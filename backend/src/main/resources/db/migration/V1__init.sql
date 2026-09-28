-- 1. 用户（全角色共用，role 三值：USER/ADMIN/AGENT，AGENT 为 M3 预留）
CREATE TABLE user (
                      id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                      phone         VARCHAR(20)  NOT NULL COMMENT '手机号',
                      password      VARCHAR(100) NOT NULL COMMENT 'BCrypt 摘要',
                      nickname      VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
                      role          VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER/ADMIN/AGENT(预留)',
                      member_level  TINYINT      NOT NULL DEFAULT 1 COMMENT '会员等级 1/2/3',
                      status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 0 禁用',
                      deleted       TINYINT      NOT NULL DEFAULT 0,
                      created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      UNIQUE KEY uk_phone (phone, deleted) COMMENT '逻辑删除后允许复用手机号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户';

-- 2. 商品分类
CREATE TABLE category (
                          id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                          name VARCHAR(50) NOT NULL COMMENT '分类名',
                          sort INT NOT NULL DEFAULT 0,
                          status TINYINT NOT NULL DEFAULT 1 COMMENT '1 上架 0 下架',
                          deleted TINYINT NOT NULL DEFAULT 0,
                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分类';

-- 3. 商品（tags 供规则化推荐筛选，技术栈 §12.2）
CREATE TABLE product (
                         id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                         category_id  BIGINT UNSIGNED NOT NULL,
                         name         VARCHAR(100) NOT NULL,
                         description  VARCHAR(500) NOT NULL DEFAULT '',
                         base_price   DECIMAL(10,2) NOT NULL COMMENT '基础价（不含规格差价）',
                         image_url    VARCHAR(255) NOT NULL DEFAULT '',
                         tags         VARCHAR(200) NOT NULL DEFAULT '' COMMENT '口味标签，逗号分隔：奶香,微甜,含咖啡因',
                         status       TINYINT NOT NULL DEFAULT 1 COMMENT '1 上架 0 下架',
                         deleted      TINYINT NOT NULL DEFAULT 0,
                         created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         KEY idx_category (category_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品';

-- 4. 规格选项（温度/糖度/冰度字典，含差价）
CREATE TABLE spec_option (
                             id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                             spec_group  VARCHAR(20) NOT NULL COMMENT 'TEMPERATURE/SWEETNESS/ICE',
                             option_name VARCHAR(20) NOT NULL COMMENT '如：冰 / 半糖 / 去冰',
                             price_delta DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '规格差价',
                             sort        INT NOT NULL DEFAULT 0,
                             status      TINYINT NOT NULL DEFAULT 1,
                             created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             KEY idx_group (spec_group, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='规格选项';

-- 5. 购物车项（规格快照随下单固化进订单明细）
CREATE TABLE cart_item (
                           id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                           user_id          BIGINT UNSIGNED NOT NULL,
                           product_id       BIGINT UNSIGNED NOT NULL,
                           spec_snapshot    VARCHAR(200) NOT NULL COMMENT '规格快照 JSON：{"TEMPERATURE":"冰","SWEETNESS":"半糖"}',
                           spec_price_delta DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '快照差价合计',
                           quantity         INT NOT NULL DEFAULT 1,
                           created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车项';

-- 6. 门店
CREATE TABLE store (
                       id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                       name           VARCHAR(100) NOT NULL,
                       address        VARCHAR(200) NOT NULL,
                       business_hours VARCHAR(100) NOT NULL COMMENT '如 07:30-22:00',
                       phone          VARCHAR(20)  NOT NULL DEFAULT '',
                       status         TINYINT NOT NULL DEFAULT 1,
                       deleted        TINYINT NOT NULL DEFAULT 0,
                       created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门店';

-- 7. 订单（order 为保留字故命名 orders；8 值状态枚举见 §2.1；version 乐观锁）
CREATE TABLE orders (
                        id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                        order_no            VARCHAR(32) NOT NULL COMMENT 'ACyyyyMMdd+6位序列（Redis INCR 日键）',
                        user_id             BIGINT UNSIGNED NOT NULL,
                        store_id            BIGINT UNSIGNED NOT NULL,
                        status              VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '见 §2.1 状态枚举',
                        pickup_method       VARCHAR(20) NOT NULL DEFAULT 'STORE_PICKUP' COMMENT 'MVP 仅自取',
                        pickup_code         VARCHAR(10) NULL COMMENT '取餐码，支付成功时生成',
                        expected_finish_time DATETIME NULL COMMENT '预计完成时间（催单降级查询的数据源，PRD 决策 #15）',
                        total_amount        DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '原价合计',
                        discount_amount     DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '券抵扣',
                        pay_amount          DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '实付',
                        user_coupon_id      BIGINT UNSIGNED NULL,
                        reward_points       INT NOT NULL DEFAULT 0 COMMENT '本单获得积分（实付1元=10分）',
                        version             INT NOT NULL DEFAULT 0 COMMENT '乐观锁（@Version）',
                        cancel_reason       VARCHAR(200) NULL,
                        paid_at             DATETIME NULL,
                        completed_at        DATETIME NULL COMMENT '取餐核销时间（退款 24h 窗口起点）',
                        created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        UNIQUE KEY uk_order_no (order_no),
                        KEY idx_user_status_created (user_id, status, created_at),
                        KEY idx_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单主表';

-- 8. 订单明细（商品与规格快照）
CREATE TABLE order_item (
                            id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                            order_id      BIGINT UNSIGNED NOT NULL,
                            product_id    BIGINT UNSIGNED NOT NULL,
                            product_name  VARCHAR(100) NOT NULL COMMENT '商品名快照',
                            spec_snapshot VARCHAR(200) NOT NULL,
                            unit_price    DECIMAL(10,2) NOT NULL COMMENT '单价=基础价+规格差价',
                            quantity      INT NOT NULL DEFAULT 1,
                            line_amount   DECIMAL(10,2) NOT NULL,
                            created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单明细';

-- 9. 支付记录（模拟支付流水）
CREATE TABLE payment (
                         id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                         order_id   BIGINT UNSIGNED NOT NULL,
                         pay_no     VARCHAR(32) NOT NULL,
                         channel    VARCHAR(20) NOT NULL DEFAULT 'MOCK',
                         amount     DECIMAL(10,2) NOT NULL,
                         status     VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED',
                         paid_at    DATETIME NULL,
                         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         UNIQUE KEY uk_pay_no (pay_no),
                         KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付记录';

-- 10. 退款单（状态：APPLYING 申请中/APPROVED 已通过(退款中)/SUCCESS 已到账/REJECTED 已拒绝；
--     MVP 模拟自动通过无 REJECT 路径，枚举保留）
CREATE TABLE refund (
                        id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                        refund_no      VARCHAR(32) NOT NULL COMMENT 'RFyyyyMMdd+6位序列',
                        order_id       BIGINT UNSIGNED NOT NULL,
                        user_id        BIGINT UNSIGNED NOT NULL COMMENT '冗余，便于"我的退款"查询',
                        amount         DECIMAL(10,2) NOT NULL,
                        reason         VARCHAR(200) NOT NULL DEFAULT '',
                        status         VARCHAR(20) NOT NULL DEFAULT 'APPLYING',
                        apply_channel  VARCHAR(20) NOT NULL DEFAULT 'USER_PAGE' COMMENT '仅用户订单页，AI 不发起（PRD 决策 #12）',
                        apply_at       DATETIME NOT NULL COMMENT '申请时间（30s 节奏的扫描基准）',
                        finished_at    DATETIME NULL,
                        created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        UNIQUE KEY uk_refund_no (refund_no),
                        KEY idx_order (order_id),
                        KEY idx_status_apply (status, apply_at) COMMENT '退款延迟到账扫描专用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款单';

-- 11. 优惠券模板
CREATE TABLE coupon_template (
                                 id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                 name            VARCHAR(100) NOT NULL,
                                 type            VARCHAR(20) NOT NULL DEFAULT 'FULL_REDUCTION' COMMENT '满减（MVP 唯一类型）',
                                 threshold_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '使用门槛，0=无门槛',
                                 discount_amount DECIMAL(10,2) NOT NULL COMMENT '面额',
                                 valid_days      INT NOT NULL COMMENT '领取后 N 天内有效',
                                 total_count     INT NOT NULL DEFAULT -1 COMMENT '发放总量，-1 不限',
                                 status          TINYINT NOT NULL DEFAULT 1,
                                 deleted         TINYINT NOT NULL DEFAULT 0,
                                 created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券模板';

-- 12. 用户优惠券
CREATE TABLE user_coupon (
                             id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                             user_id      BIGINT UNSIGNED NOT NULL,
                             template_id  BIGINT UNSIGNED NOT NULL,
                             status       VARCHAR(20) NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED/USED/EXPIRED',
                             received_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             expire_at    DATETIME NOT NULL COMMENT 'received_at + valid_days',
                             used_order_id BIGINT UNSIGNED NULL,
                             used_at      DATETIME NULL,
                             created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             KEY idx_user_status (user_id, status),
                             KEY idx_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户优惠券';

-- 13. 积分流水
CREATE TABLE point_record (
                              id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                              user_id          BIGINT UNSIGNED NOT NULL,
                              change_value     INT NOT NULL COMMENT '正=获得 负=抵扣',
                              type             VARCHAR(20) NOT NULL COMMENT 'EARN/REDEEM/EXPIRE/MANUAL',
                              related_order_id BIGINT UNSIGNED NULL,
                              balance_after    INT NOT NULL COMMENT '变动后余额',
                              remark           VARCHAR(100) NOT NULL DEFAULT '',
                              created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              KEY idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分流水（余额=user 表积分字段，流水为审计源）';

-- 14. AI 会话（status 三值一次定全：AI_SERVING/HUMAN_SERVING/ENDED，HUMAN_SERVING 为 M3 预留）
CREATE TABLE chat_session (
                              id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                              user_id    BIGINT UNSIGNED NOT NULL,
                              status     VARCHAR(20) NOT NULL DEFAULT 'AI_SERVING' COMMENT 'AI_SERVING/HUMAN_SERVING(预留)/ENDED',
                              agent_id   BIGINT UNSIGNED NULL COMMENT '承接坐席，MVP 恒 NULL（M3 启用）',
                              title      VARCHAR(100) NOT NULL DEFAULT '' COMMENT '首条消息前 20 字截断【补充】',
                              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              KEY idx_user_updated (user_id, updated_at),
                              KEY idx_status_updated (status, updated_at) COMMENT '超时扫描专用（§3.2 定时任务）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 会话';

-- 15. 会话消息（role 四值一次定全：USER/AI/AGENT/SYSTEM，AGENT 为 M3 预留）
CREATE TABLE chat_message (
                              id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                              session_id BIGINT UNSIGNED NOT NULL,
                              role       VARCHAR(10) NOT NULL COMMENT 'USER/AI/AGENT(预留)/SYSTEM',
                              content    TEXT NOT NULL,
                              tool_calls JSON NULL COMMENT '工具调用摘要：[{"toolCallId":"call_abc123","tool":"queryMyOrders","success":true,"durationMs":35}]',
                              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              KEY idx_session_id_id (session_id, id) COMMENT '游标分页专用（WHERE session_id=? AND id<? ORDER BY id DESC）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话消息';

-- 16. 工具调用日志（只读工具执行记录；"工具调用成功率 ≥95%"指标数据源）
CREATE TABLE tool_call_log (
                               id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                               message_id  BIGINT UNSIGNED NOT NULL,
                               session_id  BIGINT UNSIGNED NOT NULL COMMENT '冗余，便于按会话统计',
                               tool_name   VARCHAR(50) NOT NULL,
                               params      JSON NULL,
                               result      JSON NULL COMMENT '超长结果截断存储',
                               success     TINYINT NOT NULL DEFAULT 1,
                               duration_ms INT NOT NULL DEFAULT 0,
                               error_msg   VARCHAR(500) NULL,
                               created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               KEY idx_message (message_id),
                               KEY idx_tool_created (tool_name, created_at) COMMENT '成功率统计专用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工具调用日志';

-- 17. 知识库文档（状态门：DRAFT→PUBLISHED，总体设计 v1.2 修正 5）
CREATE TABLE kb_document (
                             id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                             title      VARCHAR(100) NOT NULL,
                             kb_type    VARCHAR(20) NOT NULL COMMENT 'MENU/ACTIVITY/MEMBER_RULE/FAQ/BRAND',
                             content    MEDIUMTEXT NOT NULL COMMENT 'Markdown 源文',
                             status     VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT 草稿/PUBLISHED 已发布（重建成功才置发布）',
                             version    INT NOT NULL DEFAULT 1,
                             deleted    TINYINT NOT NULL DEFAULT 0,
                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             KEY idx_type_status (kb_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库文档';

-- 18. 意图分类日志（总体设计 v1.3：与 tool_call_log 分表，独立口径）
CREATE TABLE intent_classify_log (
                                     id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                                     message_id    BIGINT UNSIGNED NOT NULL,
                                     intent        VARCHAR(20) NOT NULL COMMENT '最终采用意图（含降级后）',
                                     confidence    DECIMAL(3,2) NOT NULL,
                                     is_degraded   TINYINT NOT NULL DEFAULT 0,
                                     degrade_reason VARCHAR(50) NULL COMMENT 'PARSE_FAILED/TIMEOUT/INVALID_ENUM/RETRY_EXHAUSTED',
                                     duration_ms   INT NOT NULL DEFAULT 0,
                                     created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     KEY idx_message (message_id),
                                     KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='意图分类日志';