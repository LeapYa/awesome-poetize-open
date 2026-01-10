package com.ld.poetry.oauth.base;

import java.util.List;
import java.util.Map;

/**
 * OAuth提供商基础接口
 * 定义所有OAuth提供商必须实现的方法
 *
 * @author LeapYa
 * @since 2026-01-10
 */
public interface BaseOAuthProvider {

    /**
     * 获取提供商名称
     * @return 提供商名称（如github, google, twitter等）
     */
    String getProviderName();

    /**
     * 生成授权URL
     * @param state CSRF防护状态token
     * @return 授权URL
     */
    String getAuthUrl(String state);

    /**
     * 通过授权码获取访问令牌
     * @param code 授权码
     * @return 包含access_token的Map
     */
    Map<String, Object> getAccessToken(String code);

    /**
     * 通过访问令牌获取用户信息
     * @param accessToken 访问令牌
     * @return 标准化的用户信息
     */
    Map<String, Object> getUserInfo(String accessToken);

    /**
     * 验证配置是否完整
     * @return 配置是否有效
     */
    boolean validateConfig();

    /**
     * 获取必需的配置字段列表
     * @return 必需的配置字段名称列表
     */
    List<String> getRequiredConfigFields();

    /**
     * 检查是否需要前端收集邮箱
     * @param email 从OAuth API获取的邮箱地址
     * @return [处理后的邮箱, 是否需要收集邮箱] 的数组
     */
    default Object[] checkEmailCollectionNeeded(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new Object[]{"", true};
        }
        return new Object[]{email.trim(), false};
    }
}
