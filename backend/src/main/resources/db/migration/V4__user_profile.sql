-- V4__user_profile.sql · 用户资料扩展（01 v1.3 追加字段口径 / 04 v1.6 §8.1）
ALTER TABLE user
    ADD COLUMN avatar_url VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像 URL（空=默认波浪徽章）' AFTER points,
    ADD COLUMN gender     VARCHAR(10)  NOT NULL DEFAULT 'UNKNOWN' COMMENT 'UNKNOWN/MALE/FEMALE' AFTER avatar_url,
    ADD COLUMN lucky_day  VARCHAR(20)  NULL COMMENT '幸运日（展示用自由文本）' AFTER gender;

-- 种子：演示账号张三（user_id=2，V2 种子 13800000001）
UPDATE user SET avatar_url = '', gender = 'MALE', lucky_day = '' WHERE id = 2;
