package com.ld.poetry.oauth.providers;

import com.ld.poetry.oauth.base.OAuth2Provider;
import com.ld.poetry.oauth.exception.UserInfoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GitHub OAuth 2.0 提供商
 *
 * @author LeapYa
 * @since 2026-01-10
 */
@Slf4j
@Component("githubOAuthProvider")
public class GitHubOAuthProvider extends OAuth2Provider {

    private static final String AUTH_URL = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_INFO_URL = "https://api.github.com/user";
    private static final String EMAILS_URL = "https://api.github.com/user/emails";

    @Override
    public String getProviderName() {
        return "github";
    }

    @Override
    protected String getAuthorizationUrl() {
        return AUTH_URL;
    }

    @Override
    protected String getTokenUrl() {
        return getProxiedUrl(TOKEN_URL, "/github/login/oauth/access_token");
    }

    @Override
    protected String getUserInfoUrl() {
        return getProxiedUrl(USER_INFO_URL, "/github/api/user");
    }

    @Override
    protected String getScope() {
        return "user:email";
    }

    @Override
    protected String getAuthorizationHeader(String accessToken) {
        return "token " + accessToken;  // GitHub使用token格式而非Bearer
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken) {
        try {
            // 获取用户基本信息
            Map<String, Object> userInfo = sendAuthenticatedGetRequest(getUserInfoUrl(), accessToken);

            // 获取用户邮箱
            String email = fetchPrimaryEmail(accessToken);

            // 检查邮箱收集需求
            Object[] emailCheck = checkEmailCollectionNeeded(email);
            String processedEmail = (String) emailCheck[0];
            boolean emailCollectionNeeded = (Boolean) emailCheck[1];

            // 返回标准化用户信息
            Map<String, Object> result = new HashMap<>();
            result.put("provider", "github");
            result.put("uid", String.valueOf(userInfo.get("id")));
            result.put("username", userInfo.get("login"));
            result.put("email", processedEmail);
            result.put("avatar", userInfo.get("avatar_url"));
            result.put("email_collection_needed", emailCollectionNeeded);

            return result;

        } catch (Exception e) {
            log.error("获取GitHub用户信息失败", e);
            throw new UserInfoException("获取用户信息失败: " + e.getMessage(), "github", e);
        }
    }

    /**
     * 获取用户主邮箱
     */
    private String fetchPrimaryEmail(String accessToken) {
        try {
            String emailsUrl = getProxiedUrl(EMAILS_URL, "/github/api/user/emails");
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", getAuthorizationHeader(accessToken));
            headers.add("Accept", "application/json");
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(emailsUrl, HttpMethod.GET, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // GitHub返回的是邮箱数组
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> emails = objectMapper.readValue(response.getBody(), List.class);
                return findPrimaryEmail(emails);
            }
            return null;

        } catch (Exception e) {
            log.warn("获取GitHub邮箱失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从邮箱列表中找到主邮箱
     */
    private String findPrimaryEmail(List<Map<String, Object>> emails) {
        if (emails == null || emails.isEmpty()) {
            return null;
        }

        for (Map<String, Object> emailInfo : emails) {
            Boolean primary = (Boolean) emailInfo.get("primary");
            Boolean verified = (Boolean) emailInfo.get("verified");
            if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                return (String) emailInfo.get("email");
            }
        }

        return null;
    }
}
