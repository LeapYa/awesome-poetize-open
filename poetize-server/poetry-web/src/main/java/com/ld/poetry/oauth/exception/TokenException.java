package com.ld.poetry.oauth.exception;

/**
 * OAuth Token获取异常
 *
 * @author LeapYa
 * @since 2026-01-10
 */
public class TokenException extends OAuthException {

    private static final long serialVersionUID = 1L;

    public TokenException(String message, String provider) {
        super(message, "token_error", provider);
    }

    public TokenException(String message, String errorCode, String provider) {
        super(message, errorCode, provider);
    }

    public TokenException(String message, String provider, Throwable cause) {
        super(message, "token_error", provider, cause);
    }
}
