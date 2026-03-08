-- 版本升级迁移脚本 - 2026-02-19
-- 功能: article 表加支付字段 + 创建 article_payment 表 + 开放式插件系统字段 + 支付插件初始化
-- 注意: 此脚本设计为幂等，可安全重复执行

-- ============================================
-- 1. article 表加支付相关字段
-- ============================================
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article' AND COLUMN_NAME = 'pay_type');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `article` ADD COLUMN `pay_type` TINYINT(1) DEFAULT 0 COMMENT ''付费类型 [0:免费, 1:按文章付费, 2:会员专属, 3:赞赏解锁, 4:固定金额解锁]''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article' AND COLUMN_NAME = 'pay_amount');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `article` ADD COLUMN `pay_amount` DECIMAL(10,2) DEFAULT NULL COMMENT ''付费金额(元)''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article' AND COLUMN_NAME = 'free_percent');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `article` ADD COLUMN `free_percent` INT DEFAULT 30 COMMENT ''免费预览百分比(0-100)''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 2. article_payment 表：不存在则创建，已存在则跳过
-- ============================================
CREATE TABLE IF NOT EXISTS `article_payment` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` INT NOT NULL COMMENT '博客用户ID',
    `article_id` INT NOT NULL COMMENT '文章ID（0=全站会员）',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `platform` VARCHAR(32) NOT NULL DEFAULT 'afdian' COMMENT '支付平台',
    `platform_order_id` VARCHAR(128) DEFAULT NULL COMMENT '平台订单号',
    `platform_user_id` VARCHAR(128) DEFAULT NULL COMMENT '平台用户ID',
    `custom_order_id` VARCHAR(128) DEFAULT NULL COMMENT '自定义订单ID',
    `payment_status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态 [0:待确认, 1:已支付, 2:已退款]',
    `pay_type` TINYINT(1) NOT NULL COMMENT '付费类型',
    `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `expire_time` DATETIME DEFAULT NULL COMMENT '会员过期时间（article_id=0 时使用，NULL=永久）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_platform_order` (`platform_order_id`, `platform`),
    INDEX `idx_user_article` (`user_id`, `article_id`),
    INDEX `idx_custom_order` (`custom_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章付费记录表';

-- ============================================
-- 3. plugin_migrations 表（记录插件 SQL 版本）
-- ============================================
CREATE TABLE IF NOT EXISTS `plugin_migrations` (
    `id`          INT AUTO_INCREMENT,
    `plugin_key`  VARCHAR(50)  NOT NULL COMMENT '插件 key',
    `version`     VARCHAR(20)  NOT NULL COMMENT '插件版本',
    `sql_file`    VARCHAR(128) DEFAULT NULL COMMENT '执行的 SQL 文件名',
    `executed_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    `success`     TINYINT(1)   DEFAULT 1 COMMENT '是否成功',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plugin_version` (`plugin_key`, `version`, `sql_file`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件数据库迁移记录';

-- ============================================
-- 4. sys_plugin 表新增字段
-- ============================================
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_plugin' AND COLUMN_NAME = 'version');
SET @sql := IF(@col = 0,
    'ALTER TABLE sys_plugin ADD COLUMN version VARCHAR(20) DEFAULT NULL COMMENT ''插件版本号'' AFTER plugin_description',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_plugin' AND COLUMN_NAME = 'author');
SET @sql := IF(@col = 0,
    'ALTER TABLE sys_plugin ADD COLUMN author VARCHAR(64) DEFAULT NULL COMMENT ''插件作者'' AFTER version',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_plugin' AND COLUMN_NAME = 'manifest');
SET @sql := IF(@col = 0,
    'ALTER TABLE sys_plugin ADD COLUMN manifest JSON DEFAULT NULL COMMENT ''插件 manifest.json 内容''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_plugin' AND COLUMN_NAME = 'backend_code');
SET @sql := IF(@col = 0,
    'ALTER TABLE sys_plugin ADD COLUMN backend_code MEDIUMTEXT DEFAULT NULL COMMENT ''后端 Groovy 脚本源码''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_plugin' AND COLUMN_NAME = 'frontend_css');
SET @sql := IF(@col = 0,
    'ALTER TABLE sys_plugin ADD COLUMN frontend_css TEXT DEFAULT NULL COMMENT ''前端 CSS 样式''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_plugin' AND COLUMN_NAME = 'install_sql');
SET @sql := IF(@col = 0,
    'ALTER TABLE sys_plugin ADD COLUMN install_sql TEXT DEFAULT NULL COMMENT ''安装 SQL（备份用）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_plugin' AND COLUMN_NAME = 'uninstall_sql');
SET @sql := IF(@col = 0,
    'ALTER TABLE sys_plugin ADD COLUMN uninstall_sql TEXT DEFAULT NULL COMMENT ''卸载 SQL（备份用）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_plugin' AND COLUMN_NAME = 'has_backend');
SET @sql := IF(@col = 0,
    'ALTER TABLE sys_plugin ADD COLUMN has_backend TINYINT(1) DEFAULT 0 COMMENT ''是否有后端 Groovy 脚本''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 5. 添加支付插件（含 manifest/configSchema）
-- ============================================
INSERT IGNORE INTO `sys_plugin` (`plugin_type`, `plugin_key`, `plugin_name`, `plugin_description`, `plugin_config`, `plugin_code`, `enabled`, `is_system`, `sort_order`, `manifest`) VALUES
('payment', 'afdian', '爱发电', '通过爱发电平台实现付费阅读和会员订阅',
'{"userId":"","apiToken":"","planId":"","defaultPayType":1,"fixedAmount":5.00,"freePercent":30,"enableMembership":false,"memberPlanId":"","memberDurationDays":30}',
NULL, 0, 1, 0,
'{"name":"afdian","displayName":"爱发电","version":"1.0.0","author":"LeapYa","pluginType":"payment","description":"通过爱发电平台实现付费阅读和会员订阅","configSchema":{"userId":{"type":"string","label":"用户ID","description":"爱发电用户ID（在个人设置中查看）","defaultValue":""},"apiToken":{"type":"string","label":"API Token","description":"爱发电 API Token（在开发者设置中获取）","defaultValue":""},"planId":{"type":"string","label":"方案ID","description":"用于单篇文章付费的发电方案ID","defaultValue":""},"defaultPayType":{"type":"number","label":"默认付费类型","description":"1=按文章付费, 2=会员专属, 3=赞赏解锁, 4=固定金额","defaultValue":1},"fixedAmount":{"type":"number","label":"固定金额（元）","description":"单篇文章解锁金额","defaultValue":5.00},"freePercent":{"type":"number","label":"免费预览比例(%)","description":"付费文章可免费预览的内容比例","defaultValue":30},"enableMembership":{"type":"boolean","label":"启用会员功能","description":"是否启用全站会员功能","defaultValue":false},"memberPlanId":{"type":"string","label":"会员方案ID","description":"用于全站会员的发电方案ID","defaultValue":""},"memberDurationDays":{"type":"number","label":"会员有效期（天）","description":"购买会员后的有效天数","defaultValue":30}}}');

INSERT IGNORE INTO `sys_plugin` (`plugin_type`, `plugin_key`, `plugin_name`, `plugin_description`, `plugin_config`, `plugin_code`, `enabled`, `is_system`, `sort_order`, `manifest`) VALUES
('payment', 'epay', '易支付(V2)', '第三方聚合支付V2接口，SHA256WithRSA签名，支持支付宝/微信/QQ钱包',
'{"apiUrl":"","pid":"","privateKey":"","epayPublicKey":"","notifyUrl":"","returnUrl":"","defaultPayType":1,"fixedAmount":5.00,"freePercent":30,"memberDurationDays":30}',
NULL, 0, 1, 2,
'{"name":"epay","displayName":"易支付(V2)","version":"1.0.0","author":"LeapYa","pluginType":"payment","description":"第三方聚合支付V2接口，SHA256WithRSA签名，支持支付宝/微信/QQ钱包","configSchema":{"apiUrl":{"type":"string","label":"接口地址","description":"易支付API地址，如 https://pay.example.com","defaultValue":""},"pid":{"type":"string","label":"商户ID","description":"易支付商户号","defaultValue":""},"privateKey":{"type":"string","label":"商户私钥","description":"RSA私钥（PKCS#8格式，不含头尾）","defaultValue":""},"epayPublicKey":{"type":"string","label":"平台公钥","description":"易支付平台RSA公钥（用于验签）","defaultValue":""},"notifyUrl":{"type":"string","label":"异步通知URL","description":"支付成功后的回调地址","defaultValue":""},"returnUrl":{"type":"string","label":"同步跳转URL","description":"支付完成后跳转的页面地址","defaultValue":""},"defaultPayType":{"type":"number","label":"默认付费类型","description":"1=按文章付费, 2=会员专属, 3=赞赏解锁, 4=固定金额","defaultValue":1},"fixedAmount":{"type":"number","label":"固定金额（元）","description":"单篇文章解锁金额","defaultValue":5.00},"freePercent":{"type":"number","label":"免费预览比例(%)","description":"付费文章可免费预览的内容比例","defaultValue":30},"memberDurationDays":{"type":"number","label":"会员有效期（天）","description":"购买会员后的有效天数","defaultValue":30}}}');

INSERT IGNORE INTO `sys_plugin` (`plugin_type`, `plugin_key`, `plugin_name`, `plugin_description`, `plugin_config`, `plugin_code`, `enabled`, `is_system`, `sort_order`, `manifest`) VALUES
('payment', 'mianbaoduo', '面包多Pay', '面包多内容变现平台支付',
'{"appId":"","appSecret":"","callbackUrl":"","defaultPayType":1,"fixedAmount":5.00,"freePercent":30,"memberDurationDays":30}',
NULL, 0, 1, 3,
'{"name":"mianbaoduo","displayName":"面包多Pay","version":"1.0.0","author":"LeapYa","pluginType":"payment","description":"面包多内容变现平台支付","configSchema":{"appId":{"type":"string","label":"应用ID","description":"面包多应用ID","defaultValue":""},"appSecret":{"type":"string","label":"应用密钥","description":"面包多应用密钥","defaultValue":""},"callbackUrl":{"type":"string","label":"回调地址","description":"支付成功后的回调URL","defaultValue":""},"defaultPayType":{"type":"number","label":"默认付费类型","description":"1=按文章付费, 2=会员专属, 3=赞赏解锁, 4=固定金额","defaultValue":1},"fixedAmount":{"type":"number","label":"固定金额（元）","description":"单篇文章解锁金额","defaultValue":5.00},"freePercent":{"type":"number","label":"免费预览比例(%)","description":"付费文章可免费预览的内容比例","defaultValue":30},"memberDurationDays":{"type":"number","label":"会员有效期（天）","description":"购买会员后的有效天数","defaultValue":30}}}');

INSERT IGNORE INTO `sys_plugin` (`plugin_type`, `plugin_key`, `plugin_name`, `plugin_description`, `plugin_config`, `plugin_code`, `enabled`, `is_system`, `sort_order`, `manifest`) VALUES
('payment', 'buymeacoffee', 'Buy Me a Coffee', '海外创作者赞助平台',
'{"accessToken":"","webhookSecret":"","defaultPayType":3,"fixedAmount":5.00,"freePercent":30,"memberDurationDays":30}',
NULL, 0, 1, 4,
'{"name":"buymeacoffee","displayName":"Buy Me a Coffee","version":"1.0.0","author":"LeapYa","pluginType":"payment","description":"海外创作者赞助平台","configSchema":{"accessToken":{"type":"string","label":"Access Token","description":"Buy Me a Coffee API访问令牌","defaultValue":""},"webhookSecret":{"type":"string","label":"Webhook密钥","description":"用于验证回调请求的密钥","defaultValue":""},"defaultPayType":{"type":"number","label":"默认付费类型","description":"1=按文章付费, 2=会员专属, 3=赞赏解锁, 4=固定金额","defaultValue":3},"fixedAmount":{"type":"number","label":"固定金额（美元）","description":"单篇文章解锁金额","defaultValue":5.00},"freePercent":{"type":"number","label":"免费预览比例(%)","description":"付费文章可免费预览的内容比例","defaultValue":30},"memberDurationDays":{"type":"number","label":"会员有效期（天）","description":"购买会员后的有效天数","defaultValue":30}}}');
