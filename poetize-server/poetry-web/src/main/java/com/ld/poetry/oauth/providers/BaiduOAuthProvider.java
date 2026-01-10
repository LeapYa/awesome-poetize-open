package com.ld.poetry.oauth.providers;

import com.ld.poetry.oauth.base.OAuth2Provider;
import com.ld.poetry.oauth.exception.UserInfoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Baidu OAuth 2.0 提供商
 *
 * @author LeapYa
 * @since 2026-01-10
 */
@Slf4j
@Component("baiduOAuthProvider")
public class BaiduOAuthProvider extends OAuth2Provider {

    private static final String AUTH_URL = "https://openapi.baidu.com/oauth/2.0/authorize";
    private static final String TOKEN_URL = "https://openapi.baidu.com/oauth/2.0/token";
    private static final String USER_INFO_URL = "https://openapi.baidu.com/rest/2.0/passport/users/getInfo";

    @Override
    public String getProviderName() {
        return "baidu";
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
        return "basic";
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken) {
        try {
            // Baidu使用POST请求获取用户信息
            String userInfoUrl = getUserInfoUrl() + "?access_token=" + accessToken + "&get_unionid=1";
            
            Map<String, Object> userInfo = sendAuthenticatedGetRequest(userInfoUrl, accessToken);

            // 检查错误
            if (userInfo.containsKey("error_code")) {
                String errorMsg = (String) userInfo.get("error_msg");
                throw new UserInfoException("Baidu返回错误: " + errorMsg, "baidu");
            }

            // 获取用户信息
            String uid = userInfo.get("uid") != null 
                    ? String.valueOf(userInfo.get("uid")) 
                    : String.valueOf(userInfo.get("openid"));
            String username = userInfo.get("uname") != null 
                    ? (String) userInfo.get("uname") 
                    : (String) userInfo.get("username");

            // 构建头像URL
            String portrait = (String) userInfo.get("portrait");
            String avatar = portrait != null && !portrait.isEmpty()
                    ? "https://himg.bdimg.com/sys/portrait/item/" + portrait + ".jpg"
                    : "";

            // Baidu接口可能不直接返回邮箱
            String email = (String) userInfo.get("email");
            Object[] emailCheck = checkEmailCollectionNeeded(email);
            String processedEmail = (String) emailCheck[0];
            boolean emailCollectionNeeded = (Boolean) emailCheck[1];

            // 返回标准化用户信息
            Map<String, Object> result = new HashMap<>();
            result.put("provider", "baidu");
            result.put("uid", uid != null ? uid : "");
            result.put("username", username != null ? username : "");
            result.put("email", processedEmail);
            result.put("avatar", avatar);
            result.put("email_collection_needed", emailCollectionNeeded);

            return result;

        } catch (UserInfoException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取Baidu用户信息失败", e);
            throw new UserInfoException("获取用户信息失败: " + e.getMessage(), "baidu", e);
        }
    }
}
