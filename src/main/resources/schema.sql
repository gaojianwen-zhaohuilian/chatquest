-- ChatQuest 数据库初始化
-- D1 执行：创建库后运行本脚本
-- 命令：mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS chatquest
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chatquest;

-- ============================================================
-- 原始消息表
-- 用途：幂等追溯 + 抽取结果回溯（用户改任务时能查到原始来源）
-- 注意：D4 完成，落库字段可能还要按实测调整
-- ============================================================
CREATE TABLE IF NOT EXISTS `raw_message`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `platform`        VARCHAR(32)     NOT NULL COMMENT '来源平台',
    `message_id`      VARCHAR(128)    NOT NULL COMMENT '平台侧消息 ID',
    `conversation_id` VARCHAR(128)    NOT NULL COMMENT '会话 ID：群聊为群 ID',
    `user_id`         VARCHAR(128)    NOT NULL COMMENT '发送者平台侧 ID',
    `user_name`       VARCHAR(128)             DEFAULT NULL COMMENT '发送者显示名',
    `content`         TEXT            NOT NULL COMMENT '归一化后的纯文本内容',
    `payload`         JSON                     DEFAULT NULL COMMENT '平台原始报文，排查问题用',
    `sent_at`         DATETIME(3)     NOT NULL COMMENT '平台侧发送时间',
    `created_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    -- 幂等兜底：Redis 失效时数据库层面仍能拦截重复消息
    UNIQUE KEY `uk_platform_message` (`platform`, `message_id`),
    KEY `idx_conversation` (`platform`, `conversation_id`),
    KEY `idx_sent_at` (`sent_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='原始消息表';

-- ============================================================
-- 任务表
-- 字段设计要点（这是 D11 的预埋，D11 可调整）：
--   confidence      必留。用于「低置信度不自动入库」的策略追溯
--   source_msg_id   必留。用户改任务时能回溯原始消息
--   raw_content     必留。展示「任务来自哪句话」时直接用，省一次 join
--   due_at          可空。对应「有任务无明确时间」的场景（如 N02/N03 语料）
-- ============================================================
CREATE TABLE IF NOT EXISTS `task`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `title`          VARCHAR(255)    NOT NULL COMMENT '任务标题',
    `description`    TEXT                     DEFAULT NULL COMMENT '任务描述',
    `assignee_id`    VARCHAR(128)             DEFAULT NULL COMMENT '执行人平台侧 ID',
    `assignee_name`  VARCHAR(128)             DEFAULT NULL COMMENT '执行人显示名',
    `due_at`         DATETIME(3)              DEFAULT NULL COMMENT '截止时间，为空表示无明确期限',
    `priority`       TINYINT         NOT NULL DEFAULT 2 COMMENT '1=高 2=中 3=低',
    `status`         VARCHAR(16)     NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING/CONFIRMED/DONE/CANCELLED',
    `confidence`     DECIMAL(4, 3)   NOT NULL COMMENT 'LLM 抽取置信度 0~1',
    `source_platform` VARCHAR(32)             DEFAULT NULL COMMENT '来源平台',
    `source_msg_id`  VARCHAR(128)            DEFAULT NULL COMMENT '来源消息 ID',
    `raw_content`    TEXT                     DEFAULT NULL COMMENT '任务来源原文',
    `created_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),

    KEY `idx_due_at_status` (`due_at`, `status`),
    KEY `idx_confidence` (`confidence`),
    KEY `idx_source` (`source_platform`, `source_msg_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务表';

-- ============================================================
-- 抽取记录表（可选，D12 跑批量评测时需要）
-- 用途：留存每次抽取的输入输出，便于对比 Prompt 迭代前后差异
-- ============================================================
CREATE TABLE IF NOT EXISTS `extraction_log`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `case_id`        VARCHAR(32)              DEFAULT NULL COMMENT '关联评测集 ID',
    `source_platform` VARCHAR(32)             DEFAULT NULL,
    `raw_content`    TEXT            NOT NULL COMMENT '输入原文',
    `model_output`   JSON                     DEFAULT NULL COMMENT '模型原始输出',
    `parsed_task`    JSON                     DEFAULT NULL COMMENT '解析后的结构化任务',
    `confidence`     DECIMAL(4, 3)            DEFAULT NULL,
    `created_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    KEY `idx_case` (`case_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽取记录表';
