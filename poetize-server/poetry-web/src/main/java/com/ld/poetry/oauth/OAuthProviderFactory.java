package com.ld.poetry.oauth;

import com.ld.poetry.entity.ThirdPartyOauthConfig;
import com.ld.poetry.oauth.base.BaseOAuthProvider;
import com.ld.poetry.oauth.base.OAuth1Provider;
import com.ld.poetry.oauth.base.OAuth2Provider;
import com.ld.poetry.oauth.exception.ConfigurationException;
import com.ld.poetry.oauth.providers.*;
import com.ld.poetry.service.ThirdPartyOauthConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OAuth提供商工厂
 * 根据平台类型创建对应的OAuth提供商实例
 *
 * @author LeapYa
 * @since 2026-01-10
 */
@Slf4j
@Component
public class OAuthProviderFactory {

    @Autowired
    private ThirdPartyOauthConfigService configService;

    @Autowired
    private GitHubOAuthProvider gitHubOAuthProvider;

    @Autowired
    private GoogleOAuthProvider googleOAuthProvider;

    @Autowired
    private TwitterOAuthProvider twitterOAuthProvider;

    @Autowired
    private YandexOAuthProvider yandexOAuthProvider;

    @Autowired
    private GiteeOAuthProvider giteeOAuthProvider;

    @Autowired
    private QQOAuthProvider qqOAuthProvider;

    @Autowired
    private BaiduOAuthProvider baiduOAuthProvider;

    @Autowired
    private AfdianOAuthProvider afdianOAuthProvider;

    /**
     * 支持的OAuth提供商列表
     */
    private static final List<String> SUPPORTED_PROVIDERS = Arrays.asList(
            "github", "google", "x", "twitter", "yandex", "gitee", "qq", "baidu", "afdian");

    /**
     * 获取OAuth提供商
     *
     * @param platformType 平台类型
     * @return OAuth提供商实例
     */
    public BaseOAuthProvider getProvider(String platformType) {
        // 获取配置
        ThirdPartyOauthConfig oauthConfig = configService.getByPlatformType(platformType);
        if (oauthConfig == null) {
            throw new ConfigurationException("平台配置不存在: " + platformType, platformType);
        }

        if (!oauthConfig.getEnabled() || !oauthConfig.getGlobalEnabled()) {
            throw new ConfigurationException("平台未启用: " + platformType, platformType);
        }

        // 获取对应的Provider并设置配置
        BaseOAuthProvider provider = getProviderInstance(platformType);

        // 设置配置
        if (provider instanceof OAuth2Provider) {
            ((OAuth2Provider) provider).setConfig(oauthConfig);
        } else if (provider instanceof OAuth1Provider) {
            ((OAuth1Provider) provider).setConfig(oauthConfig);
        }

        // 验证配置
        if (!provider.validateConfig()) {
            throw new ConfigurationException("平台配置不完整: " + platformType, platformType);
        }

        return provider;
    }

    /**
     * 获取Provider实例
     */
    private BaseOAuthProvider getProviderInstance(String platformType) {
        switch (platformType.toLowerCase()) {
            case "github":
                return gitHubOAuthProvider;
            case "google":
                return googleOAuthProvider;
            case "x":
            case "twitter":
                return twitterOAuthProvider;
            case "yandex":
                return yandexOAuthProvider;
            case "gitee":
                return giteeOAuthProvider;
            case "qq":
                return qqOAuthProvider;
            case "baidu":
                return baiduOAuthProvider;
            case "afdian":
                return afdianOAuthProvider;
            default:
                throw new ConfigurationException("不支持的OAuth平台: " + platformType, platformType);
        }
    }

    /**
     * 获取支持的提供商列表
     */
    public List<String> getSupportedProviders() {
        return SUPPORTED_PROVIDERS;
    }

    /**
     * 获取已启用的提供商列表
     */
    public List<String> getEnabledProviders() {
        return SUPPORTED_PROVIDERS.stream()
                .filter(this::isProviderEnabled)
                .collect(Collectors.toList());
    }

    /**
     * 检查提供商是否启用
     */
    public boolean isProviderEnabled(String platformType) {
        try {
            ThirdPartyOauthConfig config = configService.getByPlatformType(platformType);
            return config != null && config.getEnabled() && config.getGlobalEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查提供商是否支持
     */
    public boolean isProviderSupported(String platformType) {
        return SUPPORTED_PROVIDERS.contains(platformType.toLowerCase());
    }
}
