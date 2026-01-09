SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS poetize DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================
-- 数据库迁移版本记录表（必须在最前面）
-- ============================================
DROP TABLE IF EXISTS `poetize`.`db_migrations`;

CREATE TABLE `poetize`.`db_migrations` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `version` varchar(20) NOT NULL COMMENT '版本号（迁移脚本文件名，不含.sql后缀）',
  `description` varchar(200) DEFAULT NULL COMMENT '迁移说明',
  `executed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  `execution_time_ms` int DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `success` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否执行成功 [0:失败, 1:成功]',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_version` (`version`) COMMENT '版本号唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库迁移版本记录表';


DROP TABLE IF EXISTS `poetize`.`user`;

CREATE TABLE `poetize`.`user` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `username` varchar(32) DEFAULT NULL COMMENT '用户名',
  `password` varchar(128) DEFAULT NULL COMMENT '密码',
  `phone_number` varchar(16) DEFAULT NULL COMMENT '手机号',
  `email` varchar(32) DEFAULT NULL COMMENT '用户邮箱',
  `user_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用[0:否，1:是]',
  `gender` tinyint(2) DEFAULT NULL COMMENT '性别[1:男，2:女，0:保密]',
  `open_id` varchar(128) DEFAULT NULL COMMENT 'openId',
  `platform_type` varchar(32) DEFAULT NULL COMMENT '第三方平台类型[wx,qq,weibo等]',
  `uid` varchar(128) DEFAULT NULL COMMENT '第三方平台用户唯一标识',
  `avatar` varchar(256) DEFAULT NULL COMMENT '头像',
  `admire` varchar(32) DEFAULT NULL COMMENT '赞赏',
  `subscribe` text DEFAULT NULL COMMENT '订阅',
  `introduction` varchar(4096) DEFAULT NULL COMMENT '简介',
  `user_type` tinyint(2) NOT NULL DEFAULT 2 COMMENT '用户类型[0:admin，1:管理员，2:普通用户]',

  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最终修改时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '最终修改人',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用[0:未删除，1:已删除]',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

DROP TABLE IF EXISTS `poetize`.`article`;

CREATE TABLE `poetize`.`article` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` int NOT NULL COMMENT '用户ID',
  `sort_id` int NOT NULL COMMENT '分类ID',
  `label_id` int NOT NULL COMMENT '标签ID',
  `article_cover` varchar(256) DEFAULT NULL COMMENT '封面',
  `article_title` varchar(500) NOT NULL COMMENT '博文标题',
  `article_content` text NOT NULL COMMENT '博文内容',
  `summary` varchar(500) DEFAULT NULL COMMENT '文章摘要',
  `video_url` varchar(1024) DEFAULT NULL COMMENT '视频链接',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览量',
  `view_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可见[0:否，1:是]',
  `password` varchar(128) DEFAULT NULL COMMENT '密码',
  `tips` varchar(128) DEFAULT NULL COMMENT '提示',
  `recommend_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否推荐[0:否，1:是]',
  `comment_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用评论[0:否，1:是]',
  `submit_to_search_engine` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否推送至搜索引擎[0:否，1:是]',

  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime  DEFAULT CURRENT_TIMESTAMP COMMENT '最终修改时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '最终修改人',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用[0:未删除，1:已删除]',

  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

DROP TABLE IF EXISTS `poetize`.`comment`;

CREATE TABLE `poetize`.`comment` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `source` int NOT NULL COMMENT '评论来源标识',
  `type` varchar(32) NOT NULL COMMENT '评论来源类型',
  `parent_comment_id` int NOT NULL DEFAULT 0 COMMENT '父评论ID',
  `user_id` int NOT NULL COMMENT '发表用户ID',
  `floor_comment_id` int DEFAULT NULL COMMENT '楼层评论ID',
  `parent_user_id` int DEFAULT NULL COMMENT '父发表用户名ID',
  `like_count` int NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_content` varchar(1024) NOT NULL COMMENT '评论内容',
  `comment_info` varchar(256) DEFAULT NULL COMMENT '评论额外信息',
  `ip_address` varchar(45) DEFAULT NULL COMMENT 'IP地址',
  `location` varchar(100) DEFAULT NULL COMMENT '地理位置',

  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`),
  KEY `source` (`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章评论表';

DROP TABLE IF EXISTS `poetize`.`sort`;

CREATE TABLE `poetize`.`sort` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `sort_name` varchar(32) NOT NULL COMMENT '分类名称',
  `sort_description` varchar(256) NOT NULL COMMENT '分类描述',
  `sort_type` tinyint(2) NOT NULL DEFAULT 1 COMMENT '分类类型[0:导航栏分类，1:普通分类]',
  `priority` int DEFAULT NULL COMMENT '分类优先级：数字小的在前面',

  PRIMARY KEY (`id`)
) ENGINE=Aria DEFAULT CHARSET=utf8mb4 COMMENT='分类';

DROP TABLE IF EXISTS `poetize`.`label`;

CREATE TABLE `poetize`.`label` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `sort_id` int NOT NULL COMMENT '分类ID',
  `label_name` varchar(32) NOT NULL COMMENT '标签名称',
  `label_description` varchar(256) NOT NULL COMMENT '标签描述',

  PRIMARY KEY (`id`)
) ENGINE=Aria DEFAULT CHARSET=utf8mb4 COMMENT='标签';

DROP TABLE IF EXISTS `poetize`.`tree_hole`;

CREATE TABLE `poetize`.`tree_hole` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `avatar` varchar(256) DEFAULT NULL COMMENT '头像',
  `message` varchar(64) NOT NULL COMMENT '留言',

  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='树洞';

DROP TABLE IF EXISTS `poetize`.`wei_yan`;

CREATE TABLE `poetize`.`wei_yan` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` int NOT NULL COMMENT '用户ID',
  `like_count` int NOT NULL DEFAULT 0 COMMENT '点赞数',
  `content` varchar(1024) NOT NULL COMMENT '内容',
  `type` varchar(32) NOT NULL COMMENT '类型',
  `source` int DEFAULT NULL COMMENT '来源标识',
  `is_public` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否公开[0:仅自己可见，1:所有人可见]',

  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微言表';

DROP TABLE IF EXISTS `poetize`.`web_info`;

CREATE TABLE `poetize`.`web_info` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `web_name` varchar(16) NOT NULL COMMENT '网站名称',
  `web_title` varchar(512) NOT NULL COMMENT '网站信息',
  `site_address` varchar(255) DEFAULT NULL COMMENT '网站地址（完整URL）',
  `notices` varchar(512) DEFAULT NULL COMMENT '公告',
  `footer` varchar(256) NOT NULL COMMENT '页脚',
  `background_image` varchar(256) DEFAULT NULL COMMENT '背景',
  `avatar` varchar(256) NOT NULL COMMENT '头像',
  `random_avatar` text DEFAULT NULL COMMENT '随机头像',
  `random_name` varchar(4096) DEFAULT NULL COMMENT '随机名称',
  `random_cover` text DEFAULT NULL COMMENT '随机封面',
  `waifu_json` text DEFAULT NULL COMMENT '看板娘消息',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用[0:否，1:是]',
  `home_page_pull_up_height` int(11) NULL DEFAULT -1 COMMENT '首页上拉高度',
  `api_enabled` tinyint(1) DEFAULT 0 COMMENT 'API是否启用[0:否，1:是]',
  `api_key` varchar(255) DEFAULT NULL COMMENT 'API密钥',
  `nav_config` text DEFAULT NULL COMMENT '导航栏配置JSON',
  `enable_waifu` tinyint(1) DEFAULT 0 COMMENT '看板娘是否启用[0:否，1:是]',
  `waifu_display_mode` varchar(20) DEFAULT 'live2d' COMMENT '看板娘显示模式 [live2d:Live2D看板娘, button:简洁按钮]',
  `footer_background_image` varchar(256) DEFAULT NULL COMMENT '页脚背景图片',
  `footer_background_config` text DEFAULT NULL COMMENT '页脚背景图片位置配置(JSON格式)',
  `email` varchar(255) DEFAULT NULL COMMENT '联系邮箱',
  `minimal_footer` tinyint(1) DEFAULT 0 COMMENT '极简页脚开关[0:否，1:是]',
  `enable_auto_night` tinyint(1) DEFAULT 0 COMMENT '自动夜间开关[0:否，1:是]',
  `auto_night_start` int DEFAULT 23 COMMENT '夜间开始时间(小时)',
  `auto_night_end` int DEFAULT 7 COMMENT '夜间结束时间(小时)',
  `enable_gray_mode` tinyint(1) DEFAULT 0 COMMENT '灰色模式开关[0:否，1:是]',
  `enable_dynamic_title` tinyint(1) DEFAULT 1 COMMENT '动态标题开关[0:否，1:是]',
  `mobile_drawer_config` TEXT NULL COMMENT '移动端侧边栏配置(JSON格式)',
  `mouse_click_effect` VARCHAR(20) DEFAULT 'none' COMMENT '鼠标点击效果类型 [none:无, text:文字, firework:烟花]',
  `mouse_click_effect_config` TEXT COMMENT '鼠标点击特效配置JSON',

  PRIMARY KEY (`id`)
) ENGINE=Aria DEFAULT CHARSET=utf8mb4 COMMENT='网站信息表';

DROP TABLE IF EXISTS `poetize`.`resource_path`;

CREATE TABLE `poetize`.`resource_path` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `title` varchar(64) NOT NULL COMMENT '标题',
  `classify` varchar(32) DEFAULT NULL COMMENT '分类',
  `cover` varchar(256) DEFAULT NULL COMMENT '封面',
  `url` varchar(256) DEFAULT NULL COMMENT '链接',
  `introduction` varchar(1024) DEFAULT NULL COMMENT '简介',
  `type` varchar(32) NOT NULL COMMENT '资源类型',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用[0:否，1:是]',
  `remark` text DEFAULT NULL COMMENT '备注',

  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`)
) ENGINE=Aria DEFAULT CHARSET=utf8mb4 COMMENT='资源聚合';

DROP TABLE IF EXISTS `poetize`.`resource`;

CREATE TABLE `poetize`.`resource` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` int NOT NULL COMMENT '用户ID',
  `type` varchar(32) NOT NULL COMMENT '资源类型',
  `path` varchar(256) NOT NULL COMMENT '资源路径',
  `size` int DEFAULT NULL COMMENT '资源内容的大小，单位：字节',
  `original_name` varchar(512) DEFAULT NULL COMMENT '文件名称',
  `mime_type` varchar(256) DEFAULT NULL COMMENT '资源的 MIME 类型',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用[0:否，1:是]',
  `store_type` varchar(16) DEFAULT NULL COMMENT '存储平台',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_path` (`path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源信息';

DROP TABLE IF EXISTS `poetize`.`history_info`;

CREATE TABLE `poetize`.`history_info` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` int DEFAULT NULL COMMENT '用户ID',
  `ip` varchar(128) NOT NULL COMMENT 'ip',
  `nation` varchar(64) DEFAULT NULL COMMENT '国家',
  `province` varchar(64) DEFAULT NULL COMMENT '省份',
  `city` varchar(64) DEFAULT NULL COMMENT '城市',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=RocksDB DEFAULT CHARSET=utf8mb4 COMMENT='历史信息';

DROP TABLE IF EXISTS `poetize`.`sys_config`;

CREATE TABLE `poetize`.`sys_config` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `config_name` varchar(128) NOT NULL COMMENT '名称',
  `config_key` varchar(64) NOT NULL COMMENT '键名',
  `config_value` text DEFAULT NULL COMMENT '键值',
  `config_type` char(1) NOT NULL COMMENT '1 私用 2 公开',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key_type` (`config_key`,`config_type`) COMMENT '配置键和类型联合唯一索引'
) ENGINE=Aria DEFAULT CHARSET=utf8mb4 COMMENT='参数配置表';

DROP TABLE IF EXISTS `poetize`.`sys_plugin`;

CREATE TABLE `poetize`.`sys_plugin` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '插件ID',
  `plugin_type` varchar(50) NOT NULL COMMENT '插件类型 [mouse_click_effect: 鼠标点击效果]',
  `plugin_key` varchar(50) NOT NULL COMMENT '插件唯一标识符',
  `plugin_name` varchar(100) NOT NULL COMMENT '插件名称',
  `plugin_description` varchar(500) DEFAULT NULL COMMENT '插件描述',
  `plugin_config` text DEFAULT NULL COMMENT '插件配置(JSON格式)',
  `plugin_code` text DEFAULT NULL COMMENT '插件代码(JavaScript)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用 [0:禁用, 1:启用]',
  `is_system` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否系统内置 [0:用户创建, 1:系统内置]',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_type_key` (`plugin_type`, `plugin_key`) COMMENT '插件类型和标识符联合唯一索引',
  KEY `idx_plugin_type` (`plugin_type`) COMMENT '插件类型索引',
  KEY `idx_enabled` (`enabled`) COMMENT '启用状态索引'
) ENGINE=Aria DEFAULT CHARSET=utf8mb4 COMMENT='插件配置表';

DROP TABLE IF EXISTS `poetize`.`sys_plugin_active`;

CREATE TABLE `poetize`.`sys_plugin_active` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `plugin_type` varchar(50) NOT NULL COMMENT '插件类型',
  `plugin_key` varchar(50) NOT NULL COMMENT '当前激活的插件标识符',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_type` (`plugin_type`) COMMENT '每种插件类型只能有一个激活项'
) ENGINE=Aria DEFAULT CHARSET=utf8mb4 COMMENT='插件激活状态表';

DROP TABLE IF EXISTS `poetize`.`sys_mail_config`;

CREATE TABLE `poetize`.`sys_mail_config` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_name` varchar(100) NOT NULL COMMENT '配置名称',
  `host` varchar(255) NOT NULL COMMENT '邮箱服务器地址',
  `port` int NOT NULL DEFAULT 25 COMMENT '邮箱服务器端口',
  `username` varchar(255) NOT NULL COMMENT '邮箱账号',
  `password` varchar(500) DEFAULT NULL COMMENT '邮箱密码或授权码（加密存储）',
  `sender_name` varchar(100) DEFAULT NULL COMMENT '发件人名称',
  `use_ssl` tinyint(1) DEFAULT 0 COMMENT '是否启用SSL (0:否 1:是)',
  `use_starttls` tinyint(1) DEFAULT 0 COMMENT '是否启用STARTTLS (0:否 1:是)',
  `auth` tinyint(1) DEFAULT 1 COMMENT '是否需要认证 (0:否 1:是)',
  `enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用 (0:禁用 1:启用)',
  `is_default` tinyint(1) DEFAULT 0 COMMENT '是否为默认配置 (0:否 1:是)',
  `connection_timeout` int DEFAULT 25000 COMMENT '连接超时(毫秒)',
  `timeout` int DEFAULT 25000 COMMENT '读取超时(毫秒)',
  `trust_all_certs` tinyint(1) DEFAULT 0 COMMENT '是否信任所有证书 (0:否 1:是)',
  `protocol` varchar(50) DEFAULT 'smtp' COMMENT '协议(smtp, smtps等)',
  `auth_mechanism` varchar(50) DEFAULT NULL COMMENT '认证机制(LOGIN, PLAIN, DIGEST-MD5等)',
  `debug` tinyint(1) DEFAULT 0 COMMENT '是否启用调试模式 (0:否 1:是)',
  `use_proxy` tinyint(1) DEFAULT 0 COMMENT '是否使用代理 (0:否 1:是)',
  `proxy_host` varchar(255) DEFAULT NULL COMMENT '代理服务器地址',
  `proxy_port` int DEFAULT NULL COMMENT '代理服务器端口',
  `proxy_user` varchar(100) DEFAULT NULL COMMENT '代理服务器用户名',
  `proxy_password` varchar(500) DEFAULT NULL COMMENT '代理服务器密码',
  `custom_properties` text DEFAULT NULL COMMENT '自定义属性(JSON格式)',
  `sort_order` int DEFAULT 0 COMMENT '排序顺序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_is_default` (`is_default`)
) ENGINE=Aria DEFAULT CHARSET=utf8mb4 COMMENT='邮件配置表';

DROP TABLE IF EXISTS `poetize`.`sys_captcha_config`;

CREATE TABLE `poetize`.`sys_captcha_config` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `enable` tinyint(1) DEFAULT 0 COMMENT '是否启用验证码 (0:否 1:是)',
  `login` tinyint(1) DEFAULT 1 COMMENT '登录时启用 (0:否 1:是)',
  `register` tinyint(1) DEFAULT 1 COMMENT '注册时启用 (0:否 1:是)',
  `comment` tinyint(1) DEFAULT 0 COMMENT '评论时启用 (0:否 1:是)',
  `reset_password` tinyint(1) DEFAULT 1 COMMENT '重置密码时启用 (0:否 1:是)',
  `screen_size_threshold` int DEFAULT 768 COMMENT '屏幕宽度阈值(px)',
  `force_slide_for_mobile` tinyint(1) DEFAULT 1 COMMENT '移动端强制滑动 (0:否 1:是)',
  `slide_accuracy` int DEFAULT 5 COMMENT '滑动验证码精确度',
  `slide_success_threshold` decimal(3,2) DEFAULT 0.95 COMMENT '滑动成功阈值',
  `checkbox_track_sensitivity` decimal(3,2) DEFAULT 0.90 COMMENT '勾选轨迹敏感度',
  `checkbox_min_track_points` int DEFAULT 2 COMMENT '勾选最少轨迹点数',
  `checkbox_reply_sensitivity` decimal(3,2) DEFAULT 0.85 COMMENT '回复评论敏感度',
  `checkbox_max_retry_count` int DEFAULT 5 COMMENT '最大重试次数',
  `checkbox_retry_decrement` decimal(3,2) DEFAULT 0.02 COMMENT '重试降低敏感度',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=Aria DEFAULT CHARSET=utf8mb4 COMMENT='验证码配置表';

DROP TABLE IF EXISTS `poetize`.`third_party_oauth_config`;

CREATE TABLE `poetize`.`third_party_oauth_config` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_type` varchar(32) NOT NULL COMMENT '平台类型（github, google, twitter, yandex, gitee等）',
  `platform_name` varchar(64) DEFAULT NULL COMMENT '平台名称',
  `client_id` varchar(256) DEFAULT NULL COMMENT '客户端ID',
  `client_secret` varchar(512) DEFAULT NULL COMMENT '客户端密钥',
  `client_key` varchar(256) DEFAULT NULL COMMENT '客户端Key（Twitter使用）',
  `redirect_uri` varchar(512) DEFAULT NULL COMMENT '重定向URI',
  `scope` varchar(256) DEFAULT NULL COMMENT '授权范围',
  `enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用该平台[0:否，1:是]',
  `global_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '全局是否启用第三方登录[0:否，1:是]',
  `sort_order` int DEFAULT 0 COMMENT '排序顺序',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',

  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除[0:未删除，1:已删除]',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_type` (`platform_type`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_global_enabled` (`global_enabled`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=Aria DEFAULT CHARSET=utf8mb4 COMMENT='第三方OAuth登录配置表';

DROP TABLE IF EXISTS `poetize`.`family`;

CREATE TABLE `poetize`.`family` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` int NOT NULL COMMENT '用户ID',
  `bg_cover` varchar(256) NOT NULL COMMENT '背景封面',
  `man_cover` varchar(256) NOT NULL COMMENT '男生头像',
  `woman_cover` varchar(256) NOT NULL COMMENT '女生头像',
  `man_name` varchar(32) NOT NULL COMMENT '男生昵称',
  `woman_name` varchar(32) NOT NULL COMMENT '女生昵称',
  `timing` varchar(32) NOT NULL COMMENT '计时',
  `countdown_title` varchar(32) DEFAULT NULL COMMENT '倒计时标题',
  `countdown_time` varchar(32) DEFAULT NULL COMMENT '倒计时时间',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用[0:否，1:是]',
  `family_info` varchar(1024) DEFAULT NULL COMMENT '额外信息',
  `like_count` int NOT NULL DEFAULT 0 COMMENT '点赞数',

  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最终修改时间',

  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭信息';


DROP TABLE IF EXISTS `poetize`.`im_chat_user_friend`;

CREATE TABLE `poetize`.`im_chat_user_friend` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` int NOT NULL COMMENT '用户ID',
  `friend_id` int NOT NULL COMMENT '好友ID',
  `friend_status` tinyint(2) NOT NULL COMMENT '朋友状态[0:未审核，1:审核通过]',
  `remark` varchar(32) DEFAULT NULL COMMENT '备注',

  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友';

DROP TABLE IF EXISTS `poetize`.`im_chat_group`;

CREATE TABLE `poetize`.`im_chat_group` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `group_name` varchar(32) NOT NULL COMMENT '群名称',
  `master_user_id` int NOT NULL COMMENT '群主用户ID',
  `avatar` varchar(256) DEFAULT NULL COMMENT '群头像',
  `introduction` varchar(128) DEFAULT NULL COMMENT '简介',
  `notice` varchar(1024) DEFAULT NULL COMMENT '公告',
  `in_type` tinyint(1) NOT NULL DEFAULT 1 COMMENT '进入方式[0:无需验证，1:需要群主或管理员同意]',
  `group_type` tinyint(2) NOT NULL DEFAULT 1 COMMENT '类型[1:聊天群，2:话题]',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天群';

DROP TABLE IF EXISTS `poetize`.`im_chat_group_user`;

CREATE TABLE `poetize`.`im_chat_group_user` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `group_id` int NOT NULL COMMENT '群ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `verify_user_id` int DEFAULT NULL COMMENT '审核用户ID',
  `remark` varchar(1024) DEFAULT NULL COMMENT '备注',
  `admin_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否管理员[0:否，1:是]',
  `user_status` tinyint(2) NOT NULL COMMENT '用户状态[0:未审核，1:审核通过，2:禁言]',

  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天群成员';

DROP TABLE IF EXISTS `poetize`.`im_chat_user_message`;

CREATE TABLE `poetize`.`im_chat_user_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `from_id` int NOT NULL COMMENT '发送ID',
  `to_id` int NOT NULL COMMENT '接收ID',
  `content` varchar(1024) NOT NULL COMMENT '内容',
  `message_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已读[0:未读，1:已读]',

  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`),
  KEY `union_index` (`to_id`,`message_status`)
) ENGINE=RocksDB DEFAULT CHARSET=utf8mb4 COMMENT='单聊记录';

DROP TABLE IF EXISTS `poetize`.`im_chat_user_group_message`;

CREATE TABLE `poetize`.`im_chat_user_group_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `group_id` int NOT NULL COMMENT '群ID',
  `from_id` int NOT NULL COMMENT '发送ID',
  `to_id` int DEFAULT NULL COMMENT '接收ID',
  `content` varchar(1024) NOT NULL COMMENT '内容',

  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`)
) ENGINE=RocksDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊记录';

DROP TABLE IF EXISTS `poetize`.`im_chat_last_read`;

CREATE TABLE `poetize`.`im_chat_last_read` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` int NOT NULL COMMENT '用户ID',
  `chat_type` tinyint NOT NULL COMMENT '聊天类型[1:私聊，2:群聊]',
  `chat_id` int NOT NULL COMMENT '聊天ID（私聊为friendId，群聊为groupId）',
  `last_read_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后查看时间',
  `is_hidden` tinyint NOT NULL DEFAULT 0 COMMENT '是否隐藏[0:否，1:是]',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_chat` (`user_id`, `chat_type`, `chat_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_chat` (`chat_type`, `chat_id`),
  KEY `idx_hidden` (`user_id`, `is_hidden`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天最后查看时间（私聊+群聊）';

DROP TABLE IF EXISTS `poetize`.`article_translation`;

CREATE TABLE `poetize`.`article_translation` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `article_id` int NOT NULL COMMENT '文章ID',
  `language` varchar(10) NOT NULL COMMENT '语言代码',
  `title` varchar(500) DEFAULT NULL COMMENT '翻译后的标题',
  `content` text DEFAULT NULL COMMENT '翻译后的内容',
  `summary` text DEFAULT NULL COMMENT '文章摘要',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_language` (`article_id`, `language`),
  KEY `idx_article_id` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章翻译内容表';

-- 主SEO配置表
CREATE TABLE IF NOT EXISTS `poetize`.`seo_config` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `enable` tinyint(1) DEFAULT 1 COMMENT 'SEO功能总开关',
  `site_description` text COMMENT '网站描述',
  `site_keywords` text COMMENT '网站关键词',
  `site_logo` varchar(512) COMMENT '网站Logo',
  `site_icon` varchar(512) COMMENT '网站图标',
  `site_icon_192` varchar(512) COMMENT '网站图标192x192',
  `site_icon_512` varchar(512) COMMENT '网站图标512x512',
  `apple_touch_icon` varchar(512) COMMENT 'Apple Touch图标',
  `site_short_name` varchar(64) COMMENT '网站短名称',
  `default_author` varchar(128) DEFAULT 'Admin' COMMENT '默认作者',
  `custom_head_code` text COMMENT '自定义头部代码',
  `robots_txt` text COMMENT 'robots.txt内容',
  `auto_generate_meta_tags` tinyint(1) DEFAULT 1 COMMENT '自动生成元标签',
  `generate_sitemap` tinyint(1) DEFAULT 1 COMMENT '生成站点地图',
  `sitemap_change_frequency` varchar(32) DEFAULT 'weekly' COMMENT '站点地图更新频率',
  `sitemap_priority` varchar(8) DEFAULT '0.7' COMMENT '站点地图优先级',
  `sitemap_exclude` varchar(512) COMMENT '站点地图排除路径',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` varchar(32) DEFAULT 'system' COMMENT '更新人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SEO主配置表';

-- 搜索引擎推送配置表
CREATE TABLE IF NOT EXISTS `poetize`.`seo_search_engine_push` (
  `id` int NOT NULL AUTO_INCREMENT,
  `seo_config_id` int NOT NULL COMMENT 'SEO配置ID',
  `engine_name` varchar(32) NOT NULL COMMENT '搜索引擎名称(baidu,google,bing等)',
  `engine_display_name` varchar(64) COMMENT '搜索引擎显示名称',
  `push_enabled` tinyint(1) DEFAULT 0 COMMENT '是否启用推送',
  `api_key` varchar(512) COMMENT 'API密钥(加密存储)',
  `api_token` varchar(512) COMMENT 'API令牌(加密存储)',
  `push_url` varchar(512) COMMENT '推送URL',
  `push_delay_seconds` int DEFAULT 300 COMMENT '推送延迟秒数',
  `last_push_time` datetime COMMENT '最后推送时间',
  `push_count` int DEFAULT 0 COMMENT '推送次数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seo_engine` (`seo_config_id`, `engine_name`),
  KEY `idx_engine_name` (`engine_name`),
  FOREIGN KEY (`seo_config_id`) REFERENCES `seo_config`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索引擎推送配置表';

-- 网站验证配置表
CREATE TABLE IF NOT EXISTS `poetize`.`seo_site_verification` (
  `id` int NOT NULL AUTO_INCREMENT,
  `seo_config_id` int NOT NULL COMMENT 'SEO配置ID',
  `platform` varchar(32) NOT NULL COMMENT '平台名称(baidu,google,bing等)',
  `platform_display_name` varchar(64) COMMENT '平台显示名称',
  `verification_code` varchar(512) COMMENT '验证代码',
  `verification_method` varchar(32) DEFAULT 'meta_tag' COMMENT '验证方式(meta_tag,html_file,dns)',
  `is_verified` tinyint(1) DEFAULT 0 COMMENT '是否已验证',
  `verified_time` datetime COMMENT '验证时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seo_platform` (`seo_config_id`, `platform`),
  FOREIGN KEY (`seo_config_id`) REFERENCES `seo_config`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网站验证配置表';

-- 社交媒体配置表
CREATE TABLE IF NOT EXISTS `poetize`.`seo_social_media` (
  `id` int NOT NULL AUTO_INCREMENT,
  `seo_config_id` int NOT NULL COMMENT 'SEO配置ID',
  `twitter_card` varchar(32) DEFAULT 'summary_large_image' COMMENT 'Twitter卡片类型',
  `twitter_site` varchar(128) COMMENT 'Twitter站点账号',
  `twitter_creator` varchar(128) COMMENT 'Twitter创建者账号',
  `og_type` varchar(32) DEFAULT 'article' COMMENT 'Open Graph类型',
  `og_site_name` varchar(128) DEFAULT 'POETIZE' COMMENT 'Open Graph站点名称',
  `og_image` varchar(512) COMMENT 'Open Graph图片',
  `fb_app_id` varchar(128) COMMENT 'Facebook应用ID',
  `fb_page_url` varchar(512) COMMENT 'Facebook页面URL',
  `linkedin_company_id` varchar(128) COMMENT 'LinkedIn公司ID',
  `linkedin_mode` varchar(32) DEFAULT 'standard' COMMENT 'LinkedIn模式',
  `pinterest_verification` varchar(512) COMMENT 'Pinterest验证码',
  `pinterest_description` varchar(512) COMMENT 'Pinterest描述',
  `wechat_miniprogram_path` varchar(512) COMMENT '微信小程序路径',
  `wechat_miniprogram_id` varchar(128) COMMENT '微信小程序ID',
  `qq_miniprogram_path` varchar(512) COMMENT 'QQ小程序路径',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seo_social` (`seo_config_id`),
  FOREIGN KEY (`seo_config_id`) REFERENCES `seo_config`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社交媒体配置表';

-- PWA配置表
CREATE TABLE IF NOT EXISTS `poetize`.`seo_pwa_config` (
  `id` int NOT NULL AUTO_INCREMENT,
  `seo_config_id` int NOT NULL COMMENT 'SEO配置ID',
  `pwa_display` varchar(32) DEFAULT 'standalone' COMMENT 'PWA显示模式',
  `pwa_background_color` varchar(16) DEFAULT '#ffffff' COMMENT 'PWA背景颜色',
  `pwa_theme_color` varchar(16) DEFAULT '#1976d2' COMMENT 'PWA主题颜色',
  `pwa_orientation` varchar(32) DEFAULT 'portrait-primary' COMMENT 'PWA屏幕方向',
  `pwa_screenshot_desktop` varchar(512) COMMENT 'PWA桌面截图',
  `pwa_screenshot_mobile` varchar(512) COMMENT 'PWA移动端截图',
  `android_app_id` varchar(128) COMMENT 'Android应用ID',
  `ios_app_id` varchar(128) COMMENT 'iOS应用ID',
  `prefer_native_apps` tinyint(1) DEFAULT 0 COMMENT '是否优先使用原生应用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seo_pwa` (`seo_config_id`),
  FOREIGN KEY (`seo_config_id`) REFERENCES `seo_config`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PWA配置表';

-- 通知配置表
CREATE TABLE IF NOT EXISTS `poetize`.`seo_notification_config` (
  `id` int NOT NULL AUTO_INCREMENT,
  `seo_config_id` int NOT NULL COMMENT 'SEO配置ID',
  `push_delay_seconds` int DEFAULT 300 COMMENT '推送延迟秒数',
  `enable_push_notification` tinyint(1) DEFAULT 0 COMMENT '启用推送通知',
  `notify_only_on_failure` tinyint(1) DEFAULT 0 COMMENT '仅失败时通知',
  `notification_email` varchar(256) COMMENT '通知邮箱',
  `notification_webhook` varchar(512) COMMENT '通知Webhook',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seo_notification` (`seo_config_id`),
  FOREIGN KEY (`seo_config_id`) REFERENCES `seo_config`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知配置表';

-- AI配置统一管理表
CREATE TABLE IF NOT EXISTS `sys_ai_config` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_type` varchar(50) NOT NULL COMMENT '配置类型 (ai_chat:AI聊天 ai_api:AI接口 article_ai:文章AI助手)',
  `config_name` varchar(100) DEFAULT 'default' COMMENT '配置名称/标识',
  `enabled` tinyint(1) DEFAULT 0 COMMENT '是否启用 (0:否 1:是)',
  
  -- ========== 通用AI配置字段 ==========
  `provider` varchar(50) DEFAULT NULL COMMENT 'AI服务提供商 (openai/anthropic/custom/deepseek/siliconflow等)',
  `api_key` varchar(500) DEFAULT NULL COMMENT 'API密钥(加密存储)',
  `api_base` varchar(500) DEFAULT NULL COMMENT 'API基础地址',
  `model` varchar(100) DEFAULT NULL COMMENT '模型名称',
  
  -- ========== AI聊天参数配置 ==========
  `temperature` decimal(3,2) DEFAULT 0.70 COMMENT '温度参数(0.0-2.0)',
  `max_tokens` int DEFAULT 1000 COMMENT '最大生成令牌数',
  `top_p` decimal(3,2) DEFAULT 1.00 COMMENT 'Top-p采样参数(0.0-1.0)',
  `frequency_penalty` decimal(3,2) DEFAULT 0.00 COMMENT '频率惩罚(-2.0到2.0)',
  `presence_penalty` decimal(3,2) DEFAULT 0.00 COMMENT '存在惩罚(-2.0到2.0)',
  
  -- ========== AI聊天外观设置 ==========
  `chat_name` varchar(50) DEFAULT 'AI助手' COMMENT '聊天助手名称',
  `chat_avatar` varchar(500) DEFAULT '' COMMENT '聊天助手头像URL',
  `welcome_message` varchar(500) DEFAULT '你好！我是你的AI助手，有什么可以帮助你的吗？' COMMENT '欢迎消息',
  `placeholder_text` varchar(200) DEFAULT '输入你想说的话...' COMMENT '输入框占位文本',
  `theme_color` varchar(20) DEFAULT '#4facfe' COMMENT '主题颜色',
  
  -- ========== AI聊天功能设置 ==========
  `max_conversation_length` int DEFAULT 20 COMMENT '对话历史最大长度',
  `enable_context` tinyint(1) DEFAULT 1 COMMENT '启用上下文 (0:否 1:是)',
  `enable_typing_indicator` tinyint(1) DEFAULT 1 COMMENT '启用输入指示器 (0:否 1:是)',
  `response_delay` int DEFAULT 1000 COMMENT '响应延迟(毫秒)',
  `enable_quick_actions` tinyint(1) DEFAULT 1 COMMENT '启用快捷操作 (0:否 1:是)',
  `enable_chat_history` tinyint(1) DEFAULT 1 COMMENT '启用聊天历史 (0:否 1:是)',
  `enable_streaming` tinyint(1) DEFAULT 0 COMMENT '启用流式响应 (0:否 1:是)',
  `rate_limit` int DEFAULT 20 COMMENT '速率限制(每分钟消息数)',
  `max_message_length` int DEFAULT 500 COMMENT '单条消息最大长度',
  `require_login` tinyint(1) DEFAULT 1 COMMENT '需要登录 (0:否 1:是)',
  `enable_content_filter` tinyint(1) DEFAULT 1 COMMENT '启用内容过滤 (0:否 1:是)',
  
  -- ========== AI聊天高级功能 ==========
  `custom_instructions` text DEFAULT NULL COMMENT '自定义指令/系统提示词',
  `enable_thinking` tinyint(1) DEFAULT 0 COMMENT '启用思考模式 (0:否 1:是)',
  `enable_tools` tinyint(1) DEFAULT 1 COMMENT '启用MCP工具 (0:否 1:是)',
  
  -- ========== 记忆管理功能 ==========
  `enable_memory` tinyint(1) DEFAULT 0 COMMENT '启用Mem0记忆功能 (0:否 1:是)',
  `mem0_api_key` varchar(500) DEFAULT NULL COMMENT 'Mem0 API密钥(加密存储)',
  `memory_auto_save` tinyint(1) DEFAULT 1 COMMENT '自动保存对话记忆 (0:否 1:是)',
  `memory_auto_recall` tinyint(1) DEFAULT 1 COMMENT '自动检索相关记忆 (0:否 1:是)',
  `memory_recall_limit` int DEFAULT 3 COMMENT '检索记忆数量限制',
  
  -- ========== 文章AI助手配置字段 ==========
  `translation_type` varchar(20) DEFAULT 'none' COMMENT '翻译实现方式 (none:不翻译 baidu:百度翻译 custom:自定义API llm:使用全局AI模型 dedicated_llm:使用翻译独立AI模型)',
  `default_source_lang` varchar(10) DEFAULT 'zh' COMMENT '默认源语言',
  `default_target_lang` varchar(10) DEFAULT 'en' COMMENT '默认目标语言',
  
  -- ========== AI API配置字段 ==========
  `include_articles` tinyint(1) DEFAULT 0 COMMENT '包含文章数据 (0:否 1:是)',
  
  -- ========== JSON扩展字段 ==========
  `baidu_config` json DEFAULT NULL COMMENT '百度翻译配置 {app_id, app_secret}',
  `custom_config` json DEFAULT NULL COMMENT '自定义API配置 {api_url, api_key, app_secret}',
  `llm_config` json DEFAULT NULL COMMENT 'LLM配置 {model, api_url, api_key, prompt, interface_type, timeout}',
  `translation_llm_config` json DEFAULT NULL COMMENT '翻译独立AI配置 {model, api_url, api_key, prompt, interface_type, timeout}',
  `summary_config` json DEFAULT NULL COMMENT '摘要生成配置 {summaryMode, style, max_length, prompt, dedicated_llm}',
  `extra_config` json DEFAULT NULL COMMENT '其他扩展配置(JSON格式)',
  
  -- ========== 元数据字段 ==========
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_type_name` (`config_type`, `config_name`) COMMENT '配置类型和名称联合唯一索引',
  KEY `idx_config_type` (`config_type`) COMMENT '配置类型索引',
  KEY `idx_enabled` (`enabled`) COMMENT '启用状态索引'
) ENGINE=Aria DEFAULT CHARSET=utf8mb4 COMMENT='AI配置统一管理表';

-- ============================================================
-- 插入默认配置数据
-- ============================================================

-- 1. AI聊天配置默认值
INSERT INTO `sys_ai_config` (
  `config_type`, `config_name`, `enabled`,
  `provider`, `api_key`, `api_base`, `model`,
  `temperature`, `max_tokens`, `top_p`, `frequency_penalty`, `presence_penalty`,
  `chat_name`, `chat_avatar`, `welcome_message`, `placeholder_text`, `theme_color`,
  `max_conversation_length`, `enable_context`, `enable_typing_indicator`, 
  `response_delay`, `enable_quick_actions`, `enable_chat_history`, `enable_streaming`,
  `rate_limit`, `max_message_length`, `require_login`, `enable_content_filter`,
  `custom_instructions`, `enable_thinking`, `enable_tools`,
  `enable_memory`, `mem0_api_key`, `memory_auto_save`, `memory_auto_recall`, `memory_recall_limit`,
  `remark`
) VALUES (
  'ai_chat', 'default', 0,
  'openai', '', '', 'gpt-3.5-turbo',
  0.70, 1000, 1.00, 0.00, 0.00,
  'AI助手', '', '你好！我是你的AI助手，有什么可以帮助你的吗？', '输入你想说的话...', '#4facfe',
  20, 1, 1,
  1000, 1, 1, 0,
  20, 500, 1, 1,
  '', 0, 1,
  0, '', 1, 1, 3,
  'AI聊天默认配置'
) ON DUPLICATE KEY UPDATE id=id;

-- 2. AI API配置默认值
INSERT INTO `sys_ai_config` (
  `config_type`, `config_name`, `enabled`,
  `provider`, `api_key`, `api_base`, `model`,
  `include_articles`,
  `remark`
) VALUES (
  'ai_api', 'default', 0,
  'openai', '', '', 'gpt-3.5-turbo',
  0,
  'AI API默认配置'
) ON DUPLICATE KEY UPDATE id=id;

-- 3. 文章AI助手配置默认值
INSERT INTO `sys_ai_config` (
  `config_type`, `config_name`, `enabled`,
  `translation_type`, `default_source_lang`, `default_target_lang`,
  `llm_config`, `summary_config`,
  `remark`
) VALUES (
  'article_ai', 'default', 0,
  'none', 'zh', 'en',
  JSON_OBJECT(
    'model', 'gpt-3.5-turbo',
    'api_url', '',
    'api_key', '',
    'prompt', '将以下TOON格式数据从{source_lang}翻译为{target_lang}。\n\n规则：\n1. 保持TOON格式结构不变（2个空格缩进）\n2. 翻译title和content的值\n3. 保持Markdown格式\n4. 只返回TOON格式数据，不添加任何解释\n\n输入TOON数据：\n{toon_data}\n\n请返回翻译后的TOON数据，格式如下：\narticle:\n  title: (翻译后的{target_lang}标题)\n  content: (翻译后的{target_lang}内容)',
    'interface_type', 'openai',
    'timeout', 240
  ),
  JSON_OBJECT(
    'summaryMode', 'global',
    'style', 'concise',
    'max_length', 150,
    'prompt', '请为以下{source_lang}文章生成多语言摘要，要求：\n1. 生成语言：{languages}\n2. 风格：{style_desc}\n3. 每个语言的摘要长度控制在{max_length}字符以内\n4. 保持TOON格式结构不变（2个空格缩进）\n5. 只返回TOON格式数据，不添加任何解释或markdown代码块标记\n6. 注意：为每个目标语言生成该语言的摘要（如需要英文摘要，则生成英文；如需要日文摘要，则生成日文）\n\n文章内容：\n\n{source_content}\n\n请返回TOON格式的摘要，格式如下：\n{toon_example}'
  ),
  '文章AI助手配置默认值'
) ON DUPLICATE KEY UPDATE id=id;


-- ============================================================
-- 字段说明和使用指南
-- ============================================================

-- 配置类型说明：
-- 1. ai_chat: AI聊天配置
--    - 主要字段：provider, api_key, api_base, model, temperature等聊天参数
--    - 外观字段：chat_name, chat_avatar, welcome_message, theme_color
--    - 功能字段：enable_streaming, rate_limit, enable_memory等
--
-- 2. ai_api: AI接口配置
--    - 主要字段：provider, api_key, api_base, model
--    - 特殊字段：include_articles
--
-- 3. article_ai: 文章AI助手配置（包含翻译、智能摘要等文章相关AI功能）
--    - 主要字段：translation_type, default_source_lang, default_target_lang
--    - JSON字段：baidu_config, custom_config, llm_config, translation_llm_config, summary_config
--    - translation_type决定翻译功能使用哪个实现方式：
--      * none: 不翻译（默认值，不生成translation表记录）
--      * baidu: 百度翻译API
--      * custom: 自定义翻译API
--      * llm: 使用全局AI模型（llm_config）
--      * dedicated_llm: 使用翻译独立AI模型（translation_llm_config）
--    - summary_config.summaryMode决定摘要生成方式（global/dedicated/textrank）
--
-- 安全注意事项：
-- - api_key 和 mem0_api_key 字段在应用层需要加密存储
-- - 查询时需要在应用层解密后使用
-- - 前端展示时需要脱敏处理（仅显示前4位和后4位）
--
-- 扩展性说明：
-- - extra_config 字段用于存储未来可能新增的配置项
-- - JSON字段支持灵活的嵌套结构
-- - 可通过 config_name 支持同类型多套配置
--
-- 语言映射说明：
-- - 语言代码到自然语言的映射直接定义在Java代码中（SysAiConfigService）
-- - 提供两套映射：前台展示用（原生语言）、后台管理用（中文）
-- - 前后端通过API接口统一获取，避免重复定义


INSERT INTO `poetize`.`user`(`id`, `username`, `password`, `phone_number`, `email`, `user_status`, `gender`, `open_id`, `admire`, `subscribe`, `avatar`, `introduction`, `user_type`, `update_by`, `deleted`) VALUES (1, 'Sara', '$2a$12$hQ2N3HlDqxdVrsJ4SKGY1.D6F/I1/dUBFVkckaq1DnJH693mPZS5S', '', '', 1, 1, '', '', '', '', '', 0, 'Sara', 0);

INSERT INTO `poetize`.`web_info`(`id`, `web_name`, `web_title`, `notices`, `footer`, `background_image`, `avatar`, `random_avatar`, `random_name`, `random_cover`, `waifu_json`, `status`, `api_enabled`, `api_key`, `nav_config`, `minimal_footer`, `mobile_drawer_config`) VALUES (1, 'Sara', 'POETIZE', '[]', '云想衣裳花想容， 春风拂槛露华浓。', '', '', '[]', '[]', '["/static/assets/backgroundPicture.jpg"]', '{
    "waifuPath": "/static/live2d-widget/waifu-tips.json",
    "cdnPath": "https://fastly.jsdelivr.net/gh/fghrsh/live2d_api/",
    "tools": ["hitokoto", "asteroids", "switch-model", "switch-texture", "photo", "info", "quit"]
}', 1, 0, NULL, '[{"name":"首页","icon":"🏡","link":"/","type":"internal","order":1,"enabled":true},{"name":"分类","icon":"📑","link":"#","type":"dropdown","order":2,"enabled":true},{"name":"家","icon":"❤️‍🔥","link":"/love","type":"internal","order":3,"enabled":true},{"name":"友人帐","icon":"🤝","link":"/friends","type":"internal","order":4,"enabled":true},{"name":"曲乐","icon":"🎵","link":"/music","type":"internal","order":5,"enabled":true},{"name":"收藏夹","icon":"📁","link":"/favorites","type":"internal","order":6,"enabled":true},{"name":"留言","icon":"📪","link":"/message","type":"internal","order":7,"enabled":true},{"name":"联系我","icon":"💬","link":"#chat","type":"special","order":8,"enabled":true}]', 0, '{"titleType":"text","titleText":"欢迎光临","avatarSize":100,"backgroundType":"image","backgroundImage":"/assets/toolbar.jpg","backgroundColor":"#000000","backgroundGradient":"linear-gradient(60deg, #ffd7e4, #c8f1ff 95%)","maskOpacity":0.7,"menuFontColor":"#ffffff","showBorder":true,"borderColor":"rgba(255, 255, 255, 0.15)","showSnowflake":true}');

INSERT INTO `poetize`.`family` (`id`, `user_id`, `bg_cover`, `man_cover`, `woman_cover`, `man_name`, `woman_name`, `timing`, `countdown_title`, `countdown_time`, `status`, `family_info`, `like_count`, `create_time`, `update_time`) VALUES (1, 1, '背景封面', '男生头像', '女生头像', 'Sara', 'Abby', '2000-01-01 00:00:00', '春节倒计时', '2025-01-29 00:00:00', 1, '', 0, '2000-01-01 00:00:00', '2000-01-01 00:00:00');

INSERT INTO `poetize`.`im_chat_group` (`id`, `group_name`, `master_user_id`, `introduction`, `notice`, `in_type`) VALUES(-1, '公共聊天室', 1, '公共聊天室', '欢迎光临！', 0);

insert into `poetize`.`im_chat_group_user` (`id`, `group_id`, `user_id`, `admin_flag`, `user_status`) values(1, -1, 1, 1, 1);

INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (1, '邮箱验证码模板', 'user.code.format', '【POETIZE】%s为本次验证的验证码，请在5分钟内完成验证。为保证账号安全，请勿泄漏此验证码。', '1');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (2, '邮箱订阅模板', 'user.subscribe.format', '【POETIZE】您订阅的专栏【%s】新增一篇文章：%s。', '1');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (3, '默认存储平台（local:本地，qiniu:七牛云，lsky:兰空图床，easyimage:简单图床）', 'store.type', 'local', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (4, '本地存储启用状态', 'local.enable', 'true', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (5, '本地存储上传文件根目录', 'local.uploadUrl', '/app/static/', '1');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (6, '本地存储下载前缀', 'local.downloadUrl', '/static/', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (7, '七牛云存储启用状态', 'qiniu.enable', 'false', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (8, '七牛云-accessKey', 'qiniu.accessKey', '', '1');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (9, '七牛云-secretKey', 'qiniu.secretKey', '', '1');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (10, '七牛云-bucket', 'qiniu.bucket', '', '1');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (11, '七牛云-域名', 'qiniu.downloadUrl', '仿照：【https://file.poetize.cn/】，将域名换成自己的七牛云ip或域名', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (12, '七牛云上传地址', 'qiniuUrl', 'https://upload.qiniup.com', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (13, '兰空图床存储启用状态', 'lsky.enable', 'false', '1');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (14, '兰空图床-API地址', 'lsky.url', 'http://your-lsky-instance.com/api/v1', '1');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (15, '兰空图床-Token', 'lsky.token', '', '1');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (16, '兰空图床-存储策略ID', 'lsky.strategy_id', '', '1');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (17, '简单图床启用状态', 'easyimage.enable', 'false', '1');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (18, '简单图床-API地址', 'easyimage.url', 'https://your-easyimage-instance.com/api/upload', '1');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (19, '简单图床-Token', 'easyimage.token', '', '1');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (20, 'IM-聊天室启用状态', 'im.enable', 'true', '1');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (21, '备案号', 'beian', '', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (22, '公安备案号', 'policeBeian', '', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (23, '前端静态资源路径前缀', 'webStaticResourcePrefix', '/static/', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (24, 'WebP图片转换启用状态', 'image.webp.enabled', 'true', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (25, 'WebP转换最小文件大小(KB)', 'image.webp.min-size', '50', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (26, 'WebP转换最小节省比例(%)', 'image.webp.min-saving-ratio', '10', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (27, '图片压缩模式(lossy:有损,lossless:无损)', 'image.compress.mode', 'lossy', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (28, '图片压缩启用状态', 'image.compress.enabled', 'true', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (29, '字体文件CDN基础路径(末尾必须有/)', 'font.cdn.base-url', '/static/assets/font_chunks/', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (30, '是否使用单一字体文件', 'font.use.single', 'false', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (31, '单一字体文件名称', 'font.single.filename', 'font.woff2', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (32, '是否从远程加载字体Unicode范围', 'font.unicode.remote', 'true', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (33, '字体Unicode范围JSON文件路径', 'font.unicode.path', '/static/assets/font_chunks/unicode_ranges.json', '2');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (34, '腾讯位置服务Key', 'tencent.lbs.key', '', '1');
INSERT INTO `poetize`.`sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`) VALUES (35, '全局评论开关', 'enableComment', 'true', '2');

-- 插入默认的看板娘模型插件（使用 INSERT IGNORE 避免重复插入）
INSERT IGNORE INTO `sys_plugin` (`plugin_type`, `plugin_key`, `plugin_name`, `plugin_description`, `plugin_config`, `enabled`, `is_system`, `sort_order`) VALUES
('waifu_model', 'pio', 'Pio酱', '来自 Potion Maker 的可爱女孩',
'{"modelPath": "Potion-Maker/Pio", "textures": ["Potion-Maker/Pio"], "scale": 1.0, "thumbnailUrl": "/static/waifu_previews/pio.png", "messages": {"greeting": ["你好呀~", "欢迎光临！"], "idle": ["无聊了...", "点点我嘛~"]}}',
1, 1, 0),
('waifu_model', 'tia', 'Tia酱', '来自 Potion Maker 的元气少女',
'{"modelPath": "Potion-Maker/Tia", "textures": ["Potion-Maker/Tia"], "scale": 1.0, "thumbnailUrl": "/static/waifu_previews/tia.png", "messages": {"greeting": ["嗨！", "今天也要加油哦！"], "idle": ["在想什么呢？", "陪我聊聊天吧~"]}}',
1, 1, 1),
('waifu_model', 'bilibili_22', 'Bilibili 22娘', '来自 Bilibili Live 的22号看板娘',
'{"modelPath": "bilibili-live/22", "textures": ["bilibili-live/22"], "scale": 1.0, "thumbnailUrl": "/static/waifu_previews/bilibili_22.png", "messages": {"greeting": ["22娘来啦~", "bilibili干杯！"], "idle": ["去看看番剧吧~", "今天投币了吗？"]}}',
1, 1, 2),
('waifu_model', 'bilibili_33', 'Bilibili 33娘', '来自 Bilibili Live 的33号看板娘',
'{"modelPath": "bilibili-live/33", "textures": ["bilibili-live/33"], "scale": 1.0, "thumbnailUrl": "/static/waifu_previews/bilibili_33.png", "messages": {"greeting": ["33娘驾到！", "素质三连走起！"], "idle": ["一键三连哦~", "关注一下呗？"]}}',
1, 1, 3),
('waifu_model', 'shizuku', 'Shizuku', 'Shizuku Talk 系列角色',
'{"modelPath": "ShizukuTalk/shizuku-48", "textures": ["ShizukuTalk/shizuku-48", "ShizukuTalk/shizuku-pajama"], "scale": 1.0, "thumbnailUrl": "/static/waifu_previews/shizuku.png", "messages": {"greeting": ["这里是Shizuku~", "有什么我能帮你的吗？"], "idle": ["换件衣服看看？", "今天天气真好~"]}}',
1, 1, 4),
('waifu_model', 'neptune', '海王星系列', '超次元游戏海王星角色合集',
'{"modelPath": "HyperdimensionNeptunia/neptune_classic", "textures": ["HyperdimensionNeptunia/neptune_classic", "HyperdimensionNeptunia/nepnep", "HyperdimensionNeptunia/neptune_santa", "HyperdimensionNeptunia/nepmaid", "HyperdimensionNeptunia/nepswim"], "scale": 1.0, "thumbnailUrl": "/static/waifu_previews/neptune.png", "messages": {"greeting": ["Nep! Nep!", "Neptune参上！"], "idle": ["我是主角哦~", "来玩游戏吧！"]}}',
1, 1, 5),
('waifu_model', 'murakumo', '叢雲', '舰队Collection - 叢雲',
'{"modelPath": "KantaiCollection/murakumo", "textures": ["KantaiCollection/murakumo"], "scale": 1.0, "thumbnailUrl": "/static/waifu_previews/murakumo.png", "messages": {"greeting": ["叢雲です", "あなたが提督？"], "idle": ["別に...", "何か用？"]}}',
1, 1, 6);

-- 设置默认激活的看板娘模型
INSERT IGNORE INTO `sys_plugin_active` (`plugin_type`, `plugin_key`) VALUES
('waifu_model', 'neptune');

-- 初始化插件数据
INSERT INTO `poetize`.`sys_plugin` (`plugin_type`, `plugin_key`, `plugin_name`, `plugin_description`, `plugin_config`, `plugin_code`, `enabled`, `is_system`, `sort_order`) VALUES
('mouse_click_effect', 'none', '无效果', '关闭鼠标点击效果', '{}', NULL, 1, 1, 0),
('mouse_click_effect', 'text', '社会主义核心价值观', '点击时显示社会主义核心价值观文字：富强、民主、文明、和谐等', 
'{"texts": ["富强", "民主", "文明", "和谐", "自由", "平等", "公正", "法治", "爱国", "敬业", "诚信", "友善"], "color": "#ff6651", "fontSize": 16, "duration": 1500, "moveDistance": 160}',
'const list = config.texts || [
  "富强", "民主", "文明", "和谐",
  "自由", "平等", "公正", "法治",
  "爱国", "敬业", "诚信", "友善"
];

if (typeof window._textEffectIdx === "undefined") {
  window._textEffectIdx = 0;
}

const span = document.createElement("span");
span.textContent = list[window._textEffectIdx];
window._textEffectIdx = (window._textEffectIdx + 1) % list.length;

Object.assign(span.style, {
  "z-index": "1000",
  top: y - 20 + "px",
  left: x + "px",
  position: "absolute",
  "pointer-events": "none",
  "font-weight": "bold",
  color: config.color || "#ff6651",
  transition: "all 1.5s ease-out"
});

if (document.body && span && span.nodeType === Node.ELEMENT_NODE) {
  document.body.appendChild(span);
} else {
  return;
}

setTimeout(() => {
  span.style.top = y - 180 + "px";
  span.style.opacity = "0";
}, 10);

setTimeout(() => {
  if (span.parentNode) {
    span.parentNode.removeChild(span);
  }
}, 1500);', 1, 1, 1),
('mouse_click_effect', 'firework', '烟花粒子', '点击时产生彩色烟花粒子扩散效果',
'{"colors": ["#FF1461", "#18FF92", "#5A87FF", "#FBF38C"], "particleCount": 30, "minRadius": 16, "maxRadius": 32, "minDistance": 50, "maxDistance": 180}',
'if (!anime) { console.warn("anime.js未加载"); return; }

const colors = config.colors || ["#FF1461", "#18FF92", "#5A87FF", "#FBF38C"];
const numberOfParticules = config.particleCount || 30;

// 将页面坐标转换为视口坐标（因为canvas使用position:fixed）
const viewportX = x - window.scrollX;
const viewportY = y - window.scrollY;

// 获取或创建canvas
let canvas = document.getElementById("mousedown-effect");
if (!canvas) {
  canvas = document.createElement("canvas");
  canvas.id = "mousedown-effect";
  Object.assign(canvas.style, {
    position: "fixed",
    left: "0",
    top: "0",
    pointerEvents: "none",
    zIndex: "1000"
  });
  document.body.appendChild(canvas);
}

// 设置canvas尺寸
canvas.width = 2 * window.innerWidth;
canvas.height = 2 * window.innerHeight;
canvas.style.width = window.innerWidth + "px";
canvas.style.height = window.innerHeight + "px";

const ctx = canvas.getContext("2d", {willReadFrequently: true});
ctx.scale(2, 2);

// 粒子方向
function setParticuleDirection(p) {
  const t = anime.random(0, 360) * Math.PI / 180;
  const a = anime.random(50, 180);
  const n = [-1, 1][anime.random(0, 1)] * a;
  return {
    x: p.x + n * Math.cos(t),
    y: p.y + n * Math.sin(t)
  };
}

// 创建粒子
function createParticule(px, py) {
  const p = {
    x: px,
    y: py,
    color: colors[anime.random(0, colors.length - 1)],
    radius: anime.random(16, 32)
  };
  p.endPos = setParticuleDirection(p);
  p.draw = function() {
    ctx.beginPath();
    ctx.arc(p.x, p.y, p.radius, 0, 2 * Math.PI, true);
    ctx.fillStyle = p.color;
    ctx.fill();
  };
  return p;
}

// 创建圆环
function createCircle(px, py) {
  const c = {
    x: px,
    y: py,
    color: "#F00",
    radius: 0.1,
    alpha: 0.5,
    lineWidth: 6
  };
  c.draw = function() {
    ctx.globalAlpha = c.alpha;
    ctx.beginPath();
    ctx.arc(c.x, c.y, c.radius, 0, 2 * Math.PI, true);
    ctx.lineWidth = c.lineWidth;
    ctx.strokeStyle = c.color;
    ctx.stroke();
    ctx.globalAlpha = 1;
  };
  return c;
}

// 创建粒子和圆环（使用视口坐标）
const circle = createCircle(viewportX, viewportY);
const particules = [];
for (let i = 0; i < numberOfParticules; i++) {
  particules.push(createParticule(viewportX, viewportY));
}

// 所有动画目标
const allTargets = [...particules, circle];

// 渲染函数 - 绘制所有元素
function renderAll() {
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  for (let i = 0; i < allTargets.length; i++) {
    allTargets[i].draw();
  }
}

// 启动动画
anime.timeline().add({
  targets: particules,
  x: function(p) { return p.endPos.x; },
  y: function(p) { return p.endPos.y; },
  radius: 0.1,
  duration: anime.random(1200, 1800),
  easing: "easeOutExpo",
  update: renderAll
}).add({
  targets: circle,
  radius: anime.random(80, 160),
  lineWidth: 0,
  alpha: {
    value: 0,
    easing: "linear",
    duration: anime.random(600, 800)
  },
  duration: anime.random(1200, 1800),
  easing: "easeOutExpo",
  offset: 0
});', 1, 1, 2);

-- 初始化插件激活状态
INSERT INTO `poetize`.`sys_plugin_active` (`plugin_type`, `plugin_key`) VALUES
('mouse_click_effect', 'none');

-- 初始化验证码配置数据
INSERT INTO `poetize`.`sys_captcha_config` (`id`, `enable`, `login`, `register`, `comment`, `reset_password`, `screen_size_threshold`, `force_slide_for_mobile`, `slide_accuracy`, `slide_success_threshold`, `checkbox_track_sensitivity`, `checkbox_min_track_points`, `checkbox_reply_sensitivity`, `checkbox_max_retry_count`, `checkbox_retry_decrement`) VALUES (1, 1, 1, 1, 1, 1, 768, 1, 5, 0.95, 0.99, 3, 0.85, 5, 0.02);

-- 初始化第三方OAuth登录配置数据
INSERT INTO `poetize`.`third_party_oauth_config` (`platform_type`, `platform_name`, `scope`, `enabled`, `global_enabled`, `sort_order`, `remark`, `deleted`) VALUES
('github', 'GitHub', 'user:email', 0, 0, 1, 'GitHub OAuth登录配置，需要在GitHub开发者设置中创建OAuth应用', 0),
('google', 'Google', 'openid email profile', 0, 0, 2, 'Google OAuth登录配置，需要在Google Cloud Console中创建OAuth客户端', 0),
('twitter', 'Twitter', 'tweet.read users.read', 0, 0, 3, 'Twitter OAuth登录配置，需要在Twitter Developer Portal中创建应用', 0),
('yandex', 'Yandex', 'login:email login:info', 0, 0, 4, 'Yandex OAuth登录配置，需要在Yandex OAuth中创建应用', 0),
('gitee', 'Gitee', 'user_info emails', 0, 0, 5, 'Gitee OAuth登录配置，需要在Gitee第三方应用中创建应用', 0),
('qq', 'QQ', 'get_user_info', 0, 0, 6, 'QQ OAuth登录配置，需要在QQ互联平台中创建应用', 0),
('baidu', 'Baidu', 'basic', 0, 0, 7, 'Baidu OAuth登录配置，需要在Baidu开发者平台中创建应用', 0);

INSERT INTO `poetize`.`resource_path` (`title`, `cover`, `introduction`, `type`, `status`) VALUES ('POETIZE', 'https://s1.ax1x.com/2022/11/10/z9VlHs.png', '这是一个 Vue2 Vue3 与 SpringBoot 结合的产物～', 'siteInfo', 1);

-- ========== 导入静态资源到resource表 ==========
-- 将public/assets目录下的静态文件录入到数据库，使其在后台资源管理中可见

-- 图片资源
INSERT INTO `poetize`.`resource` (`user_id`, `type`, `path`, `size`, `original_name`, `mime_type`, `status`, `store_type`, `create_time`) VALUES 
(1, 'assets', '/static/assets/admireImage.jpg', 8192, 'admireImage.jpg', 'image/jpeg', 1, 'local', NOW()),
(1, 'assets', '/static/assets/backgroundPicture.jpg', 915456, 'backgroundPicture.jpg', 'image/jpeg', 1, 'local', NOW()),
(1, 'assets', '/static/assets/cloud.png', 67227, 'cloud.png', 'image/png', 1, 'local', NOW()),
(1, 'assets', '/static/assets/love.jpg', 222208, 'love.jpg', 'image/jpeg', 1, 'local', NOW()),
(1, 'assets', '/static/assets/loveMessage.jpg', 112640, 'loveMessage.jpg', 'image/jpeg', 1, 'local', NOW()),
(1, 'assets', '/static/assets/lovePhoto.jpg', 99328, 'lovePhoto.jpg', 'image/jpeg', 1, 'local', NOW()),
(1, 'assets', '/static/assets/loveWeiYan.jpg', 109568, 'loveWeiYan.jpg', 'image/jpeg', 1, 'local', NOW()),
(1, 'assets', '/static/assets/friendLetterMiddle.jpg', 116736, 'friendLetterMiddle.jpg', 'image/jpeg', 1, 'local', NOW()),
(1, 'assets', '/static/assets/toolbar.jpg', 292864, 'toolbar.jpg', 'image/jpeg', 1, 'local', NOW()),
(1, 'assets', '/static/assets/bannerWave1.png', 5120, 'bannerWave1.png', 'image/png', 1, 'local', NOW()),
(1, 'assets', '/static/assets/bannerWave2.png', 4915, 'bannerWave2.png', 'image/png', 1, 'local', NOW()),
(1, 'assets', '/static/assets/commentURL.png', 68234, 'commentURL.png', 'image/png', 1, 'local', NOW()),
(1, 'assets', '/static/assets/friendLetterBiLi.png', 13312, 'friendLetterBiLi.png', 'image/png', 1, 'local', NOW()),
(1, 'assets', '/static/assets/friendLetterBottom.png', 158720, 'friendLetterBottom.png', 'image/png', 1, 'local', NOW()),
(1, 'assets', '/static/assets/friendLetterTop.png', 63488, 'friendLetterTop.png', 'image/png', 1, 'local', NOW()),
(1, 'assets', '/static/assets/springBg.png', 122880, 'springBg.png', 'image/png', 1, 'local', NOW()),
(1, 'assets', '/static/assets/toTop.png', 8192, 'toTop.png', 'image/png', 1, 'local', NOW());

-- SVG文件
INSERT INTO `poetize`.`resource` (`user_id`, `type`, `path`, `size`, `original_name`, `mime_type`, `status`, `store_type`, `create_time`) VALUES 
(1, 'assets', '/static/assets/loveLike.svg', 8601, 'loveLike.svg', 'image/svg+xml', 1, 'local', NOW());

-- 视频文件
INSERT INTO `poetize`.`resource` (`user_id`, `type`, `path`, `size`, `original_name`, `mime_type`, `status`, `store_type`, `create_time`) VALUES 
(1, 'assets', '/static/assets/backgroundVideo.mp4', 1955207, 'backgroundVideo.mp4', 'video/mp4', 1, 'local', NOW());

-- 字体文件
INSERT INTO `poetize`.`resource` (`user_id`, `type`, `path`, `size`, `original_name`, `mime_type`, `status`, `store_type`, `create_time`) VALUES 
(1, 'assets', '/static/assets/font_chunks/font.base.woff2', 8956, 'font.base.woff2', 'font/woff2', 1, 'local', NOW()),
(1, 'assets', '/static/assets/font_chunks/font.level1.woff2', 830860, 'font.level1.woff2', 'font/woff2', 1, 'local', NOW()),
(1, 'assets', '/static/assets/font_chunks/font.level2.woff2', 756328, 'font.level2.woff2', 'font/woff2', 1, 'local', NOW()),
(1, 'assets', '/static/assets/font_chunks/font.other.woff2', 198920, 'font.other.woff2', 'font/woff2', 1, 'local', NOW());

-- 优化 `article` 表
-- 为用户ID添加索引，加速查询某个用户的所有文章
ALTER TABLE `poetize`.`article` ADD INDEX `idx_user_id` (`user_id`);
-- 为分类和标签创建复合索引，加速按分类和标签筛选文章
ALTER TABLE `poetize`.`article` ADD INDEX `idx_sort_label` (`sort_id`, `label_id`);
-- 为推荐状态添加索引，加速查询推荐文章
ALTER TABLE `poetize`.`article` ADD INDEX `idx_recommend_status` (`recommend_status`);
-- 为可见状态添加索引，确保查询时能快速过滤不可见文章
ALTER TABLE `poetize`.`article` ADD INDEX `idx_view_status` (`view_status`);

-- 优化 `comment` 表
-- 为用户ID添加索引，加速查询某个用户的所有评论
ALTER TABLE `poetize`.`comment` ADD INDEX `idx_user_id` (`user_id`);
-- 为父评论ID添加索引，加速构建评论楼层
ALTER TABLE `poetize`.`comment` ADD INDEX `idx_parent_comment_id` (`parent_comment_id`);
-- 优化已有的 `source` 索引，改为复合索引，提高查询特定来源评论的效率
ALTER TABLE `poetize`.`comment` DROP INDEX `source`;
ALTER TABLE `poetize`.`comment` ADD INDEX `idx_source_type` (`source`, `type`);

-- 优化 `label` 表
-- 为分类ID添加索引，加速查询某个分类下的所有标签
ALTER TABLE `poetize`.`label` ADD INDEX `idx_sort_id` (`sort_id`);

-- 优化 `im_chat_user_friend` 表
-- 添加唯一复合索引，防止重复的好友关系，并加速双向查询
ALTER TABLE `poetize`.`im_chat_user_friend` ADD UNIQUE INDEX `uk_user_friend` (`user_id`, `friend_id`);
-- 为好友ID单独添加索引，加速反向查询（例如查询"谁加了我为好友"）
ALTER TABLE `poetize`.`im_chat_user_friend` ADD INDEX `idx_friend_id` (`friend_id`);

-- 优化 `im_chat_group_user` 表
-- 添加唯一复合索引，防止用户重复加入同一个群
ALTER TABLE `poetize`.`im_chat_group_user` ADD UNIQUE INDEX `uk_group_user` (`group_id`, `user_id`);
-- 为用户ID单独添加索引，加速查询某个用户加入了哪些群
ALTER TABLE `poetize`.`im_chat_group_user` ADD INDEX `idx_user_id` (`user_id`);

-- 优化 `wei_yan` 表
-- 为 `source` 和 `type` 添加复合索引，加速查询特定来源的微言
ALTER TABLE `poetize`.`wei_yan` ADD INDEX `idx_source_type` (`source`, `type`);
-- 为 `is_public` 和 `create_time` 添加复合索引，加速查询公开的、并按时间排序的微言
ALTER TABLE `poetize`.`wei_yan` ADD INDEX `idx_public_create` (`is_public`, `create_time`);

-- 优化 `resource` 表
-- 为 `user_id` 和 `type` 添加复合索引，加速查询某个用户的特定类型资源
ALTER TABLE `poetize`.`resource` ADD INDEX `idx_user_type` (`user_id`, `type`);