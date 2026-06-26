-- =============================================
-- Migration 001: 补缺失复合索引
-- 对应 issue: P1-3 message + thread 缺失索引
-- 适用: 已存在的数据库 (新库请直接用 ddl.sql)
-- =============================================

-- 1. message 游标分页索引
--    查询模式: WHERE channel_id=? AND id<? ORDER BY id DESC
--    原索引 idx_channel_id(channel_id) 无法覆盖 ORDER BY id
ALTER TABLE `message` ADD INDEX `idx_channel_id_id` (`channel_id`, `id`);

-- 2. thread 自动归档索引
--    查询模式: WHERE status='ACTIVE' AND last_active<?
--    原索引 idx_channel_id(channel_id) 对归档查询无效
ALTER TABLE `thread` ADD INDEX `idx_status_last_active` (`status`, `last_active`);
