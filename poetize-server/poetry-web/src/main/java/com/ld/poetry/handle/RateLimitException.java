package com.ld.poetry.handle;

/**
 * 限流异常
 * 
 * <p>当请求触发限流规则时抛出此异常</p>
 * 
 * @author LeapYa
 */
public class RateLimitException extends RuntimeException {
    
    /**
     * 限流器名称
     */
    private final String limitName;
    
    /**
     * 建议重试等待时间（秒）
     */
    private final long retryAfterSeconds;
    
    /**
     * 限流Key
     */
    private final String limitKey;
    
    public RateLimitException(String message, String limitName, long retryAfterSeconds, String limitKey) {
        super(message);
        this.limitName = limitName;
        this.retryAfterSeconds = retryAfterSeconds;
        this.limitKey = limitKey;
    }
    
    public RateLimitException(String message, String limitName, long retryAfterSeconds) {
        this(message, limitName, retryAfterSeconds, null);
    }
    
    public String getLimitName() {
        return limitName;
    }
    
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
    
    public String getLimitKey() {
        return limitKey;
    }
}
