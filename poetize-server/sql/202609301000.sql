-- ============================================================
-- 功能说明：文章回收站与历史版本快照 + 资源回收站（删除可恢复、替换旧版可回滚）
-- 变更内容：
--   1. article.deleted_time：文章移入回收站的时间，用于 30 天保留期倒计时
--   2. article_version：文章每次更新/恢复前的全量快照（含全部语言翻译），
--      误更新可回滚，默认保留每篇最近 20 版且 90 天
--   3. resource.trash_time：资源移入回收站的时间（content_state='TRASH' 时用于保留期倒计时）
--   4. resource_trash：替换成功后保留的旧文件备份登记（误替换可恢复），
--      每个备份文件一行；唯一键防重复登记
--   5. 回收站分页列表与每日超期清理的复合索引：
--      article(deleted, deleted_time)、resource(content_state, trash_time)
-- 设计说明：
--   - 文章删除沿用 deleted 逻辑删除底座，仅补充删除时间；恢复时翻译随文章一起还原
--   - 快照与更新在同一事务写入，保证版本与数据一致
--   - 恢复指定版本前会先把当前版快照为 RESTORE 型，恢复操作本身可反复撤销
--   - 删除进回收站不移动文件：status=false + content_state='TRASH'，
--     /media/{publicId} 因 content_state 非 ACTIVE 自动不可读
--   - 彻底删除（物理清理）复用既有 claim → 删文件 → finalize 管线，仅限管理员网页端
--   - 替换备份保留在原目录（*.backup 文件），恢复时哈希校验后原子移回
--   - 索引列顺序与查询条件一致（等值列在前、范围/排序列在后）
--   - 全部语句可重复执行（IF NOT EXISTS / MODIFY 幂等），中断后可安全重跑
-- 日期：2026-09-30
-- ============================================================

-- ======================= 1. 文章历史版本快照表（建表前置） =======================
-- 建表语句统一前置：MySQL/TiDB 不支持 ALTER ... IF NOT EXISTS，若首条 ALTER 报语法错误，
-- mariadb/mysql 客户端会中止整个脚本；把 CREATE TABLE 放前面可保证"表一定被创建"。

CREATE TABLE IF NOT EXISTS `article_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` int NOT NULL COMMENT '文章ID',
  `version_no` int NOT NULL COMMENT '版本号（每篇文章自增）',
  `snapshot_type` varchar(16) NOT NULL DEFAULT 'UPDATE' COMMENT '快照类型 UPDATE:更新前/RESTORE:恢复前',
  `editor_user_id` int DEFAULT NULL COMMENT '操作人用户ID',
  `editor_username` varchar(64) DEFAULT NULL COMMENT '操作人用户名',
  `article_title` varchar(500) DEFAULT NULL COMMENT '博文标题快照',
  `article_slug` varchar(160) DEFAULT NULL COMMENT 'URL别名快照',
  `article_content` longtext DEFAULT NULL COMMENT '博文内容快照',
  `summary` varchar(500) DEFAULT NULL COMMENT '文章摘要快照',
  `article_cover` varchar(256) DEFAULT NULL COMMENT '封面快照',
  `video_url` varchar(1024) DEFAULT NULL COMMENT '视频链接快照',
  `password` varchar(128) DEFAULT NULL COMMENT '访问密码快照',
  `tips` varchar(128) DEFAULT NULL COMMENT '提示快照',
  `view_status` tinyint(1) DEFAULT NULL COMMENT '是否可见快照[0:否，1:是]',
  `comment_status` tinyint(1) DEFAULT NULL COMMENT '是否启用评论快照[0:否，1:是]',
  `recommend_status` tinyint(1) DEFAULT NULL COMMENT '是否推荐快照[0:否，1:是]',
  `submit_to_search_engine` tinyint(1) DEFAULT NULL COMMENT '是否推送搜索引擎快照[0:否，1:是]',
  `pay_type` tinyint(1) DEFAULT NULL COMMENT '付费类型快照',
  `pay_amount` decimal(10,2) DEFAULT NULL COMMENT '付费金额快照(元)',
  `free_percent` int DEFAULT NULL COMMENT '免费预览百分比快照(0-100)',
  `sort_id` int DEFAULT NULL COMMENT '分类ID快照',
  `label_id` int DEFAULT NULL COMMENT '标签ID快照',
  `publish_time` datetime DEFAULT NULL COMMENT '首次公开发布时间快照',
  `translations_json` mediumtext DEFAULT NULL COMMENT '全部语言翻译快照JSON[{language,title,content,summary}]',
  `content_hash` char(64) DEFAULT NULL COMMENT '内容SHA-256（去重用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照时间',
  PRIMARY KEY (`id`),
  KEY `idx_article_version_article` (`article_id`, `version_no`),
  KEY `idx_article_version_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章历史版本快照表';

-- ====================== 2. 替换旧版备份登记表（建表前置） ======================

CREATE TABLE IF NOT EXISTS `resource_trash` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `resource_id` int NOT NULL COMMENT '资源ID',
  `public_id` varchar(64) DEFAULT NULL COMMENT '资源公开ID',
  `item_type` varchar(32) NOT NULL DEFAULT 'REPLACEMENT_BACKUP' COMMENT '条目类型 REPLACEMENT_BACKUP:替换旧版备份',
  `original_path` varchar(512) NOT NULL COMMENT '被替换的目标文件路径（恢复目标）',
  `backup_path` varchar(512) NOT NULL COMMENT '备份文件路径',
  `backup_path_hash` char(64) NOT NULL COMMENT '备份路径SHA-256（唯一键用，规避长路径前缀索引冲突）',
  `file_hash` char(64) DEFAULT NULL COMMENT '备份文件SHA-256',
  `file_size` bigint DEFAULT NULL COMMENT '备份文件大小(字节)',
  `mime_type` varchar(128) DEFAULT NULL COMMENT '备份文件MIME类型',
  `original_name` varchar(256) DEFAULT NULL COMMENT '资源原始文件名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '进入回收站时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_trash_backup_path_hash` (`backup_path_hash`),
  KEY `idx_resource_trash_backup_path` (`backup_path`(255)),
  KEY `idx_resource_trash_resource` (`resource_id`),
  KEY `idx_resource_trash_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源回收站（替换旧版备份）表';

-- ============================ 3. 文章回收站 ============================

ALTER TABLE `article`
    ADD COLUMN IF NOT EXISTS `deleted_time` datetime DEFAULT NULL COMMENT '进入回收站时间' AFTER `deleted`;

-- 回填历史逻辑删除行：升级前被删除的文章 deleted_time 为 NULL，会被"deleted_time is not null"的
-- 超期清理条件永久跳过，且回收站倒计时显示为 0 天。以 update_time 兜底，缺省用当前时间。
UPDATE `article`
   SET `deleted_time` = COALESCE(`update_time`, NOW())
 WHERE `deleted` = 1 AND `deleted_time` IS NULL;

ALTER TABLE `article`
    ADD INDEX IF NOT EXISTS `idx_article_deleted_time` (`deleted`, `deleted_time`);

-- ============================ 4. 资源回收站 ============================

ALTER TABLE `resource`
    ADD COLUMN IF NOT EXISTS `trash_time` datetime DEFAULT NULL COMMENT '进入回收站时间' AFTER `content_state`;

ALTER TABLE `resource`
    ADD COLUMN IF NOT EXISTS `deletion_pending_time` datetime DEFAULT NULL COMMENT '进入删除声明状态时间（回收兜底的时间护栏）' AFTER `trash_time`;

ALTER TABLE `resource`
    MODIFY COLUMN `content_state` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
        COMMENT 'ACTIVE/REPLACEMENT_PENDING/DELETION_PENDING/TRASH';

ALTER TABLE `resource`
    ADD INDEX IF NOT EXISTS `idx_resource_content_state_time` (`content_state`, `trash_time`);

