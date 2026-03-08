-- 爱发电 OAuth 登录配置
INSERT INTO `poetize`.`third_party_oauth_config` 
(
  `platform_type`, 
  `platform_name`, 
  `client_id`, 
  `client_secret`, 
  `redirect_uri`, 
  `scope`, 
  `enabled`, 
  `global_enabled`, 
  `sort_order`, 
  `remark`
) 
VALUES 
(
  'afdian',
  '爱发电',
  '',
  '',
  '',
  'basic',
  0,
  0,
  8,
  '爱发电 OAuth 登录配置，需要在 afdian.com 开发者设置中获取 client_id 和 client_secret'
)
ON DUPLICATE KEY UPDATE 
  `platform_name` = '爱发电',
  `scope` = 'basic',
  `sort_order` = 8,
  `remark` = '爱发电 OAuth 登录配置，需要在 afdian.com 开发者设置中获取 client_id 和 client_secret';
