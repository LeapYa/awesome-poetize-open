package com.ld.poetry.oauth.providers;

import com.ld.poetry.oauth.base.OAuth2Provider;
import com.ld.poetry.oauth.exception.UserInfoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Yandex OAuth 2.0 提供商
 *
 * @author LeapYa
 * @since 2026-01-10
 */
@Slf4j
@Component("yandexOAuthProvider")
public class YandexOAuthProvider extends OAuth2Provider {

    private static final String AUTH_URL = "https://oauth.yandex.com/authorize";
    private static final String TOKEN_URL = "https://oauth.yandex.com/token";
    private static final String USER_INFO_URL = "https://login.yandex.ru/info";

    @Override
    public String getProviderName() {
        return "yandex";
    }

    @Override
    protected String getAuthorizationUrl() {
        return AUTH_URL;
    }

    @Override
    protected String getTokenUrl() {
        return getProxiedUrl(TOKEN_URL, "/yandex/token");
    }

    @Override
    protected String getUserInfoUrl() {
        return getProxiedUrl(USER_INFO_URL, "/yandex/login/info");
    }

    @Override
    protected String getScope() {
        return "login:email login:info";
    }

    @Override
    protected String getAuthorizationHeader(String accessToken) {
        return "OAuth " + accessToken;  // Yandex使用OAuth格式
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken) {
        try {
            Map<String, Object> userInfo = sendAuthenticatedGetRequest(getUserInfoUrl() + "?format=json", accessToken);

            // 获取邮箱
            String email = (String) userInfo.get("default_email");

            // 检查邮箱收集需求
            Object[] emailCheck = checkEmailCollectionNeeded(email);
            String processedEmail = (String) emailCheck[0];
            boolean emailCollectionNeeded = (Boolean) emailCheck[1];

            // 构建头像URL
            String avatarId = (String) userInfo.get("default_avatar_id");
            String avatar = avatarId != null 
                    ? "https://avatars.yandex.net/get-yapic/" + avatarId + "/islands-200" 
                    : "";

            // 返回标准化用户信息
            Map<String, Object> result = new HashMap<>();
            result.put("provider", "yandex");
            result.put("uid", userInfo.get("id"));
            result.put("username", userInfo.get("login"));
            result.put("email", processedEmail);
            result.put("avatar", avatar);
            result.put("email_collection_needed", emailCollectionNeeded);

            return result;

        } catch (Exception e) {
            log.error("获取Yandex用户信息失败", e);
            throw new UserInfoException("获取用户信息失败: " + e.getMessage(), "yandex", e);
        }
    }
}
