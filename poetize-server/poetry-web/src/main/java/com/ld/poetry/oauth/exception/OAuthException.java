package com.ld.poetry.oauth.exception;

/**
 * OAuth基础异常类
 *
 * @author LeapYa
 * @since 2026-01-10
 */
public class OAuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String provider;

    public OAuthException(String message) {
        super(message);
        this.errorCode = null;
        this.provider = null;
    }

    public OAuthException(String message, String errorCode, String provider) {
        super(message);
        this.errorCode = errorCode;
        this.provider = provider;
    }

    public OAuthException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.provider = null;
    }

    public OAuthException(String message, String errorCode, String provider, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.provider = provider;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getProvider() {
        return provider;
    }
}
