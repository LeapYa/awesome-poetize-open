package com.ld.poetry.oauth.exception;

/**
 * OAuth用户信息获取异常
 *
 * @author LeapYa
 * @since 2026-01-10
 */
public class UserInfoException extends OAuthException {

    private static final long serialVersionUID = 1L;

    public UserInfoException(String message, String provider) {
        super(message, "user_info_error", provider);
    }

    public UserInfoException(String message, String errorCode, String provider) {
        super(message, errorCode, provider);
    }

    public UserInfoException(String message, String provider, Throwable cause) {
        super(message, "user_info_error", provider, cause);
    }
}
