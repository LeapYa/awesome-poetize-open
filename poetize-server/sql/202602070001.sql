-- 版本升级迁移脚本 - 2026-02-07
-- 功能: 添加文章主题插件系统，支持自定义文章标题装饰和目录样式
-- 注意: 此脚本设计为幂等，可安全重复执行

-- ============================================
-- 1. 插入文章主题插件
-- ============================================
-- 用户视角的 H1~H5 对应实际 HTML 的 H2~H6
-- JSON中的 h1~h5 是用户视角的标题级别

-- 默认主题（与现有硬编码样式一致）
INSERT IGNORE INTO `sys_plugin` (`plugin_type`, `plugin_key`, `plugin_name`, `plugin_description`, `plugin_config`, `plugin_code`, `enabled`, `is_system`, `sort_order`) VALUES
('article_theme', 'default', '默认主题', '经典装饰风格，保留原始设计',
'{"headings":{"h1":{"emoji":"📑","color":null,"show":true,"paddingLeft":"40px"},"h2":{"emoji":"#","color":"#ff6d6d","show":true,"paddingLeft":"25px"},"h3":{"emoji":"▌","color":"#ff6d6d","show":true,"paddingLeft":"20px"},"h4":{"emoji":"🌷","color":null,"show":true,"paddingLeft":"28px"},"h5":{"emoji":"","color":null,"show":false,"paddingLeft":""}},"toc":{"emoji":"🏖️","show":true}}',
NULL, 1, 1, 0);

-- 简约主题
INSERT IGNORE INTO `sys_plugin` (`plugin_type`, `plugin_key`, `plugin_name`, `plugin_description`, `plugin_config`, `plugin_code`, `enabled`, `is_system`, `sort_order`) VALUES
('article_theme', 'minimal', '简约主题', '低调的灰色装饰，适合专注阅读',
'{"headings":{"h1":{"emoji":"","color":null,"show":false,"paddingLeft":""},"h2":{"emoji":"#","color":"#909399","show":true,"paddingLeft":"25px"},"h3":{"emoji":"▌","color":"#909399","show":true,"paddingLeft":"20px"},"h4":{"emoji":"","color":null,"show":false,"paddingLeft":""},"h5":{"emoji":"","color":null,"show":false,"paddingLeft":""}},"toc":{"emoji":"📖","show":true}}',
NULL, 1, 1, 1);

-- 无装饰主题
INSERT IGNORE INTO `sys_plugin` (`plugin_type`, `plugin_key`, `plugin_name`, `plugin_description`, `plugin_config`, `plugin_code`, `enabled`, `is_system`, `sort_order`) VALUES
('article_theme', 'plain', '无装饰', '移除所有标题装饰，纯净阅读体验',
'{"headings":{"h1":{"emoji":"","color":null,"show":false,"paddingLeft":""},"h2":{"emoji":"","color":null,"show":false,"paddingLeft":""},"h3":{"emoji":"","color":null,"show":false,"paddingLeft":""},"h4":{"emoji":"","color":null,"show":false,"paddingLeft":""},"h5":{"emoji":"","color":null,"show":false,"paddingLeft":""}},"toc":{"emoji":"","show":false}}',
NULL, 1, 1, 2);

-- 花园主题
INSERT IGNORE INTO `sys_plugin` (`plugin_type`, `plugin_key`, `plugin_name`, `plugin_description`, `plugin_config`, `plugin_code`, `enabled`, `is_system`, `sort_order`) VALUES
('article_theme', 'garden', '花园主题', '清新自然的植物装饰风格',
'{"headings":{"h1":{"emoji":"🌿","color":null,"show":true,"paddingLeft":"40px"},"h2":{"emoji":"🌱","color":null,"show":true,"paddingLeft":"36px"},"h3":{"emoji":"🍃","color":null,"show":true,"paddingLeft":"32px"},"h4":{"emoji":"🌷","color":null,"show":true,"paddingLeft":"32px"},"h5":{"emoji":"🌼","color":null,"show":true,"paddingLeft":"28px"}},"toc":{"emoji":"🌺","show":true}}',
NULL, 1, 1, 3);

-- 学术主题
INSERT IGNORE INTO `sys_plugin` (`plugin_type`, `plugin_key`, `plugin_name`, `plugin_description`, `plugin_config`, `plugin_code`, `enabled`, `is_system`, `sort_order`) VALUES
('article_theme', 'academic', '学术主题', '专业严谨的学术排版风格',
'{"headings":{"h1":{"emoji":"§","color":null,"show":true,"paddingLeft":"24px"},"h2":{"emoji":"¶","color":null,"show":true,"paddingLeft":"20px"},"h3":{"emoji":"▸","color":null,"show":true,"paddingLeft":"18px"},"h4":{"emoji":"•","color":null,"show":true,"paddingLeft":"16px"},"h5":{"emoji":"◦","color":null,"show":true,"paddingLeft":"16px"}},"toc":{"emoji":"📚","show":true}}',
NULL, 1, 1, 4);

-- 星空主题
INSERT IGNORE INTO `sys_plugin` (`plugin_type`, `plugin_key`, `plugin_name`, `plugin_description`, `plugin_config`, `plugin_code`, `enabled`, `is_system`, `sort_order`) VALUES
('article_theme', 'space', '星空主题', '梦幻星空装饰风格',
'{"headings":{"h1":{"emoji":"🌟","color":null,"show":true,"paddingLeft":"40px"},"h2":{"emoji":"⭐","color":null,"show":true,"paddingLeft":"33px"},"h3":{"emoji":"💫","color":null,"show":true,"paddingLeft":"31px"},"h4":{"emoji":"✨","color":null,"show":true,"paddingLeft":"31px"},"h5":{"emoji":"·","color":null,"show":true,"paddingLeft":"16px"}},"toc":{"emoji":"🛸","show":true}}',
NULL, 1, 1, 5);

-- ============================================
-- 2. 设置默认激活主题
-- ============================================
INSERT IGNORE INTO `sys_plugin_active` (`plugin_type`, `plugin_key`, `update_time`) VALUES
('article_theme', 'default', NOW());
