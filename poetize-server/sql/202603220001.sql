ALTER TABLE `sys_ai_config`
ADD COLUMN IF NOT EXISTS `show_timestamp` tinyint(1) DEFAULT 1 COMMENT '显示时间戳 (0:否 1:是)' AFTER `enable_typing_indicator`;

UPDATE `sys_ai_config`
SET `show_timestamp` = 1
WHERE `config_type` = 'ai_chat'
  AND `show_timestamp` IS NULL;
