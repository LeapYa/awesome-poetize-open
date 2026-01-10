package com.ld.poetry.oauth.providers;

import com.ld.poetry.oauth.base.OAuth2Provider;
import com.ld.poetry.oauth.exception.TokenException;
import com.ld.poetry.oauth.exception.UserInfoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QQ OAuth 2.0 提供商
 * QQ的OAuth返回格式非标准，需要特殊处理
 *
 * @author LeapYa
 * @since 2026-01-10
 */
@Slf4j
@Component("qqOAuthProvider")
public class QQOAuthProvider extends OAuth2Provider {

    private static final String AUTH_URL = "https://graph.qq.com/oauth2.0/authorize";
    private static final String TOKEN_URL = "https://graph.qq.com/oauth2.0/token";
    private static final String OPENID_URL = "https://graph.qq.com/oauth2.0/me";
    private static final String USER_INFO_URL = "https://graph.qq.com/user/get_user_info";

    // 用于存储openid
    private ThreadLocal<String> openidHolder = new ThreadLocal<>();

    @Override
    public String getProviderName() {
        return "qq";
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
        return "get_user_info";
    }

    @Override
    public Map<String, Object> getAccessToken(String code) {
        try {
            // QQ使用GET请求获取token
            String tokenUrl = getTokenUrl() + "?grant_type=authorization_code"
                    + "&client_id=" + config.getClientId()
                    + "&client_secret=" + config.getClientSecret()
                    + "&code=" + code
                    + "&redirect_uri=" + config.getRedirectUri();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Accept", "text/html");

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();

                // QQ返回格式: access_token=xxx&expires_in=xxx&refresh_token=xxx
                Map<String, Object> tokenData = parseQQTokenResponse(responseBody);

                if (tokenData.containsKey("error")) {
                    throw new TokenException("获取QQ访问令牌失败: " + tokenData.get("error_description"), "qq");
                }

                String accessToken = (String) tokenData.get("access_token");
                if (accessToken == null || accessToken.isEmpty()) {
                    throw new TokenException("QQ未返回访问令牌", "qq");
                }

                return tokenData;
            }

            throw new TokenException("获取QQ访问令牌失败: HTTP " + response.getStatusCode(), "qq");

        } catch (TokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取QQ访问令牌失败", e);
            throw new TokenException("获取访问令牌失败: " + e.getMessage(), "qq", e);
        }
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken) {
        try {
            // 1. 获取OpenID
            String openid = getOpenId(accessToken);
            openidHolder.set(openid);

            // 2. 获取用户信息
            String userInfoUrl = getUserInfoUrl()
                    + "?access_token=" + accessToken
                    + "&oauth_consumer_key=" + config.getClientId()
                    + "&openid=" + openid;

            HttpHeaders headers = new HttpHeaders();
            headers.add("Accept", "application/json");

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userInfo = objectMapper.readValue(response.getBody(), Map.class);

                // 检查返回状态
                Integer ret = (Integer) userInfo.get("ret");
                if (ret != null && ret != 0) {
                    String msg = (String) userInfo.get("msg");
                    throw new UserInfoException("QQ返回错误: " + msg, "qq");
                }

                // QQ登录默认不返回邮箱
                boolean emailCollectionNeeded = true;

                // 返回标准化用户信息
                Map<String, Object> result = new HashMap<>();
                result.put("provider", "qq");
                result.put("uid", openid);
                result.put("username", userInfo.get("nickname"));
                result.put("email", "");  // QQ默认不提供邮箱
                result.put("avatar", getAvatar(userInfo));
                result.put("email_collection_needed", emailCollectionNeeded);

                return result;
            }

            throw new UserInfoException("获取QQ用户信息失败: HTTP " + response.getStatusCode(), "qq");

        } catch (UserInfoException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取QQ用户信息失败", e);
            throw new UserInfoException("获取用户信息失败: " + e.getMessage(), "qq", e);
        } finally {
            openidHolder.remove();
        }
    }

    /**
     * 获取QQ OpenID
     * QQ返回格式: callback( {"client_id":"xxx","openid":"xxx"} );
     */
    private String getOpenId(String accessToken) {
        try {
            String openidUrl = OPENID_URL + "?access_token=" + accessToken;

            ResponseEntity<String> response = restTemplate.getForEntity(openidUrl, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();

                // 提取JSON部分
                String json = extractJsonFromCallback(responseBody);
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(json, Map.class);

                String openid = (String) data.get("openid");
                if (openid == null || openid.isEmpty()) {
                    throw new UserInfoException("QQ未返回OpenID", "qq");
                }

                return openid;
            }

            throw new UserInfoException("获取QQ OpenID失败", "qq");

        } catch (UserInfoException e) {
            throw e;
        } catch (Exception e) {
            throw new UserInfoException("获取OpenID失败: " + e.getMessage(), "qq", e);
        }
    }

    /**
     * 解析QQ Token响应
     * 格式: access_token=xxx&expires_in=xxx&refresh_token=xxx
     */
    private Map<String, Object> parseQQTokenResponse(String responseBody) {
        Map<String, Object> result = new HashMap<>();

        if (responseBody == null || responseBody.isEmpty()) {
            return result;
        }

        // 检查是否是错误响应
        if (responseBody.contains("callback")) {
            String json = extractJsonFromCallback(responseBody);
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> errorData = objectMapper.readValue(json, Map.class);
                return errorData;
            } catch (Exception e) {
                log.warn("解析QQ错误响应失败", e);
            }
        }

        // 解析URL编码格式
        String[] pairs = responseBody.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                result.put(keyValue[0], keyValue[1]);
            }
        }

        return result;
    }

    /**
     * 从callback格式中提取JSON
     * callback( {"client_id":"xxx","openid":"xxx"} ); -> {"client_id":"xxx","openid":"xxx"}
     */
    private String extractJsonFromCallback(String responseBody) {
        if (responseBody == null) {
            return "{}";
        }

        String trimmed = responseBody.trim();
        if (trimmed.startsWith("callback(")) {
            // 移除 callback( 和 );
            Pattern pattern = Pattern.compile("callback\\s*\\(\\s*(.+?)\\s*\\)\\s*;?");
            Matcher matcher = pattern.matcher(trimmed);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return trimmed;
    }

    /**
     * 获取最佳头像
     */
    private String getAvatar(Map<String, Object> userInfo) {
        // 优先使用高清头像
        String avatar = (String) userInfo.get("figureurl_qq_2");
        if (avatar != null && !avatar.isEmpty()) {
            return avatar;
        }

        avatar = (String) userInfo.get("figureurl_qq_1");
        if (avatar != null && !avatar.isEmpty()) {
            return avatar;
        }

        return "";
    }
}
