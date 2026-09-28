-- V3__add_user_points.sql
-- 积分余额字段：point_record 注释约定"余额=user 表积分字段，流水为审计源"，
-- V1 初稿漏列，按 Flyway 演进叙事补齐（详细设计 §1.2 point_record）。
ALTER TABLE user
    ADD COLUMN points INT NOT NULL DEFAULT 0 COMMENT '积分余额（point_record 为审计源）' AFTER member_level;
