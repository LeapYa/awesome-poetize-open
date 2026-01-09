package com.ld.poetry.config;

import com.ld.poetry.constants.CacheConstants;
import com.ld.poetry.utils.IpUtil;
import com.ld.poetry.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * 安全过滤器 - 拦截常见的恶意扫描请求并实现IP拉黑
 * 记录恶意IP，达到阈值后自动拉黑一段时间
 */
@Component
@Slf4j
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private RedisUtil redisUtil;

    // 攻击次数阈值 - 超过此次数将被拉黑
    private static final int ATTACK_THRESHOLD = 3;

    // 拉黑时长（小时）
    private static final int BLACKLIST_DURATION_HOURS = 24;

    // 内部服务标识
    private static final Set<String> INTERNAL_SERVICES = Set.of(
        "poetize-python",
        "poetize-java", 
        "poetize-prerender",
        "poetize-nginx"
    );

    // 常见的恶意扫描路径 - 只包含明确的恶意路径，避免误拦截正常请求
    private static final Set<String> MALICIOUS_PATHS = Set.of(
        "/.env",                    // 环境变量文件
        "/.env.local",
        "/.env.production", 
        "/.git",                    // Git版本控制目录
        "/.git/config",
        "/phpmyadmin",              // 数据库管理工具
        "/pma",
        "/wp-admin",                // WordPress管理后台
        "/wp-login.php",
        "/wp-config.php",
        "/config.php",              // PHP配置文件
        "/database.php",
        "/xmlrpc.php",              // WordPress XML-RPC
        "/.aws",                    // AWS配置
        "/.docker",                 // Docker配置
        "/docker-compose.yml",
        "/Dockerfile",
        "/.DS_Store",               // macOS系统文件
        "/admin.php",               // PHP管理页面（而不是/admin）
        "/admin/login.php",
        "/administrator.php",
        "/manager.php",
        "/console.php",
        "/debug.php",
        "/test.php",
        "/info.php",
        "/phpinfo.php",
        "/sql.php",
        "/backup.sql",
        "/database.sql",
        "/.htaccess",               // Apache配置文件
        "/.htpasswd",
        "/web.config",              // IIS配置文件
        "/server.xml",              // Tomcat配置
        "/application.properties",  // Spring配置文件（如果在根目录就是恶意扫描）
        "/application.yml",
        "/build.sh",                // 构建脚本，常被扫描利用
        "/index.html",              // 根目录index.html探测
        "/translation/test-summary", // 翻译模型测试探测
        "/translation/definite_notexist_path", // 翻译模型路径扫描
        "/definite_notexist_path"  // 通用路径探测
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String clientIP = getClientIpAddress(request);
        
        // 检查是否为内部服务请求，如果是则直接放行
        if (isInternalServiceRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 清理过期的拉黑记录和攻击计数
        cleanupExpiredRecords();
        
        // 检查IP是否被拉黑
        if (isIPBlacklisted(clientIP)) {
            log.warn("拒绝已拉黑IP的访问: {} from IP: {}", requestURI, clientIP);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("403 Forbidden - IP Blacklisted");
            return;
        }
        
        boolean isMaliciousRequest = false;
        String attackType = "";
        
        // 检查是否为恶意扫描路径
        if (MALICIOUS_PATHS.contains(requestURI)) {
            isMaliciousRequest = true;
            attackType = "恶意路径扫描";
        }
        // 检查是否包含恶意扫描特征
        else if (containsMaliciousPattern(requestURI)) {
            isMaliciousRequest = true;
            attackType = "可疑请求特征";
        }
        // 检查无参数的敏感API调用
        else if (isInvalidApiCall(request)) {
            isMaliciousRequest = true;
            attackType = "恶意API探测";
        }

        if (isMaliciousRequest) {
            // 统计拦截请求总数 - 使用Redis计数器
            long blocked = redisUtil.incr(CacheConstants.CACHE_PREFIX + "security:blocked:total", 1);

            // 每100次拦截记录一次统计信息
            if (blocked % 100 == 0) {
                log.info("安全过滤器已累计拦截 {} 次恶意请求", blocked);
            }
            
            // 记录攻击并检查是否需要拉黑
            recordAttackAndCheckBlacklist(clientIP, requestURI, attackType);
            
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("404 Not Found");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 检查是否包含恶意模式
     */
    private boolean containsMaliciousPattern(String requestURI) {
        try {
            // URL解码，防止编码绕过
            String decodedURI = URLDecoder.decode(requestURI, StandardCharsets.UTF_8);
            String lowerURI = decodedURI.toLowerCase();
            
            // 路径遍历攻击
            if (decodedURI.contains("..") || decodedURI.contains("./")) {
                return true;
            }
            
            // 敏感文件扩展名
            if (decodedURI.endsWith(".sql") || decodedURI.endsWith(".bak") || 
                decodedURI.endsWith(".backup") || decodedURI.endsWith(".old") || 
                decodedURI.endsWith(".tmp") || decodedURI.endsWith(".log")) {
                return true;
            }
            
            // PHP相关恶意路径（WordPress、PHPMyAdmin等）
            if (lowerURI.contains("/wp-") || lowerURI.contains(".php.") ||
                lowerURI.contains("admin.php") || lowerURI.contains("login.php") ||
                lowerURI.contains("config.php") || lowerURI.contains("phpinfo")) {
                return true;
            }
            
            // 只拦截已知的恶意翻译路径扫描，不拦截合法翻译服务请求
            if (lowerURI.equals("/translation/test-summary") || lowerURI.equals("/translation/definite_notexist_path")) {
                return true;
            }
            
            // 检测明显的恶意探测模式
            if (lowerURI.contains("notexist") || 
                lowerURI.contains("test-") || 
                lowerURI.contains("scanner") || 
                lowerURI.contains("scan") ||
                lowerURI.contains("probe")) {
                return true;
            }
            
            // 检测常见的CMS路径扫描
            Set<String> commonCmsPatterns = Set.of(
                "/wp-", "/wordpress", "/drupal", "/joomla", "/magento",
                "/administrator.php", "/admin/login.php", "/phpmyadmin", "/xmlrpc", 
                "/blog/wp-", "/cms/", "/old/", "/new/", "/backup/", "/bak/", 
                "/beta/", "/temp/", "/dev/"
            );
            
            for (String pattern : commonCmsPatterns) {
                if (lowerURI.contains(pattern)) {
                    return true;
                }
            }
            
            // XSS和脚本注入检测
            if (lowerURI.contains("<script") || lowerURI.contains("javascript:") ||
                lowerURI.contains("eval(") || lowerURI.contains("base64_decode")) {
                return true;
            }
            
            // SQL注入检测（大小写不敏感）
            if (lowerURI.contains("union") && lowerURI.contains("select") ||
                lowerURI.contains("drop") && lowerURI.contains("table") ||
                lowerURI.contains("insert") && lowerURI.contains("into") ||
                lowerURI.contains("delete") && lowerURI.contains("from") ||
                lowerURI.contains("update") && lowerURI.contains("set") ||
                lowerURI.contains("select") && lowerURI.contains("from")) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            // URL解码失败，可能是恶意构造的URL
            log.warn("URL解码失败，可能为恶意请求: {}", requestURI);
            return true;
        }
    }
    
    /**
     * 检查是否为内部服务请求
     */
    private boolean isInternalServiceRequest(HttpServletRequest request) {
        String internalService = request.getHeader("X-Internal-Service");
        
        if (internalService != null) {
            if (INTERNAL_SERVICES.contains(internalService)) {
                return true;
            } else {
                // 记录未知的内部服务标识，可能是恶意伪造
                log.warn("检测到未知的内部服务标识: {}, URI: {}, IP: {}", 
                        internalService, request.getRequestURI(), getClientIpAddress(request));
            }
        }
        
        return false;
    }
    
    /**
     * 获取客户端真实IP地址
     * 使用增强的IP获取工具，提供更好的容错和监控能力
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = IpUtil.getClientRealIp(request);
        
        // 如果获取到的是unknown，在安全过滤器中使用unknown_ip以区分
        if ("unknown".equals(ip)) {
            log.warn("无法获取客户端真实IP地址，请求URI: {}, User-Agent: {}", 
                    request.getRequestURI(), request.getHeader("User-Agent"));
            return "unknown_ip";
        }
        
        return ip;
    }
    
    /**
     * 检查IP是否被拉黑
     */
    private boolean isIPBlacklisted(String ip) {
        String blacklistKey = CacheConstants.buildIpBlacklistKey(ip);
        return redisUtil.hasKey(blacklistKey);
    }
    
    /**
     * 记录攻击并检查是否需要拉黑
     */
    private void recordAttackAndCheckBlacklist(String ip, String requestURI, String attackType) {
        // 对于无法获取IP的情况，采用更严格的策略
        if ("unknown_ip".equals(ip)) {
            log.error("拦截{}攻击: {} from 未知IP (无法获取真实IP地址，可能存在代理配置问题或恶意伪造)",
                     attackType, requestURI);

            // 对unknown_ip立即拉黑，防止绕过检测
            String blacklistKey = CacheConstants.buildIpBlacklistKey(ip);
            redisUtil.set(blacklistKey, LocalDateTime.now().toString(), CacheConstants.IP_BLACKLIST_EXPIRE_TIME);

            log.error("未知IP因恶意攻击被立即拉黑{}小时，拉黑时间: {}",
                     BLACKLIST_DURATION_HOURS,
                     LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            return;
        }

        // 正常IP的处理逻辑 - 使用Redis计数器
        String attackKey = CacheConstants.buildIpAttackKey(ip);
        long currentCount = redisUtil.incr(attackKey, 1);

        // 设置攻击计数的过期时间
        if (currentCount == 1) {
            redisUtil.expire(attackKey, CacheConstants.IP_ATTACK_EXPIRE_TIME);
        }

        log.warn("拦截{}攻击: {} from IP: {} (攻击次数: {})", attackType, requestURI, ip, currentCount);

        // 检查是否达到拉黑阈值
        if (currentCount >= ATTACK_THRESHOLD) {
            String blacklistKey = CacheConstants.buildIpBlacklistKey(ip);
            redisUtil.set(blacklistKey, LocalDateTime.now().toString(), CacheConstants.IP_BLACKLIST_EXPIRE_TIME);

            log.error("IP {} 因连续{}次恶意攻击被拉黑{}小时，拉黑时间: {}",
                     ip, currentCount, BLACKLIST_DURATION_HOURS,
                     LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // 重置攻击计数
            redisUtil.del(attackKey);
        }
    }
    
    /**
     * 清理过期的拉黑记录和攻击计数
     * 注意：Redis会自动处理过期键，这个方法主要用于兼容性
     */
    private void cleanupExpiredRecords() {
        // Redis会自动清理过期的键，无需手动处理
    }
    
    /**
     * 手动解除IP拉黑（管理员功能）
     * 注意：这是静态方法，需要通过Spring上下文获取RedisUtil
     */
    public static boolean unblacklistIP(String ip) {
        // 由于这是静态方法，需要通过其他方式获取RedisUtil实例
        // 建议将此方法改为非静态方法或通过Service层调用
        log.info("管理员手动解除IP拉黑: {} (需要通过Service层实现)", ip);
        return true;
    }
    
    /**
     * 检查是否为无参数的敏感API调用
     * 识别常见的恶意API探测模式，特别是那些缺少必要参数的请求
     */
    private boolean isInvalidApiCall(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        // 1. 忘记密码API无参数调用
        if (requestURI.equals("/user/getCodeForForgetPassword")) {
            // 检查是否缺少必要参数'place'
            String place = request.getParameter("place");
            if (place == null || place.isEmpty()) {
                log.warn("检测到无参数的密码找回API调用: {}, IP: {}", 
                        requestURI, getClientIpAddress(request));
                return true;
            }
        }

        return false;
    }

}