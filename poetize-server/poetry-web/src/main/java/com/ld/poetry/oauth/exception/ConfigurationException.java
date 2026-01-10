package com.ld.poetry.oauth.exception;

/**
 * OAuth配置异常
 *
 * @author LeapYa
 * @since 2026-01-10
 */
public class ConfigurationException extends OAuthException {

    private static final long serialVersionUID = 1L;

    public ConfigurationException(String message, String provider) {
        super(message, "config_error", provider);
    }

    public ConfigurationException(String message, String errorCode, String provider) {
        super(message, errorCode, provider);
    }
}
