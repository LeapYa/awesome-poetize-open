-- ============================================================
-- 功能说明：删除 seo_social_media 表中无用的 og_site_name 字段
-- 变更内容：
--   1. 删除 og_site_name 字段（由网站标题/名称动态生成，不再从数据库读取）
-- 日期：2026-06-15
-- 幂等改造（2026-10-09）：原写法在列已删除时重复执行会报错中断，
--   影响迁移"幂等复核重跑"（重跑已执行脚本以自愈 schema 漂移），
--   改为存在才删的动态检查写法，与其他迁移脚本的幂等风格一致。
-- ============================================================

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'seo_social_media' AND COLUMN_NAME = 'og_site_name');
SET @sql = IF(@col_exists > 0,
    'ALTER TABLE `seo_social_media` DROP COLUMN `og_site_name`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
