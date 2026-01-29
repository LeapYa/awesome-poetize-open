package com.ld.poetry.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 多重限流注解容器
 * 
 * <p>用于在同一方法上应用多个 @RateLimit 注解</p>
 * 
 * @author LeapYa
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimits {
    RateLimit[] value();
}
