-- 清理已下线的搜索引擎推送配置，保留站点验证配置
DELETE FROM `poetize`.`seo_search_engine_push`
WHERE `engine_name` IN ('google', 'yahoo', 'so');
