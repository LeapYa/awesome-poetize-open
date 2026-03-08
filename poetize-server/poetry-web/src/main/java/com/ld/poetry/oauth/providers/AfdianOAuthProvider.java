package com.ld.poetry.oauth.providers;

import com.ld.poetry.oauth.base.OAuth2Provider;
import com.ld.poetry.oauth.exception.TokenException;
import com.ld.poetry.oauth.exception.UserInfoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

/**
 * 爱发电 OAuth 2.0 提供商
 * <p>
 * 爱发电使用标准 authorization_code 模式。
 * 特殊之处：access_token 响应中直接返回 user_id，没有独立的 userInfo 接口。
 * </p>
 * <p>
 * 授权 URL: https://afdian.com/oauth2/authorize
 * Token URL: https://afdian.com/api/oauth2/access_token
 * </p>
 *
 * @author LeapYa
 * @since 2026-02-18
 */
@Slf4j
@Component("afdianOAuthProvider")
public class AfdianOAuthProvider extends OAuth2Provider {

    private static final String AUTH_URL = "https://afdian.com/oauth2/authorize";
    private static final String TOKEN_URL = "https://afdian.com/api/oauth2/access_token";

    /**
     * 临时存储 token 响应中的 user_id，供 getUserInfo 使用
     */
    private final ThreadLocal<Map<String, Object>> tokenResponseHolder = new ThreadLocal<>();

    @Override
    public String getProviderName() {
        return "afdian";
    }

    @Override
    protected String getAuthorizationUrl() {
        return AUTH_URL;
    }

    @Override
    protected String getTokenUrl() {
        return TOKEN_URL;
    }

    @Override
    protected String getUserInfoUrl() {
        // 爱发电没有独立的 userInfo 接口，user_id 在 token 响应中返回
        return null;
    }

    @Override
    protected String getScope() {
        return "basic";
    }

    @Override
    public Map<String, Object> getAccessToken(String code) {
        String tokenUrl = getTokenUrl();

        try {
            // 爱发电使用 POST 表单提交
            org.springframework.util.LinkedMultiValueMap<String, String> params = new org.springframework.util.LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", config.getClientId());
            params.add("client_secret", config.getClientSecret());
            params.add("code", code);
            params.add("redirect_uri", config.getRedirectUri());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.add("Accept", "application/json");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            log.debug("获取爱发电访问令牌: tokenUrl={}", tokenUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fullResponse = objectMapper.readValue(response.getBody(), Map.class);

                // 爱发电返回格式: { "ec": 200, "data": { "user_id": "xxx", "access_token": "xxx" } }
                Integer ec = (Integer) fullResponse.get("ec");
                if (ec == null || ec != 200) {
                    String em = (String) fullResponse.get("em");
                    log.warn("爱发电获取token失败: ec={}, em={}", ec, em);
                    throw new TokenException("爱发电授权失败: " + (em != null ? em : "未知错误"), getProviderName());
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) fullResponse.get("data");
                if (data == null) {
                    throw new TokenException("爱发电返回数据为空", getProviderName());
                }

                String accessToken = (String) data.get("access_token");
                if (accessToken == null || accessToken.isBlank()) {
                    throw new TokenException("爱发电未返回 access_token", getProviderName());
                }

                // 保存完整 token 响应（含 user_id），供 getUserInfo 使用
                tokenResponseHolder.set(data);

                // 返回标准格式
                Map<String, Object> result = new HashMap<>();
                result.put("access_token", accessToken);
                if (data.containsKey("user_id")) {
                    result.put("user_id", data.get("user_id"));
                }
                return result;
            } else {
                throw new TokenException("爱发电请求失败: HTTP " + response.getStatusCode(), getProviderName());
            }
        } catch (TokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取爱发电访问令牌失败", e);
            throw new TokenException("网络请求失败", getProviderName(), e);
        }
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken) {
        try {
            // 爱发电没有独立的 userInfo 接口
            // user_id 在 getAccessToken 阶段已通过 tokenResponseHolder 保存
            Map<String, Object> tokenData = tokenResponseHolder.get();

            String userId = null;
            if (tokenData != null) {
                userId = (String) tokenData.get("user_id");
            }

            if (userId == null || userId.isBlank()) {
                throw new UserInfoException("无法获取爱发电用户ID", "afdian");
            }

            // 返回标准化用户信息
            Map<String, Object> result = new HashMap<>();
            result.put("provider", "afdian");
            result.put("uid", userId);
            result.put("username", "afdian_" + userId.substring(0, Math.min(8, userId.length())));
            result.put("email", "");
            result.put("avatar", "");
            result.put("email_collection_needed", true);

            return result;
        } catch (UserInfoException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取爱发电用户信息失败", e);
            throw new UserInfoException("获取用户信息失败: " + e.getMessage(), "afdian", e);
        } finally {
            // 清理 ThreadLocal 防止内存泄漏
            tokenResponseHolder.remove();
        }
    }
}
