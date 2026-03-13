-- 为 web_info 表添加 API IP 白名单字段
ALTER TABLE `web_info`
ADD COLUMN IF NOT EXISTS `api_ip_whitelist` TEXT DEFAULT NULL COMMENT 'API IP白名单，支持IP和CIDR，逗号或换行分隔' AFTER `api_key`;
