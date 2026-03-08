-- Hello World 插件安装 SQL
-- 注意：插件只允许操作 plugin_ 前缀的表（由 SqlSafetyValidator 强制检查）
CREATE TABLE IF NOT EXISTS plugin_hello_world_log (
    id          INT          NOT NULL AUTO_INCREMENT,
    event_type  VARCHAR(50)  NOT NULL COMMENT '事件类型：article_save / comment_publish / user_register',
    target_id   BIGINT       DEFAULT NULL COMMENT '关联对象ID',
    note        VARCHAR(255) DEFAULT NULL COMMENT '备注信息',
    created_at  DATETIME     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Hello World 插件事件日志';
