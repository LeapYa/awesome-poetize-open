-- ============================================================
-- 数据库迁移版本管理表初始化脚本
-- 此脚本必须最先执行（文件名以0开头确保排序在最前）
-- 功能: 创建版本跟踪表，记录已执行的迁移脚本
-- 此脚本本身设计为幂等，可安全重复执行
-- ============================================================

-- 创建迁移版本记录表
CREATE TABLE IF NOT EXISTS `db_migrations` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `version` varchar(20) NOT NULL COMMENT '版本号（迁移脚本文件名，不含.sql后缀）',
  `description` varchar(200) DEFAULT NULL COMMENT '迁移说明',
  `executed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  `execution_time_ms` int DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `success` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否执行成功 [0:失败, 1:成功]',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_version` (`version`) COMMENT '版本号唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库迁移版本记录表';

-- 记录此脚本本身已执行（使用 INSERT IGNORE 避免重复）
INSERT IGNORE INTO `db_migrations` (`version`, `description`, `success`) VALUES
('000000000000', '数据库迁移版本管理表初始化', 1);
