package com.ld.poetry.controller;

import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.oauth.OAuthProviderFactory;
import com.ld.poetry.oauth.base.BaseOAuthProvider;
import com.ld.poetry.oauth.exception.ConfigurationException;
import com.ld.poetry.oauth.exception.OAuthException;
import com.ld.poetry.oauth.providers.TwitterOAuthProvider;
import com.ld.poetry.oauth.state.OAuthStateService;
import com.ld.poetry.service.UserService;
import com.ld.poetry.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth登录控制器
 * 处理OAuth登录入口和回调
 *
 * @author LeapYa
 * @since 2026-01-10
 */
@Slf4j
@RestController
@RequestMapping("/oauth")
public class OAuthLoginController {

    @Autowired
    private OAuthProviderFactory providerFactory;

    @Autowired
    private OAuthStateService stateService;

    @Autowired
    private UserService userService;

    /**
     * OAuth登录入口
     * 重定向到第三方OAuth授权页面
     */
    @GetMapping("/login/{provider}")
    public void login(@PathVariable String provider,
                      @RequestParam(required = false) String redirect,
                      HttpServletRequest request,
                      HttpServletResponse response) throws IOException {
        try {
            log.info("OAuth登录请求: provider={}", provider);

            // 获取Provider
            BaseOAuthProvider oauthProvider = providerFactory.getProvider(provider);

            // 处理Twitter OAuth 1.0特殊流程
            if (oauthProvider instanceof TwitterOAuthProvider) {
                handleTwitterLogin((TwitterOAuthProvider) oauthProvider, redirect, request, response);
                return;
            }

            // OAuth 2.0 标准流程
            // 生成state token（包含redirect路径）
            String sessionId = getSessionId(request);
            String state = stateService.generateState(provider, sessionId, redirect);

            // 获取授权URL并重定向
            String authUrl = oauthProvider.getAuthUrl(state);
            log.info("重定向到OAuth授权页面: provider={}", provider);
            response.sendRedirect(authUrl);

        } catch (ConfigurationException e) {
            log.warn("OAuth配置错误: provider={}, error={}", provider, e.getMessage());
            redirectToError(response, "未配置信息，请先在后台设置", provider);
        } catch (Exception e) {
            log.error("OAuth登录失败: provider={}", provider, e);
            redirectToError(response, "登录服务暂时不可用", provider);
        }
    }

    /**
     * 处理Twitter OAuth 1.0登录
     */
    private void handleTwitterLogin(TwitterOAuthProvider provider, String redirect,
                                     HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // 获取Request Token
            String callbackUri = provider.getConfig().getRedirectUri();
            Map<String, String> requestTokenData = provider.getRequestToken(callbackUri);

            // 存储token secret到session
            HttpSession session = request.getSession();
            session.setAttribute("x_oauth_token_secret", requestTokenData.get("oauth_token_secret"));

            // 保存redirect路径
            if (StringUtils.hasText(redirect)) {
                session.setAttribute("x_redirect", redirect);
            }

            // 重定向到Twitter授权页面
            String authUrl = "https://api.twitter.com/oauth/authenticate?oauth_token=" + requestTokenData.get("oauth_token");
            response.sendRedirect(authUrl);

        } catch (Exception e) {
            log.error("Twitter登录失败", e);
            redirectToError(response, "Twitter登录服务暂时不可用", "x");
        }
    }

    /**
     * OAuth回调处理
     */
    @GetMapping("/callback/{provider}")
    public void callback(@PathVariable String provider,
                         @RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         @RequestParam(required = false) String error,
                         @RequestParam(required = false) String oauth_token,
                         @RequestParam(required = false) String oauth_verifier,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        try {
            log.info("OAuth回调: provider={}", provider);

            // 检查OAuth错误
            if (StringUtils.hasText(error)) {
                log.warn("OAuth授权被拒绝: provider={}, error={}", provider, error);
                redirectToError(response, error, provider);
                return;
            }

            // 获取Provider
            BaseOAuthProvider oauthProvider = providerFactory.getProvider(provider);

            // 处理Twitter OAuth 1.0回调
            if (oauthProvider instanceof TwitterOAuthProvider) {
                handleTwitterCallback((TwitterOAuthProvider) oauthProvider, oauth_token, oauth_verifier, request, response);
                return;
            }

            // OAuth 2.0 标准回调处理

            // 验证state
            if (!StringUtils.hasText(state)) {
                log.warn("回调缺少state参数: provider={}", provider);
                redirectToError(response, "安全验证失败", provider);
                return;
            }

            Map<String, Object> stateData = stateService.verifyAndConsumeState(state, provider);
            if (stateData == null) {
                log.warn("state验证失败: provider={}", provider);
                redirectToError(response, "安全验证失败，请重新授权", provider);
                return;
            }

            // 验证授权码
            if (!StringUtils.hasText(code)) {
                log.warn("回调缺少授权码: provider={}", provider);
                redirectToError(response, "授权失败，缺少授权码", provider);
                return;
            }

            // 获取访问令牌
            Map<String, Object> tokenData = oauthProvider.getAccessToken(code);
            String accessToken = (String) tokenData.get("access_token");

            // 获取用户信息
            Map<String, Object> userInfo = oauthProvider.getUserInfo(accessToken);

            // 处理登录
            processLogin(userInfo, stateData, request, response);

        } catch (ConfigurationException e) {
            log.warn("OAuth配置错误: provider={}, error={}", provider, e.getMessage());
            redirectToError(response, "配置错误", provider);
        } catch (OAuthException e) {
            log.error("OAuth回调处理失败: provider={}, error={}", provider, e.getMessage());
            redirectToError(response, "授权失败，请重试", provider);
        } catch (Exception e) {
            log.error("OAuth回调异常: provider={}", provider, e);
            redirectToError(response, "回调处理失败", provider);
        }
    }

    /**
     * 处理Twitter OAuth 1.0回调
     */
    private void handleTwitterCallback(TwitterOAuthProvider provider, String oauthToken, String oauthVerifier,
                                        HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // 获取session中的token secret
            HttpSession session = request.getSession();
            String oauthTokenSecret = (String) session.getAttribute("x_oauth_token_secret");

            if (!StringUtils.hasText(oauthToken) || !StringUtils.hasText(oauthVerifier) || !StringUtils.hasText(oauthTokenSecret)) {
                log.warn("Twitter回调参数不完整");
                redirectToError(response, "授权参数不完整", "x");
                return;
            }

            // 获取Access Token
            Map<String, String> accessTokenData = provider.getAccessTokenWithVerifier(oauthToken, oauthTokenSecret, oauthVerifier);

            // 获取用户信息
            Map<String, Object> userInfo = provider.getUserInfoWithSecret(
                    accessTokenData.get("access_token"),
                    accessTokenData.get("access_token_secret")
            );

            // 清理session
            session.removeAttribute("x_oauth_token_secret");

            // 处理登录 - 构建Twitter的状态数据
            Map<String, Object> stateData = new HashMap<>();
            stateData.put("provider", "x");
            Object redirectPath = session.getAttribute("x_redirect");
            if (redirectPath != null) stateData.put("redirect_path", redirectPath);
            session.removeAttribute("x_redirect");
            
            processLogin(userInfo, stateData, request, response);

        } catch (Exception e) {
            log.error("Twitter回调处理失败", e);
            redirectToError(response, "Twitter授权失败", "x");
        }
    }

    /**
     * 处理登录结果
     */
    private void processLogin(Map<String, Object> userInfo, Map<String, Object> stateData,
                               HttpServletRequest request, HttpServletResponse response) throws IOException {
        String provider = (String) stateData.getOrDefault("provider", "unknown");
        try {
            // 获取保存的redirectPath
            String redirectPath = (String) stateData.get("redirect_path");

            // 调用用户服务处理第三方登录
            String uid = (String) userInfo.get("uid");
            String username = (String) userInfo.get("username");
            String email = (String) userInfo.get("email");
            String avatar = (String) userInfo.get("avatar");
            Boolean emailCollectionNeeded = (Boolean) userInfo.get("email_collection_needed");

            PoetryResult<UserVO> result = userService.thirdLogin(provider, uid, username, email, avatar);

            if (result.isSuccess()) {
                UserVO userVO = result.getData();
                String accessToken = userVO.getAccessToken();

                // 构建重定向URL（使用相对路径）
                StringBuilder redirectUrl = new StringBuilder("/?userToken=").append(accessToken);

                // 检查是否需要邮箱收集
                boolean userHasEmail = StringUtils.hasText(userVO.getEmail());
                if (!userHasEmail && Boolean.TRUE.equals(emailCollectionNeeded)) {
                    redirectUrl.append("&emailCollectionNeeded=true");
                }

                // 添加原始重定向路径
                if (StringUtils.hasText(redirectPath)) {
                    redirectUrl.append("&redirect=").append(URLEncoder.encode(redirectPath, StandardCharsets.UTF_8.name()));
                }

                log.info("OAuth登录成功: provider={}, userId={}", provider, userVO.getId());
                response.sendRedirect(redirectUrl.toString());

            } else {
                log.warn("用户登录失败: provider={}, error={}", provider, result.getMessage());
                redirectToError(response, result.getMessage(), provider);
            }

        } catch (Exception e) {
            log.error("处理登录结果失败", e);
            redirectToError(response, "登录处理失败", provider);
        }
    }

    /**
     * 重定向到错误页面（使用相对路径）
     */
    private void redirectToError(HttpServletResponse response, String error, String provider) throws IOException {
        String errorUrl = "/oauth-callback?error=" + URLEncoder.encode(error, StandardCharsets.UTF_8.name())
                + "&platform=" + provider;
        response.sendRedirect(errorUrl);
    }

    /**
     * 获取会话ID
     */
    private String getSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        return session.getId();
    }

    /**
     * 获取支持的OAuth提供商列表
     */
    @GetMapping("/providers")
    public PoetryResult<Map<String, Object>> getProviders() {
        Map<String, Object> result = new HashMap<>();
        result.put("supported_providers", providerFactory.getSupportedProviders());
        result.put("enabled_providers", providerFactory.getEnabledProviders());
        return PoetryResult.success(result);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public PoetryResult<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", "oauth-login-java");
        result.put("status", "ok");
        result.put("version", "1.0.0");
        result.put("timestamp", System.currentTimeMillis());
        result.put("state_stats", stateService.getStats());
        return PoetryResult.success(result);
    }
}
