package com.ld.poetry.utils;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * IP地址获取和验证工具类
 * 提供更强大的IP获取、验证、监控功能
 */
@Slf4j
public class IpUtil {
    
    // IPv4地址正则表达式
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"
    );
    
    // IPv6地址正则表达式（简化版）
    private static final Pattern IPV6_PATTERN = Pattern.compile(
        "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::1$|^::$"
    );
    
    // IP获取失败统计
    private static final ConcurrentHashMap<String, AtomicLong> failureStats = new ConcurrentHashMap<>();
    
    // IP获取成功统计
    private static final AtomicLong successCount = new AtomicLong(0);
    
    // IP获取总次数统计
    private static final AtomicLong totalCount = new AtomicLong(0);
    
    /**
     * 获取客户端真实IP地址
     * 支持多种代理环境和容错机制
     */
    public static String getClientRealIp(HttpServletRequest request) {
        if (request == null) {
            recordFailure("request_null");
            return "unknown";
        }
        
        totalCount.incrementAndGet();
        
        return RetryUtil.executeWithRetry(() -> {
            try {
                // 按优先级顺序尝试获取IP地址
                String[] headerNames = {
                    "X-Forwarded-For",
                    "X-Real-IP",
                    "X-Original-Forwarded-For",
                    "Proxy-Client-IP",
                    "WL-Proxy-Client-IP",
                    "HTTP_CLIENT_IP",
                    "HTTP_X_FORWARDED_FOR",
                    "CF-Connecting-IP",  // Cloudflare
                    "True-Client-IP",    // Akamai
                    "X-Cluster-Client-IP" // 集群环境
                };
                
                for (String headerName : headerNames) {
                    String ip = extractValidIpFromHeader(request, headerName);
                    if (ip != null) {
                        successCount.incrementAndGet();
                        return ip;
                    }
                }
                
                // 最后尝试getRemoteAddr()
                String remoteAddr = request.getRemoteAddr();
                if (isValidPublicIp(remoteAddr)) {
                    successCount.incrementAndGet();
                    return remoteAddr;
                }
                
                // 如果是内网IP，但在开发环境可能是有效的
                if (isValidIpFormat(remoteAddr)) {
                    successCount.incrementAndGet();
                    return remoteAddr;
                }
                
                // 记录调试信息
                logDetailedRequestInfo(request);
                recordFailure("no_valid_ip");
                
                return "unknown";
                
            } catch (Exception e) {
                log.error("获取客户端IP时发生异常: {}", e.getMessage(), e);
                recordFailure("exception_" + e.getClass().getSimpleName());
                return "unknown";
            }
        }, 3, 100, "获取客户端IP");
    }
    
    /**
     * 从请求头中提取有效的IP地址
     */
    private static String extractValidIpFromHeader(HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (!isValidHeaderValue(headerValue)) {
            return null;
        }
        
        // 处理多个IP的情况（通常用逗号分隔）
        if (headerValue.contains(",")) {
            String[] ips = headerValue.split(",");
            for (String ip : ips) {
                ip = ip.trim();
                if (isValidPublicIp(ip)) {
                    return ip;
                }
            }
            // 如果没有公网IP，返回第一个有效格式的IP
            for (String ip : ips) {
                ip = ip.trim();
                if (isValidIpFormat(ip)) {
                    return ip;
                }
            }
        } else {
            // 单个IP的情况
            String ip = headerValue.trim();
            if (isValidPublicIp(ip)) {
                return ip;
            }
            if (isValidIpFormat(ip)) {
                return ip;
            }
        }
        
        return null;
    }
    
    /**
     * 验证请求头值是否有效
     */
    private static boolean isValidHeaderValue(String value) {
        return value != null && 
               !value.trim().isEmpty() && 
               !"unknown".equalsIgnoreCase(value.trim()) &&
               !"null".equalsIgnoreCase(value.trim()) &&
               !"undefined".equalsIgnoreCase(value.trim()) &&
               !"-".equals(value.trim());
    }
    
    /**
     * 验证IP格式是否正确
     */
    public static boolean isValidIpFormat(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        ip = ip.trim();
        
        // 检查IPv4格式
        if (IPV4_PATTERN.matcher(ip).matches()) {
            return true;
        }
        
        // 检查IPv6格式（简化检查）
        if (IPV6_PATTERN.matcher(ip).matches()) {
            return true;
        }
        
        // 检查是否为有效的主机名（可以解析为IP）
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
    
    /**
     * 验证是否为有效的公网IP
     */
    public static boolean isValidPublicIp(String ip) {
        if (!isValidIpFormat(ip)) {
            return false;
        }
        
        return !isInternalIp(ip);
    }
    
    /**
     * 判断是否为内网IP
     */
    public static boolean isInternalIp(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return true;
        }
        
        ip = ip.trim();
        
        // 本地回环地址
        if ("127.0.0.1".equals(ip) || "localhost".equals(ip) || 
            "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return true;
        }
        
        // 私有IP地址段
        if (ip.startsWith("192.168.") || 
            ip.startsWith("10.") || 
            (ip.startsWith("172.") && isInRange172(ip)) ||
            ip.startsWith("169.254.")) { // 链路本地地址
            return true;
        }
        
        // Docker默认网络
        if (ip.startsWith("172.17.") || ip.startsWith("172.18.")) {
            return true;
        }
        
        // 其他无效IP
        if ("unknown".equals(ip) || "0.0.0.0".equals(ip) || "255.255.255.255".equals(ip)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查是否在172.16.0.0-172.31.255.255范围内
     */
    private static boolean isInRange172(String ip) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                int second = Integer.parseInt(parts[1]);
                return second >= 16 && second <= 31;
            }
        } catch (NumberFormatException e) {
            // 忽略解析错误
        }
        return false;
    }
    
    /**
     * 记录失败统计
     */
    private static void recordFailure(String reason) {
        failureStats.computeIfAbsent(reason, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * 记录详细的请求信息用于调试
     */
    private static void logDetailedRequestInfo(HttpServletRequest request) {
    }
    
    /**
     * 获取IP获取统计信息
     */
    public static String getIpStatistics() {
        long total = totalCount.get();
        long success = successCount.get();
        double successRate = total > 0 ? (double) success / total * 100 : 0;
        
        StringBuilder stats = new StringBuilder();
        stats.append(String.format("IP获取统计: 总次数=%d, 成功=%d, 成功率=%.2f%%", 
                                  total, success, successRate));
        
        if (!failureStats.isEmpty()) {
            stats.append("; 失败原因: ");
            failureStats.forEach((reason, count) -> 
                stats.append(reason).append("=").append(count.get()).append(" "));
        }
        
        return stats.toString();
    }
    
    /**
     * 重置统计信息
     */
    public static void resetStatistics() {
        totalCount.set(0);
        successCount.set(0);
        failureStats.clear();
        log.info("IP获取统计信息已重置");
    }
    
    /**
     * 验证IP是否在指定的CIDR范围内
     */
    public static boolean isIpInCidr(String ip, String cidr) {
        try {
            String[] cidrParts = cidr.split("/");
            if (cidrParts.length != 2) {
                return false;
            }
            
            InetAddress targetAddr = InetAddress.getByName(ip);
            InetAddress cidrAddr = InetAddress.getByName(cidrParts[0]);
            int prefixLength = Integer.parseInt(cidrParts[1]);
            
            byte[] targetBytes = targetAddr.getAddress();
            byte[] cidrBytes = cidrAddr.getAddress();
            
            if (targetBytes.length != cidrBytes.length) {
                return false;
            }
            
            int bytesToCheck = prefixLength / 8;
            int bitsToCheck = prefixLength % 8;
            
            // 检查完整字节
            for (int i = 0; i < bytesToCheck; i++) {
                if (targetBytes[i] != cidrBytes[i]) {
                    return false;
                }
            }
            
            // 检查剩余位
            if (bitsToCheck > 0 && bytesToCheck < targetBytes.length) {
                int mask = 0xFF << (8 - bitsToCheck);
                return (targetBytes[bytesToCheck] & mask) == (cidrBytes[bytesToCheck] & mask);
            }
            
            return true;
        } catch (Exception e) {
            log.error("CIDR匹配检查失败: ip={}, cidr={}, error={}", ip, cidr, e.getMessage());
            return false;
        }
    }

    /**
     * 规范化IP白名单配置
     * 支持逗号、分号、换行分隔，自动去重并保留顺序
     */
    public static List<String> normalizeIpWhitelist(String whitelistText) {
        if (whitelistText == null || whitelistText.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] rawEntries = whitelistText.split("[,;\\r\\n]+");
        Set<String> normalizedEntries = new LinkedHashSet<>();
        for (String rawEntry : rawEntries) {
            if (rawEntry == null) {
                continue;
            }

            String entry = rawEntry.trim();
            if (!entry.isEmpty()) {
                normalizedEntries.add(entry);
            }
        }

        return new ArrayList<>(normalizedEntries);
    }

    /**
     * 验证IP白名单条目是否合法
     * 支持单个IP和CIDR网段
     */
    public static boolean isValidIpWhitelistEntry(String entry) {
        if (entry == null || entry.trim().isEmpty()) {
            return false;
        }

        String normalizedEntry = entry.trim();
        if (normalizedEntry.contains("/")) {
            return isValidCidr(normalizedEntry);
        }

        return isValidIpLiteral(normalizedEntry);
    }

    /**
     * 判断IP是否命中白名单
     * 白名单为空时表示不限制
     */
    public static boolean isIpAllowedByWhitelist(String ip, String whitelistText) {
        List<String> whitelistEntries = normalizeIpWhitelist(whitelistText);
        if (whitelistEntries.isEmpty()) {
            return true;
        }

        if (!isValidIpFormat(ip)) {
            return false;
        }

        for (String entry : whitelistEntries) {
            if (entry.contains("/")) {
                if (isIpInCidr(ip, entry)) {
                    return true;
                }
            } else if (ip.equals(entry)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isValidCidr(String cidr) {
        try {
            String[] cidrParts = cidr.split("/");
            if (cidrParts.length != 2) {
                return false;
            }

            String baseIp = cidrParts[0].trim();
            String prefixText = cidrParts[1].trim();
            if (!isValidIpLiteral(baseIp)) {
                return false;
            }

            InetAddress inetAddress = InetAddress.getByName(baseIp);
            int maxPrefixLength = inetAddress.getAddress().length * 8;
            int prefixLength = Integer.parseInt(prefixText);
            return prefixLength >= 0 && prefixLength <= maxPrefixLength;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isValidIpLiteral(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }

        String normalizedIp = ip.trim();
        if (normalizedIp.contains(":")) {
            try {
                InetAddress.getByName(normalizedIp);
                return true;
            } catch (UnknownHostException e) {
                return false;
            }
        }

        return IPV4_PATTERN.matcher(normalizedIp).matches();
    }
}
