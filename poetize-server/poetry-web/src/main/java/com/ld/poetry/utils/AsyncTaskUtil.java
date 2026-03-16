package com.ld.poetry.utils;

import com.ld.poetry.config.AsyncUserContext;
import com.ld.poetry.entity.User;
import com.ld.poetry.utils.cache.UserCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import com.ld.poetry.constants.CommonConst;

/**
 * 异步任务工具类
 * 提供异步任务中常用的用户信息获取方法
 */
@Component
@Slf4j
public class AsyncTaskUtil {
    
    @Autowired
    private UserCacheManager userCacheManager;
    
    private static UserCacheManager staticUserCacheManager;
    
    @PostConstruct
    public void init() {
        staticUserCacheManager = userCacheManager;
    }
    
    /**
     * 获取当前异步任务中的用户信息
     * @return 用户对象，如果不存在则返回null
     */
    public static User getCurrentUser() {
        return RetryUtil.executeWithRetry(() -> {
            // 优先从异步上下文获取
            User user = AsyncUserContext.getUser();
            if (user != null) {
                return user;
            }
            
            // 尝试从缓存获取
            if (staticUserCacheManager != null) {
                String token = AsyncUserContext.getToken();
                if (token != null) {
                    user = staticUserCacheManager.getUserByToken(token);
                    if (user != null) {
                        return user;
                    }
                }
            }
            
            return null;
        });
    }
    
    /**
     * 获取当前异步任务中的用户ID
     * @return 用户ID，如果不存在则返回null
     */
    public static Integer getCurrentUserId() {
        return RetryUtil.executeWithRetry(() -> {
            // 优先从异步上下文获取
            Integer userId = AsyncUserContext.getCurrentUserId();
            if (userId != null) {
                return userId;
            }
            
            // 尝试从缓存获取
            User user = getCurrentUser();
            return user != null ? user.getId() : null;
        });
    }
    
    /**
     * 获取当前异步任务中的用户名
     * @return 用户名，如果不存在则返回"匿名用户"
     */
    public static String getCurrentUsername() {
        return RetryUtil.executeWithRetry(() -> {
            // 优先从异步上下文获取
            String username = AsyncUserContext.getCurrentUsername();
            if (username != null) {
                return username;
            }
            
            // 尝试从缓存获取
            User user = getCurrentUser();
            if (user != null && user.getUsername() != null) {
                return user.getUsername();
            }
            
            return "匿名用户";
        });
    }
    
    /**
     * 获取当前异步任务中的用户Token
     * @return 用户Token，如果不存在则返回null
     */
    public static String getCurrentToken() {
        return AsyncUserContext.getToken();
    }
    
    /**
     * 检查当前异步任务是否有用户上下文
     * @return 是否有用户上下文
     */
    public static boolean hasUserContext() {
        return AsyncUserContext.hasUserContext();
    }
    
    /**
     * 记录异步任务的用户操作日志
     * @param operation 操作描述
     * @param details 操作详情
     */
    public static void logUserOperation(String operation, String details) {
        // 获取当前用户信息
        String username = getCurrentUsername();
        Integer userId = getCurrentUserId();
        
        // 增加更多可能的上下文来源检查
        User user = getCurrentUser();
        if (user == null) {
            // 尝试从当前请求获取用户信息（适用于HTTP请求触发的异步任务）
            HttpServletRequest request = PoetryUtil.getRequest();
            if (request != null) {
                String token = PoetryUtil.getTokenWithoutBearer();
                if (token != null && !token.equals("null") && staticUserCacheManager != null) {
                    try {
                        user = staticUserCacheManager.getUserByToken(token);
                        if (user != null) {
                            username = user.getUsername();
                            userId = user.getId();
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        
        String userInfo = userId != null ? username + "(ID: " + userId + ")" : username;
        log.info("异步任务用户操作 - 用户: {}, 操作: {}, 详情: {}", userInfo, operation, details);
    }
    
    /**
     * 记录异步任务的用户操作日志（简化版）
     * @param operation 操作描述
     */
    public static void logUserOperation(String operation) {
        logUserOperation(operation, "");
    }
    
    /**
     * 验证当前异步任务是否有有效的用户上下文
     * @param requireUser 是否要求必须有用户上下文
     * @return 验证结果
     */
    public static boolean validateUserContext(boolean requireUser) {
        if (requireUser && !hasUserContext()) {
            log.warn("异步任务要求用户上下文，但当前没有用户信息");
            return false;
        }
        return true;
    }
    
    /**
     * 获取任务执行时间
     * @return 执行时间（毫秒），如果无法获取则返回null
     */
    public static Long getTaskExecutionTime() {
        return AsyncUserContext.getExecutionTime();
    }
    
    /**
     * 记录任务执行时间
     * @param taskName 任务名称
     */
    public static void logTaskExecutionTime(String taskName) {
        Long executionTime = getTaskExecutionTime();
        if (executionTime != null) {
            log.info("异步任务执行时间 - 任务: {}, 用户: {}, 耗时: {}ms", 
                    taskName, getCurrentUsername(), executionTime);
        }
    }
}
