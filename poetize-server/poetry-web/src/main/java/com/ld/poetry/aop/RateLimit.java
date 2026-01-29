package com.ld.poetry.aop;

import java.lang.annotation.*;

/**
 * 接口限流注解
 * 
 * <p>支持多维度限流：账号、指纹、IP，可叠加使用</p>
 * 
 * <p>使用示例：</p>
 * <pre>
 * // 单一限流
 * @RateLimit(name = "login", count = 5, time = 300)
 * 
 * // 多维度叠加限流
 * @RateLimits({
 *     @RateLimit(name = "login:account", count = 5, time = 300, keyType = KeyType.CUSTOM, key = "#account"),
 *     @RateLimit(name = "login:fp", count = 20, time = 300, keyType = KeyType.FINGERPRINT),
 *     @RateLimit(name = "login:ip", count = 500, time = 60, keyType = KeyType.IP)
 * })
 * </pre>
 * 
 * @author LeapYa
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RateLimits.class)
public @interface RateLimit {
    
    /**
     * 限流器名称，用于区分不同的限流规则
     */
    String name();
    
    /**
     * 时间窗口内允许的最大请求数
     */
    int count() default 10;
    
    /**
     * 时间窗口（秒）
     */
    int time() default 60;
    
    /**
     * 限流Key类型
     */
    KeyType keyType() default KeyType.IP;
    
    /**
     * 自定义Key的SpEL表达式（当keyType=CUSTOM时生效）
     * 
     * <p>支持的变量：</p>
     * <ul>
     *   <li>#参数名 - 方法参数</li>
     *   <li>#root.args[0] - 第一个参数</li>
     *   <li>#root.method.name - 方法名</li>
     * </ul>
     */
    String key() default "";
    
    /**
     * 限流提示消息
     */
    String message() default "操作过于频繁，请稍后再试";
    
    /**
     * Redis故障时是否放行（fail-open）
     * true: Redis故障时放行请求
     * false: Redis故障时拒绝请求
     */
    boolean failOpen() default true;
    
    /**
     * 限流Key类型枚举
     */
    enum KeyType {
        /**
         * 基于客户端IP（兜底方案，阈值应设置较高）
         */
        IP,
        
        /**
         * 基于浏览器指纹（从请求头X-Fingerprint获取）
         */
        FINGERPRINT,
        
        /**
         * 基于已登录用户ID
         */
        USER,
        
        /**
         * 自定义Key（通过SpEL表达式指定）
         */
        CUSTOM,
        
        /**
         * 组合Key：指纹优先，IP兜底
         */
        FINGERPRINT_OR_IP
    }
}
