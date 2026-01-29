package com.ld.poetry.aop;

import com.ld.poetry.aop.RateLimit.KeyType;
import com.ld.poetry.entity.User;
import com.ld.poetry.handle.RateLimitException;
import com.ld.poetry.utils.PoetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Collections;

/**
 * 限流切面
 * 
 * <p>使用Redis Lua脚本实现原子性限流，支持滑动窗口计数器算法</p>
 * 
 * @author LeapYa
 */
@Aspect
@Component
@Order(0) // 限流优先级最高，在其他切面之前执行
@Slf4j
public class RateLimitAspect {
    
    /**
     * 限流Redis Key前缀
     */
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    
    /**
     * 浏览器指纹请求头名称
     */
    private static final String FINGERPRINT_HEADER = "X-Fingerprint";
    
    /**
     * Redis Lua限流脚本
     * 
     * <p>原子性操作：INCR + EXPIRE</p>
     * <p>返回值：1=放行，0=限流</p>
     */
    private static final String RATE_LIMIT_LUA_SCRIPT = 
        "local key = KEYS[1] " +
        "local limit = tonumber(ARGV[1]) " +
        "local window = tonumber(ARGV[2]) " +
        "local current = redis.call('INCR', key) " +
        "if current == 1 then " +
        "    redis.call('EXPIRE', key, window) " +
        "end " +
        "if current > limit then " +
        "    return 0 " +
        "end " +
        "return 1";
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    private final SpelExpressionParser spelParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    
    /**
     * 处理单个 @RateLimit 注解
     */
    @Around("@annotation(rateLimit)")
    public Object aroundSingle(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        checkRateLimit(joinPoint, rateLimit);
        return joinPoint.proceed();
    }
    
    /**
     * 处理多个 @RateLimit 注解（@RateLimits容器）
     */
    @Around("@annotation(rateLimits)")
    public Object aroundMultiple(ProceedingJoinPoint joinPoint, RateLimits rateLimits) throws Throwable {
        for (RateLimit rateLimit : rateLimits.value()) {
            checkRateLimit(joinPoint, rateLimit);
        }
        return joinPoint.proceed();
    }
    
    /**
     * 执行限流检查
     */
    private void checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String limitKey = buildLimitKey(joinPoint, rateLimit);
        
        // Key为空时跳过限流（如：未登录用户使用USER类型）
        if (limitKey == null || limitKey.isEmpty()) {
            log.debug("[RateLimit] 跳过限流检查，Key为空: name={}", rateLimit.name());
            return;
        }
        
        String redisKey = RATE_LIMIT_KEY_PREFIX + rateLimit.name() + ":" + limitKey;
        
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMIT_LUA_SCRIPT, Long.class);
            Long result = stringRedisTemplate.execute(
                script,
                Collections.singletonList(redisKey),
                String.valueOf(rateLimit.count()),
                String.valueOf(rateLimit.time())
            );
            
            if (result != null && result == 0) {
                // 触发限流
                log.warn("[RateLimit] 触发限流: name={}, key={}, limit={}/{}", 
                    rateLimit.name(), maskKey(limitKey), rateLimit.count(), rateLimit.time());
                throw new RateLimitException(
                    rateLimit.message(),
                    rateLimit.name(),
                    rateLimit.time(),
                    maskKey(limitKey)
                );
            }
            
            log.debug("[RateLimit] 通过: name={}, key={}", rateLimit.name(), maskKey(limitKey));
            
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.error("[RateLimit] Redis执行异常: name={}, key={}", rateLimit.name(), maskKey(limitKey), e);
            if (!rateLimit.failOpen()) {
                // fail-close: Redis故障时拒绝请求
                throw new RateLimitException(
                    "系统繁忙，请稍后再试",
                    rateLimit.name(),
                    rateLimit.time(),
                    maskKey(limitKey)
                );
            }
            // fail-open: Redis故障时放行
        }
    }
    
    /**
     * 构建限流Key
     */
    private String buildLimitKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        KeyType keyType = rateLimit.keyType();
        
        switch (keyType) {
            case IP:
                return getClientIp();
                
            case FINGERPRINT:
                return getFingerprint();
                
            case USER:
                return getUserId();
                
            case FINGERPRINT_OR_IP:
                String fp = getFingerprint();
                return (fp != null && !fp.isEmpty()) ? "fp:" + fp : "ip:" + getClientIp();
                
            case CUSTOM:
                return resolveSpelKey(joinPoint, rateLimit.key());
                
            default:
                return getClientIp();
        }
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        try {
            HttpServletRequest request = PoetryUtil.getRequest();
            if (request == null) {
                return "unknown";
            }
            return PoetryUtil.getIpAddr(request);
        } catch (Exception e) {
            log.warn("[RateLimit] 获取客户端IP失败", e);
            return "unknown";
        }
    }
    
    /**
     * 获取浏览器指纹
     */
    private String getFingerprint() {
        try {
            HttpServletRequest request = PoetryUtil.getRequest();
            if (request == null) {
                return null;
            }
            String fingerprint = request.getHeader(FINGERPRINT_HEADER);
            // 指纹长度验证（FingerprintJS生成的是32位hash）
            if (fingerprint != null && fingerprint.length() >= 8 && fingerprint.length() <= 64) {
                return fingerprint;
            }
            return null;
        } catch (Exception e) {
            log.warn("[RateLimit] 获取浏览器指纹失败", e);
            return null;
        }
    }
    
    /**
     * 获取当前登录用户ID
     */
    private String getUserId() {
        try {
            User user = PoetryUtil.getCurrentUser();
            if (user != null && user.getId() != null) {
                return String.valueOf(user.getId());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 解析SpEL表达式
     */
    private String resolveSpelKey(ProceedingJoinPoint joinPoint, String spelExpression) {
        if (spelExpression == null || spelExpression.isEmpty()) {
            return null;
        }
        
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] args = joinPoint.getArgs();
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            
            EvaluationContext context = new StandardEvaluationContext();
            
            // 绑定方法参数到SpEL上下文
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
            
            // 特殊变量：请求对象
            try {
                context.setVariable("request", PoetryUtil.getRequest());
                context.setVariable("ip", getClientIp());
                context.setVariable("fingerprint", getFingerprint());
                context.setVariable("userId", getUserId());
            } catch (Exception ignored) {}
            
            Expression expression = spelParser.parseExpression(spelExpression);
            Object value = expression.getValue(context);
            
            return value != null ? String.valueOf(value) : null;
            
        } catch (Exception e) {
            log.error("[RateLimit] SpEL表达式解析失败: expression={}", spelExpression, e);
            return null;
        }
    }
    
    /**
     * 脱敏Key（日志输出用）
     */
    private String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return key;
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
