package com.ld.poetry.oauth.providers;

import com.ld.poetry.oauth.base.OAuth2Provider;
import com.ld.poetry.oauth.exception.TokenException;
import com.ld.poetry.oauth.exception.UserInfoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Google OAuth 2.0 提供商
 *
 * @author LeapYa
 * @since 2026-01-10
 */
@Slf4j
@Component("googleOAuthProvider")
public class GoogleOAuthProvider extends OAuth2Provider {

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    // 存储ID Token用于用户信息验证
    private ThreadLocal<String> idTokenHolder = new ThreadLocal<>();

    @Override
    public String getProviderName() {
        return "google";
    }

    @Override
    protected String getAuthorizationUrl() {
        return AUTH_URL;
    }

    @Override
    protected String getTokenUrl() {
        return getProxiedUrl(TOKEN_URL, "/google/oauth2/token");
    }

    @Override
    protected String getUserInfoUrl() {
        return getProxiedUrl(USER_INFO_URL, "/google/oauth2/v2/userinfo");
    }

    @Override
    protected String getScope() {
        return "openid email profile";
    }

    @Override
    protected void addAuthUrlParams(UriComponentsBuilder builder) {
        builder.queryParam("access_type", "offline");  // 获取refresh_token
    }

    @Override
    public Map<String, Object> getAccessToken(String code) {
        Map<String, Object> tokenData = super.getAccessToken(code);

        // 保存ID Token用于后续验证
        String idToken = (String) tokenData.get("id_token");
        if (idToken != null) {
            idTokenHolder.set(idToken);
        }

        return tokenData;
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken) {
        try {
            String idToken = idTokenHolder.get();
            Map<String, Object> idInfo = null;

            // 尝试解析ID Token
            if (idToken != null) {
                try {
                    idInfo = parseIdToken(idToken);
                } catch (Exception e) {
                    log.warn("解析Google ID Token失败，将使用userinfo API", e);
                }
            }

            // 获取用户信息
            Map<String, Object> userInfo = sendAuthenticatedGetRequest(getUserInfoUrl(), accessToken);

            // 优先使用ID Token中的信息
            String uid = idInfo != null ? (String) idInfo.get("sub") : (String) userInfo.get("id");
            String email = idInfo != null ? (String) idInfo.get("email") : (String) userInfo.get("email");
            String name = userInfo.get("name") != null ? (String) userInfo.get("name") : "";
            String picture = userInfo.get("picture") != null ? (String) userInfo.get("picture") : "";

            // 验证客户端ID（如果有ID Token）
            if (idInfo != null) {
                String aud = (String) idInfo.get("aud");
                if (aud != null && !aud.equals(config.getClientId())) {
                    log.warn("Google ID Token audience不匹配");
                }
            }

            // 检查邮箱收集需求
            Object[] emailCheck = checkEmailCollectionNeeded(email);
            String processedEmail = (String) emailCheck[0];
            boolean emailCollectionNeeded = (Boolean) emailCheck[1];

            // 返回标准化用户信息
            Map<String, Object> result = new HashMap<>();
            result.put("provider", "google");
            result.put("uid", uid);
            result.put("username", name);
            result.put("email", processedEmail);
            result.put("avatar", picture);
            result.put("email_collection_needed", emailCollectionNeeded);

            return result;

        } catch (Exception e) {
            log.error("获取Google用户信息失败", e);
            throw new UserInfoException("获取用户信息失败: " + e.getMessage(), "google", e);
        } finally {
            // 清理ThreadLocal
            idTokenHolder.remove();
        }
    }

    /**
     * 解析Google ID Token（不验证签名，仅用于提取信息）
     * 生产环境建议使用Google官方库验证签名
     */
    private Map<String, Object> parseIdToken(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new TokenException("无效的ID Token格式", "google");
            }

            // 解码payload
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);

            // 验证issuer
            String iss = (String) claims.get("iss");
            if (!"accounts.google.com".equals(iss) && !"https://accounts.google.com".equals(iss)) {
                throw new TokenException("无效的ID Token issuer", "google");
            }

            return claims;

        } catch (Exception e) {
            throw new TokenException("解析ID Token失败: " + e.getMessage(), "google", e);
        }
    }
}
