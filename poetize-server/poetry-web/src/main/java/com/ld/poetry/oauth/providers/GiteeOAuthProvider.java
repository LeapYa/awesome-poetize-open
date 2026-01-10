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
 * Gitee OAuth 2.0 提供商
 *
 * @author LeapYa
 * @since 2026-01-10
 */
@Slf4j
@Component("giteeOAuthProvider")
public class GiteeOAuthProvider extends OAuth2Provider {

    private static final String AUTH_URL = "https://gitee.com/oauth/authorize";
    private static final String TOKEN_URL = "https://gitee.com/oauth/token";
    private static final String USER_INFO_URL = "https://gitee.com/api/v5/user";
    private static final String EMAILS_URL = "https://gitee.com/api/v5/emails";

    @Override
    public String getProviderName() {
        return "gitee";
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
        return USER_INFO_URL;
    }

    @Override
    protected String getScope() {
        return "user_info emails";
    }

    @Override
    protected String getAuthorizationHeader(String accessToken) {
        return "token " + accessToken;  // Gitee使用token格式
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken) {
        try {
            // 获取用户基本信息
            Map<String, Object> userInfo = sendAuthenticatedGetRequest(getUserInfoUrl(), accessToken);

            // 获取用户邮箱
            String email = fetchEmail(accessToken);

            // 检查邮箱收集需求
            Object[] emailCheck = checkEmailCollectionNeeded(email);
            String processedEmail = (String) emailCheck[0];
            boolean emailCollectionNeeded = (Boolean) emailCheck[1];

            // 返回标准化用户信息
            Map<String, Object> result = new HashMap<>();
            result.put("provider", "gitee");
            result.put("uid", String.valueOf(userInfo.get("id")));
            result.put("username", userInfo.get("login"));
            result.put("email", processedEmail);
            result.put("avatar", userInfo.get("avatar_url"));
            result.put("email_collection_needed", emailCollectionNeeded);

            return result;

        } catch (Exception e) {
            log.error("获取Gitee用户信息失败", e);
            throw new UserInfoException("获取用户信息失败: " + e.getMessage(), "gitee", e);
        }
    }

    /**
     * 获取用户邮箱
     */
    private String fetchEmail(String accessToken) {
        try {
            // Gitee邮箱API需要带token参数
            String emailsUrl = EMAILS_URL + "?access_token=" + accessToken;
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Accept", "application/json");
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(emailsUrl, HttpMethod.GET, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Gitee返回的是邮箱数组
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> emails = objectMapper.readValue(response.getBody(), List.class);
                return findBestEmail(emails);
            }
            return null;

        } catch (Exception e) {
            log.warn("获取Gitee邮箱失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从邮箱列表中找到最佳邮箱
     * 优先级：主邮箱 > 已验证邮箱 > 第一个邮箱
     */
    private String findBestEmail(List<Map<String, Object>> emails) {
        if (emails == null || emails.isEmpty()) {
            return null;
        }

        String primaryEmail = null;
        String verifiedEmail = null;
        String firstEmail = null;

        for (Map<String, Object> emailInfo : emails) {
            String email = (String) emailInfo.get("email");
            
            if (firstEmail == null) {
                firstEmail = email;
            }

            Boolean primary = (Boolean) emailInfo.get("primary");
            if (Boolean.TRUE.equals(primary)) {
                primaryEmail = email;
            }

            Boolean verified = (Boolean) emailInfo.get("verified");
            if (Boolean.TRUE.equals(verified) && verifiedEmail == null) {
                verifiedEmail = email;
            }
        }

        // 返回优先级最高的邮箱
        if (primaryEmail != null) {
            return primaryEmail;
        }
        if (verifiedEmail != null) {
            return verifiedEmail;
        }
        return firstEmail;
    }
}
