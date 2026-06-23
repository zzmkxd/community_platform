-- =============================================
-- Community Platform DDL (社群平台数据库)
-- MySQL 8.0
-- =============================================

CREATE DATABASE IF NOT EXISTS community_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE community_platform;

-- =============================================
-- 1. User 用户表
-- =============================================
-- 设计决策: username/password 仅用于 seed data 测试通道（无注册端点）。
-- 微信扫码用户 open_id 有值，username/password/email 为 NULL。
-- 两种用户通过 NULL 值区分，MySQL UNIQUE 索引允许多个 NULL。
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
    `username`    VARCHAR(32)  DEFAULT NULL COMMENT '用户名 (seed data 测试账号)',
    `password`    VARCHAR(128) DEFAULT NULL COMMENT 'BCrypt 密码 (seed data 测试账号)',
    `nickname`    VARCHAR(32)  DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
    `email`       VARCHAR(64)  DEFAULT NULL COMMENT '邮箱',
    `open_id`     VARCHAR(64)  DEFAULT NULL COMMENT '微信 openId (UK)',
    `union_id`    VARCHAR(64)  DEFAULT NULL COMMENT '微信 unionId',
    `sex`         TINYINT      DEFAULT 0 COMMENT '性别: 0=未知 1=男 2=女',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态: 0=禁用 1=正常',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_open_id` (`open_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- Seed data: 预置测试账号 (密码均为 "123456")
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `email`) VALUES
(1, 'alice',   '$2a$10$mLITQAM7X/6NI0trNtT10O6yUGEDczRxMz9RLAF8/VwrYO5yoZuw6', 'Alice',   'alice@test.com'),
(2, 'bob',     '$2a$10$mLITQAM7X/6NI0trNtT10O6yUGEDczRxMz9RLAF8/VwrYO5yoZuw6', 'Bob',     'bob@test.com'),
(3, 'charlie', '$2a$10$mLITQAM7X/6NI0trNtT10O6yUGEDczRxMz9RLAF8/VwrYO5yoZuw6', 'Charlie', 'charlie@test.com');

-- =============================================
-- 2. Server 服务器表
-- =============================================
CREATE TABLE IF NOT EXISTS `server` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '服务器 ID',
    `name`        VARCHAR(64)  NOT NULL COMMENT '服务器名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `icon`        VARCHAR(255) DEFAULT NULL COMMENT '图标 URL',
    `owner_id`    BIGINT       NOT NULL COMMENT '创建者用户 ID',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态: 0=删除 1=正常',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_owner_id` (`owner_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务器表';

-- =============================================
-- 3. Category 分类表
-- =============================================
CREATE TABLE IF NOT EXISTS `category` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类 ID',
    `server_id`   BIGINT      NOT NULL COMMENT '所属服务器 ID',
    `name`        VARCHAR(32) NOT NULL COMMENT '分类名称',
    `sort_order`  INT         DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_server_id` (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

-- =============================================
-- 4. Channel 频道表
-- =============================================
CREATE TABLE IF NOT EXISTS `channel` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '频道 ID',
    `server_id`   BIGINT       NOT NULL COMMENT '所属服务器 ID',
    `category_id` BIGINT       DEFAULT NULL COMMENT '所属分类 ID (可为空)',
    `name`        VARCHAR(32)  NOT NULL COMMENT '频道名称',
    `type`        TINYINT      DEFAULT 0 COMMENT '类型: 0=TEXT 1=VOICE',
    `topic`       VARCHAR(255) DEFAULT NULL COMMENT '频道主题/描述',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态: 0=删除 1=正常',
    `last_msg_id` BIGINT       DEFAULT NULL COMMENT '最后一条消息 ID',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_server_id` (`server_id`),
    KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='频道表';

-- =============================================
-- 5. ServerMember 成员表
-- =============================================
CREATE TABLE IF NOT EXISTS `server_member` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '成员关联 ID',
    `server_id`   BIGINT       NOT NULL COMMENT '服务器 ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户 ID',
    `nickname`    VARCHAR(32)  DEFAULT NULL COMMENT '服务器内昵称',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态: 1=正常 2=被踢 3=已离开',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_server_user` (`server_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务器成员表';

-- =============================================
-- 6. Role 角色表
-- =============================================
CREATE TABLE IF NOT EXISTS `role` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色 ID',
    `server_id`   BIGINT       NOT NULL COMMENT '所属服务器 ID',
    `name`        VARCHAR(32)  NOT NULL COMMENT '角色名称',
    `color`       VARCHAR(7)   DEFAULT NULL COMMENT '角色颜色 (#hex)',
    `permissions` BIGINT       DEFAULT 0 COMMENT '权限位图 (14 位)',
    `position`    INT          DEFAULT 0 COMMENT '排序 (越大越靠上)',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_server_id` (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- =============================================
-- 7. MemberRole 成员-角色关联表
-- =============================================
CREATE TABLE IF NOT EXISTS `member_role` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '关联 ID',
    `member_id`   BIGINT   NOT NULL COMMENT '成员关联 ID (server_member.id)',
    `role_id`     BIGINT   NOT NULL COMMENT '角色 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_role` (`member_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成员-角色关联表';

-- =============================================
-- 8. ChannelPermission 频道权限覆盖表
-- =============================================
CREATE TABLE IF NOT EXISTS `channel_permission` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '覆盖 ID',
    `channel_id`  BIGINT   NOT NULL COMMENT '频道 ID',
    `target_type` TINYINT  NOT NULL COMMENT '目标类型: 0=角色 1=用户',
    `target_id`   BIGINT   NOT NULL COMMENT '角色 ID 或 用户 ID',
    `allow_bits`  BIGINT   DEFAULT 0 COMMENT '允许的权限位',
    `deny_bits`   BIGINT   DEFAULT 0 COMMENT '拒绝的权限位',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_channel_target` (`channel_id`, `target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='频道权限覆盖表';

-- =============================================
-- 9. Emoji 表情表
-- =============================================
CREATE TABLE IF NOT EXISTS `emoji` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '表情 ID',
    `server_id`   BIGINT       NOT NULL COMMENT '所属服务器 ID',
    `name`        VARCHAR(64)  NOT NULL COMMENT '表情名称',
    `url`         VARCHAR(255) NOT NULL COMMENT 'MinIO objectKey',
    `creator_id`  BIGINT       NOT NULL COMMENT '上传者用户 ID',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_server_id` (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表情表';

-- =============================================
-- 10. Invite 邀请链接表
-- =============================================
CREATE TABLE IF NOT EXISTS `invite` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '邀请 ID',
    `server_id`   BIGINT       NOT NULL COMMENT '所属服务器 ID',
    `inviter_id`  BIGINT       NOT NULL COMMENT '创建者用户 ID',
    `code`        VARCHAR(16)  NOT NULL COMMENT '邀请码（UUID 短码）',
    `max_uses`    INT          DEFAULT 0 COMMENT '最大使用次数（0=不限）',
    `used_count`  INT          DEFAULT 0 COMMENT '已使用次数',
    `expire_time` DATETIME     DEFAULT NULL COMMENT '过期时间（NULL=永久）',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态: 0=失效 1=有效',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_server_id` (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请链接表';

-- =============================================
-- 11. Message 消息表
-- =============================================
CREATE TABLE IF NOT EXISTS `message` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息 ID',
    `channel_id`  BIGINT       NOT NULL COMMENT '频道 ID',
    `thread_id`   BIGINT       DEFAULT NULL COMMENT '话题 ID (可为空)',
    `from_uid`    BIGINT       NOT NULL COMMENT '发送者用户 ID',
    `content`     TEXT         NOT NULL COMMENT '消息内容',
    `msg_type`    TINYINT      DEFAULT 1 COMMENT '消息类型: 1=TEXT 2=IMAGE 3=FILE 4=SYSTEM 5=SOUND 6=EMOJI',
    `extra`       JSON         DEFAULT NULL COMMENT '扩展信息 JSON',
    `status`      TINYINT      DEFAULT 0 COMMENT '状态: 0=正常 1=删除 2=已编辑',
    `reply_msg_id` BIGINT      DEFAULT NULL COMMENT '回复的消息 ID',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_channel_id` (`channel_id`),
    KEY `idx_thread_id` (`thread_id`),
    KEY `idx_from_uid` (`from_uid`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- =============================================
-- 12. Thread 话题表
-- =============================================
CREATE TABLE IF NOT EXISTS `thread` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '话题 ID',
    `channel_id`    BIGINT      NOT NULL COMMENT '所属频道 ID',
    `root_msg_id`   BIGINT      NOT NULL COMMENT '触发创建的消息 ID',
    `name`          VARCHAR(128) DEFAULT NULL COMMENT '话题名称',
    `creator_id`    BIGINT      NOT NULL COMMENT '创建者用户 ID',
    `status`        VARCHAR(16) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE / ARCHIVED',
    `message_count` INT         DEFAULT 0 COMMENT '消息数量',
    `last_msg_id`   BIGINT      DEFAULT NULL COMMENT '最后一条消息 ID',
    `last_active`   DATETIME    DEFAULT NULL COMMENT '最后活跃时间',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_channel_id` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题表';

-- =============================================
-- 13. Reaction 反应表
-- =============================================
CREATE TABLE IF NOT EXISTS `reaction` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '反应 ID',
    `message_id`  BIGINT      NOT NULL COMMENT '消息 ID',
    `user_id`     BIGINT      NOT NULL COMMENT '用户 ID',
    `emoji`       VARCHAR(32) NOT NULL COMMENT 'Emoji 字符',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_msg_user_emoji` (`message_id`, `user_id`, `emoji`),
    KEY `idx_message_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反应表';

-- =============================================
-- 14. channel_read_state 已读追踪表
-- =============================================
CREATE TABLE IF NOT EXISTS `channel_read_state` (
    `id`              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录 ID',
    `user_id`         BIGINT   NOT NULL COMMENT '用户 ID',
    `channel_id`      BIGINT   NOT NULL COMMENT '频道 ID',
    `last_read_msg_id` BIGINT  DEFAULT 0 COMMENT '最后已读消息 ID',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_channel` (`user_id`, `channel_id`),
    KEY `idx_channel_id` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='频道已读追踪表';

-- =============================================
-- 15. sensitive_word 敏感词表
-- =============================================
CREATE TABLE IF NOT EXISTS `sensitive_word` (
    `word` VARCHAR(64) NOT NULL COMMENT '敏感词',
    PRIMARY KEY (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词表';

-- =============================================
-- 16. FileAttachment 文件附件表
-- =============================================
CREATE TABLE IF NOT EXISTS `file_attachment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件 ID',
    `user_id`     BIGINT       NOT NULL COMMENT '上传者用户 ID',
    `object_key`  VARCHAR(255) NOT NULL COMMENT 'MinIO 对象 Key',
    `file_name`   VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_size`   BIGINT       DEFAULT 0 COMMENT '文件大小 (bytes)',
    `mime_type`   VARCHAR(64)  DEFAULT NULL COMMENT 'MIME 类型',
    `status`      VARCHAR(16)  DEFAULT 'PENDING' COMMENT '状态: PENDING / UPLOADED',
    `message_id`  BIGINT       DEFAULT NULL COMMENT '关联的消息 ID',
    `width`       INT          DEFAULT NULL COMMENT '图片宽度',
    `height`      INT          DEFAULT NULL COMMENT '图片高度',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_message_id` (`message_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件附件表';

-- =============================================
-- 17. secure_invoke_record 本地消息表
-- =============================================
CREATE TABLE IF NOT EXISTS `secure_invoke_record` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `secure_invoke_json` JSON        NOT NULL COMMENT '方法调用快照 (className+methodName+parameterTypes+args)',
    `status`            TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1=待执行 2=已失败',
    `next_retry_time`   DATETIME     NOT NULL COMMENT '下次重试时间',
    `retry_times`       INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `max_retry_times`   INT          NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    `fail_reason`       VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status_next_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全执行本地消息表';
