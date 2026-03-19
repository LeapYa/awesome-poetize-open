-- 为 resource 表添加图片宽高字段，用于前台渲染时预留占位避免布局偏移
ALTER TABLE `resource`
    ADD COLUMN IF NOT EXISTS `width` INT DEFAULT NULL COMMENT '图片宽度（像素）',
    ADD COLUMN IF NOT EXISTS `height` INT DEFAULT NULL COMMENT '图片高度（像素）';
