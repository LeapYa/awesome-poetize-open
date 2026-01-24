package com.ld.poetry.service.impl;

import com.ld.poetry.service.CaptchaService;
import com.ld.poetry.service.SysCaptchaConfigService;
import com.ld.poetry.utils.JsonUtils;
import com.ld.poetry.vo.CaptchaProof;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 验证码验证服务实现
 */
@Slf4j
@Service
public class CaptchaServiceImpl implements CaptchaService {
    
    @Autowired
    private SysCaptchaConfigService captchaConfigService;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final String CAPTCHA_TOKEN_PREFIX = "captcha:token:";
    private static final String IP_VERIFY_COUNT_PREFIX = "captcha:ip:count:";
    private static final String IP_BLOCK_PREFIX = "captcha:ip:block:";
    private static final String FINGERPRINT_PREFIX = "captcha:fingerprint:";
    private static final long TOKEN_EXPIRY = 5; // 5分钟过期
    private static final long IP_COUNT_WINDOW = 5; // IP统计窗口：5分钟
    private static final long IP_BLOCK_DURATION = 30; // IP封禁时长：30分钟
    private static final int MAX_VERIFY_PER_IP = 15; // 5分钟内最多验证15次
    private static final int MAX_FINGERPRINT_SWITCHES = 3; // 允许的最大指纹切换次数
    private static final int CAPTCHA_PROOF_VERSION = 1;
    
    @Override
    public boolean isCaptchaRequired(String action) {
        try {
            Map<String, Object> config = captchaConfigService.getCaptchaConfig();
            
            // 检查是否启用验证码
            Boolean enable = (Boolean) config.get("enable");
            if (!Boolean.TRUE.equals(enable)) {
                return false;
            }
            
            // 检查特定操作是否需要验证码
            Object actionValue = config.get(action);
            boolean required = actionValue instanceof Boolean ? (Boolean) actionValue : false;
            
            return required;
        } catch (Exception e) {
            log.error("检查验证码需求失败", e);
            // 出错时默认不需要验证码，确保用户可以正常操作
            return false;
        }
    }
    
    @Override
    public Map<String, Object> verifyCheckboxCaptcha(
            List<Map<String, Object>> mouseTrack,
            Double straightRatio,
            Boolean isReplyComment,
            Integer retryCount,
            Double frontendSensitivity,
            Integer frontendMinPoints,
            Long clickDelay,
            Long thinkingTime,
            Boolean isTouchDevice,
            String browserFingerprint,
            String clientIp,
            String action) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean hasValidTrack = mouseTrack != null && mouseTrack.size() >= 2;
            if (!hasValidTrack) {
                result.put("success", false);
                result.put("token", "");
                result.put("message", "轨迹数据无效");
                return result;
            }

            String clientKey = buildClientKey(clientIp, browserFingerprint);

            // 0. IP频率检查和封禁检查
            if (clientKey != null && !clientKey.isEmpty()) {
                // 检查IP是否被封禁
                if (isIpBlocked(clientKey)) {
                    long remainingMinutes = getIpBlockRemainingTime(clientKey);
                    log.warn("IP已被封禁: {}, 剩余{}分钟", clientIp, remainingMinutes);
                    result.put("success", false);
                    result.put("token", "");
                    result.put("message", String.format("验证失败次数过多，已被临时限制 %d 分钟，请稍后再试", remainingMinutes));
                    result.put("blocked", true);
                    result.put("remainingMinutes", remainingMinutes);
                    return result;
                }
                
                // 获取当前验证次数
                int currentCount = getIpVerifyCount(clientKey);
                int remainingAttempts = MAX_VERIFY_PER_IP - currentCount;
                
                // 检查IP验证频率
                if (!checkIpRateLimit(clientKey)) {
                    log.warn("IP验证频率过高: {}, 已达{}次", clientIp, currentCount);
                    blockIp(clientKey);
                    result.put("success", false);
                    result.put("token", "");
                    result.put("message", String.format("验证次数过多（%d次/%d分钟），已被临时限制 %d 分钟", 
                            MAX_VERIFY_PER_IP, (int)IP_COUNT_WINDOW, (int)IP_BLOCK_DURATION));
                    result.put("blocked", true);
                    result.put("remainingMinutes", IP_BLOCK_DURATION);
                    return result;
                }
                
                // 如果剩余次数较少，添加警告信息
                if (remainingAttempts <= 3 && remainingAttempts > 0) {
                    result.put("warning", String.format("提示：您还剩 %d 次验证机会（%d分钟内）", 
                            remainingAttempts, (int)IP_COUNT_WINDOW));
                }
            }
            
            // 获取验证码配置
            Map<String, Object> config = captchaConfigService.getCaptchaConfig();
            Map<String, Object> checkboxConfig = (Map<String, Object>) config.getOrDefault("checkbox", new HashMap<>());
            
            boolean isValid = true;
            List<String> validationDetails = new ArrayList<>();
            
            // 从轨迹时间戳独立计算操作时间
            long serverCalculatedOperationTime = 0;
            if (mouseTrack != null && mouseTrack.size() >= 2) {
                long firstTimestamp = getLongFromMap(mouseTrack.get(0), "timestamp", 0L);
                long lastTimestamp = getLongFromMap(mouseTrack.get(mouseTrack.size() - 1), "timestamp", 0L);
                serverCalculatedOperationTime = lastTimestamp - firstTimestamp;
            }
            
            // ========== 蜜罐检测：对比前端传值与后端计算值 ==========
            // 正常用户：前端值和后端计算值应该一致或接近
            // 攻击者：可能伪造前端值，导致数值不匹配
            int honeypotScore = 0; // 蜜罐可疑分数
            
            // 检测1：直线率蜜罐
            if (mouseTrack != null && mouseTrack.size() >= 3 && straightRatio != null) {
                double serverStraightRatio = calculateStraightRatio(mouseTrack);
                double ratioDiff = Math.abs(serverStraightRatio - straightRatio);
                if (ratioDiff > 0.1) {
                    honeypotScore += 30;
                    validationDetails.add(String.format("蜜罐触发:直线率不匹配(前端%.3f,后端%.3f)", straightRatio, serverStraightRatio));
                }
            }
            
            // 检测2：操作时间蜜罐
            if (clickDelay != null && serverCalculatedOperationTime > 0) {
                long timeDiff = Math.abs(clickDelay - serverCalculatedOperationTime);
                int trackPointCountForHoneypot = mouseTrack != null ? mouseTrack.size() : 0;
                long allowedDiff = 120;
                long percentAllowed = (long) Math.ceil(serverCalculatedOperationTime * 0.15);
                allowedDiff = Math.max(allowedDiff, percentAllowed);
                if (trackPointCountForHoneypot > 0 && trackPointCountForHoneypot <= 3) {
                    allowedDiff = Math.max(allowedDiff, 250);
                }
                allowedDiff = Math.min(800, allowedDiff);

                if (timeDiff > allowedDiff) {
                    honeypotScore += 25;
                    validationDetails.add(String.format("蜜罐触发:操作时间不匹配(前端%dms,后端%dms)", clickDelay, serverCalculatedOperationTime));
                }
            }
            
            // 检测3：触屏设备声明但有大量鼠标轨迹
            if (Boolean.TRUE.equals(isTouchDevice) && mouseTrack != null && mouseTrack.size() > 10) {
                honeypotScore += 20;
                validationDetails.add("蜜罐触发:声称触屏但轨迹过多");
            }
            
            // 检测4：thinkingTime异常（声称思考了很久但轨迹时间戳不支持）
            if (thinkingTime != null && thinkingTime > 5000 && mouseTrack != null && mouseTrack.size() >= 2) {
                long firstTimestamp = getLongFromMap(mouseTrack.get(0), "timestamp", 0L);
                long lastTimestamp = getLongFromMap(mouseTrack.get(mouseTrack.size() - 1), "timestamp", 0L);
                // 如果声称思考了5秒以上，但轨迹只有几百毫秒，可疑
                if (lastTimestamp - firstTimestamp < 1000 && mouseTrack.size() > 5) {
                    honeypotScore += 25;
                    validationDetails.add("蜜罐触发:思考时间与轨迹时间不符");
                }
            }
            
            // 蜜罐分数过高直接判定为机器人
            if (honeypotScore >= 50) {
                isValid = false;
                log.warn("蜜罐检测触发! IP: {}, 分数: {}, 详情: {}", clientIp, honeypotScore, validationDetails);
            }
            // ========== 蜜罐检测结束 ==========
            
            // 1. 轨迹数据合理性检查
            int trackPointCount = mouseTrack != null ? mouseTrack.size() : 0;
            
            // 通过轨迹特征推断设备类型和行为
            boolean likelyTouchDevice = false;
            boolean likelyHumanBehavior = false;
            
            if (trackPointCount == 0) {
                if (Boolean.TRUE.equals(isReplyComment)) {
                    likelyHumanBehavior = true;
                    validationDetails.add("回复评论场景，无轨迹但允许通过");
                } else {
                    isValid = false;
                    validationDetails.add("无鼠标轨迹数据");
                }
            } else if (trackPointCount >= 1 && trackPointCount <= 3) {
                if (serverCalculatedOperationTime >= 50 && serverCalculatedOperationTime <= 500) {
                    likelyTouchDevice = true;
                    likelyHumanBehavior = true;
                    validationDetails.add(String.format("少量轨迹点(%d个)，操作时间%dms", 
                        trackPointCount, serverCalculatedOperationTime));
                } else if (serverCalculatedOperationTime > 500) {
                    likelyHumanBehavior = true;
                    validationDetails.add(String.format("少量轨迹点但操作时间较长(%dms)", serverCalculatedOperationTime));
                }
            } else {
                likelyHumanBehavior = true;
            }
            
            // 2. 操作时间检查（使用服务端计算的时间）
            if (trackPointCount >= 2) {
                int minOperationTime = Boolean.TRUE.equals(isReplyComment) ? 30 : 100;
                // 重试时降低要求
                if (retryCount != null && retryCount > 0) {
                    minOperationTime = Math.max(20, minOperationTime - retryCount * 20);
                }
                
                if (serverCalculatedOperationTime < minOperationTime) {
                    // 操作时间过短，但如果轨迹特征正常也可以通过
                    validationDetails.add(String.format("操作时间较短: %dms", serverCalculatedOperationTime));
                } else if (serverCalculatedOperationTime > 60000) {
                    isValid = false;
                    validationDetails.add(String.format("操作时间过长: %dms > 60000ms (页面可能失效)", serverCalculatedOperationTime));
                }
            }
            
            // 3. 轨迹点数量检查
            int minTrackPoints = getIntValue(checkboxConfig, "minTrackPoints", 3);
            
            if (Boolean.TRUE.equals(isReplyComment) || likelyTouchDevice) {
                minTrackPoints = 0;
            }
            
            if (trackPointCount < minTrackPoints) {
                isValid = false;
                validationDetails.add(String.format("轨迹点数不足: %d < %d", trackPointCount, minTrackPoints));
            }
            
            // 4. 直线率检查（后端独立计算）
            if (!Boolean.TRUE.equals(isReplyComment) && mouseTrack != null && mouseTrack.size() >= 3) {
                double trackSensitivity = getDoubleValue(checkboxConfig, "trackSensitivity", 0.98);
                double serverCalculatedStraightRatio = calculateStraightRatio(mouseTrack);
                
                if (serverCalculatedStraightRatio > trackSensitivity) {
                    isValid = false;
                    validationDetails.add(String.format("轨迹过于直线: %.3f > %.3f", 
                        serverCalculatedStraightRatio, trackSensitivity));
                }
            }
            
            // 5. 浏览器指纹检测
            if (browserFingerprint != null && !browserFingerprint.isEmpty() && clientIp != null) {
                // 检查该IP是否频繁切换浏览器指纹（可疑行为）
                if (!checkFingerprintConsistency(clientIp, browserFingerprint)) {
                    isValid = false;
                    validationDetails.add("检测到频繁切换设备/浏览器（疑似自动化）");
                }
            }
            
            // 6. 轨迹特征分析
            if (!Boolean.TRUE.equals(isReplyComment) && mouseTrack != null && mouseTrack.size() >= 2) {
                double avgSpeed = calculateAverageSpeed(mouseTrack);
                double speedVariance = calculateSpeedVariance(mouseTrack, avgSpeed);
                
                // 速度过于恒定 = 疑似脚本
                if (speedVariance < 0.1 && avgSpeed > 100) {
                    isValid = false;
                    validationDetails.add(String.format("速度过于恒定: variance=%.3f", speedVariance));
                }
                
                // 方向变化过少 = 疑似直线移动
                int directionChanges = calculateDirectionChanges(mouseTrack);
                if (directionChanges < 2 && mouseTrack.size() > 5) {
                    isValid = false;
                    validationDetails.add(String.format("方向变化过少: %d", directionChanges));
                }
            }
            
            log.info("验证结果: {}, IP: {}, 详情: {}", isValid, clientIp, validationDetails);
            
            // 生成验证令牌
            String token = "";
            if (isValid) {
                // 存储令牌到Redis，5分钟过期
                try {
                    String tokenValue = buildCaptchaProofValue(action, clientIp, browserFingerprint);
                    if (tokenValue == null || tokenValue.isEmpty()) {
                        isValid = false;
                    } else {
                        token = generateVerificationToken();
                    redisTemplate.opsForValue().set(
                        CAPTCHA_TOKEN_PREFIX + token, 
                        tokenValue,
                        TOKEN_EXPIRY, 
                        TimeUnit.MINUTES
                    );
                    }
                } catch (Exception e) {
                    log.error("存储验证令牌到Redis失败", e);
                    isValid = false;
                }
            }
            
            result.put("success", isValid);
            result.put("token", token);
            
        } catch (Exception e) {
            log.error("复选框验证出错", e);
            result.put("success", false);
            result.put("token", "");
        }
        
        return result;
    }
    
    @Override
    public boolean verifyToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        try {
            String key = CAPTCHA_TOKEN_PREFIX + token;
            String value = redisTemplate.opsForValue().get(key);
            
            if (value != null) {
                // 验证后删除令牌（一次性）
                redisTemplate.delete(key);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("验证令牌失败", e);
            return false;
        }
    }

    @Override
    public boolean verifyToken(String action, String token, String clientIp, String browserFingerprint) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            String key = CAPTCHA_TOKEN_PREFIX + token;
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return false;
            }

            CaptchaProof proof = parseCaptchaProof(value);
            if (proof == null) {
                redisTemplate.delete(key);
                return false;
            }

            String normalizedAction = normalizeAction(action);
            String normalizedProofAction = normalizeAction(proof.getAction());
            if (normalizedAction != null && !normalizedAction.isEmpty()
                && normalizedProofAction != null && !normalizedProofAction.isEmpty()
                && !normalizedAction.equals(normalizedProofAction)) {
                redisTemplate.delete(key);
                return false;
            }

            if (browserFingerprint != null && !browserFingerprint.isEmpty()
                && proof.getFingerprintHash() != null && !proof.getFingerprintHash().isEmpty()) {
                String fingerprintHash = sha256Hex(browserFingerprint);
                if (fingerprintHash != null && !fingerprintHash.equals(proof.getFingerprintHash())) {
                    redisTemplate.delete(key);
                    return false;
                }
            }

            if (clientIp != null && !clientIp.isEmpty()
                && proof.getIpHash() != null && !proof.getIpHash().isEmpty()) {
                String ipHash = sha256Hex(clientIp);
                if (ipHash != null && !ipHash.equals(proof.getIpHash())) {
                    redisTemplate.delete(key);
                    return false;
                }
            }

            redisTemplate.delete(key);
            return true;
        } catch (Exception e) {
            log.error("验证令牌失败", e);
            return false;
        }
    }
    
    @Override
    public Map<String, Object> verifySlideCaptcha(
            List<Map<String, Object>> slideTrack,
            Long totalTime,
            Double maxDistance,
            Double finalPosition,
            String browserFingerprint,
            String clientIp,
            Double avgSpeed,
            Integer backtrackCount,
            Integer trackPointCount,
            String action) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean hasValidTrack = slideTrack != null && slideTrack.size() >= 2
                && totalTime != null && maxDistance != null && finalPosition != null;
            if (!hasValidTrack) {
                result.put("success", false);
                result.put("message", "轨迹数据无效");
                return result;
            }

            String clientKey = buildClientKey(clientIp, browserFingerprint);

            // 0. IP频率检查
            if (clientKey != null && !clientKey.isEmpty()) {
                // 检查IP是否被封禁
                if (isIpBlocked(clientKey)) {
                    long remainingMinutes = getIpBlockRemainingTime(clientKey);
                    log.warn("IP已被封禁: {}, 剩余{}分钟", clientIp, remainingMinutes);
                    result.put("success", false);
                    result.put("message", String.format("验证失败次数过多，已被临时限制 %d 分钟，请稍后再试", remainingMinutes));
                    result.put("blocked", true);
                    result.put("remainingMinutes", remainingMinutes);
                    return result;
                }
                
                // 获取当前验证次数
                int currentCount = getIpVerifyCount(clientKey);
                int remainingAttempts = MAX_VERIFY_PER_IP - currentCount;
                
                // 检查IP验证频率
                if (!checkIpRateLimit(clientKey)) {
                    log.warn("IP滑动验证频率过高: {}, 已达{}次", clientIp, currentCount);
                    blockIp(clientKey);
                    result.put("success", false);
                    result.put("message", String.format("验证次数过多（%d次/%d分钟），已被临时限制 %d 分钟", 
                            MAX_VERIFY_PER_IP, (int)IP_COUNT_WINDOW, (int)IP_BLOCK_DURATION));
                    result.put("blocked", true);
                    result.put("remainingMinutes", IP_BLOCK_DURATION);
                    return result;
                }
                
                // 如果剩余次数较少，添加警告信息
                if (remainingAttempts <= 3 && remainingAttempts > 0) {
                    result.put("warning", String.format("提示：您还剩 %d 次验证机会（%d分钟内）", 
                            remainingAttempts, (int)IP_COUNT_WINDOW));
                }
            }
            
            boolean isValid = true;
            List<String> validationDetails = new ArrayList<>();
            
            // 从轨迹时间戳独立计算总时间
            long serverCalculatedTotalTime = 0;
            if (slideTrack != null && slideTrack.size() >= 2) {
                long firstTimestamp = getLongFromMap(slideTrack.get(0), "timestamp", 0L);
                long lastTimestamp = getLongFromMap(slideTrack.get(slideTrack.size() - 1), "timestamp", 0L);
                serverCalculatedTotalTime = lastTimestamp - firstTimestamp;
            }
            
            // ========== 蜜罐检测 ==========
            int honeypotScore = 0;
            
            // 检测1：总时间蜜罐
            if (totalTime != null && serverCalculatedTotalTime > 0) {
                long timeDiff = Math.abs(totalTime - serverCalculatedTotalTime);
                if (timeDiff > 200) {
                    honeypotScore += 40;
                    validationDetails.add(String.format("蜜罐:时间不匹配(前端%dms,后端%dms)", totalTime, serverCalculatedTotalTime));
                }
            }
            
            // 检测2：最终位置蜜罐
            if (finalPosition != null && slideTrack != null && !slideTrack.isEmpty()) {
                double lastTrackX = getDoubleFromMap(slideTrack.get(slideTrack.size() - 1), "x", 0.0);
                double posDiff = Math.abs(finalPosition - lastTrackX);
                if (posDiff > 5) {
                    honeypotScore += 30;
                    validationDetails.add(String.format("蜜罐:位置不匹配(前端%.1f,轨迹%.1f)", finalPosition, lastTrackX));
                }
            }
            
            // 检测3：声称滑动很慢但轨迹点很少
            if (totalTime != null && totalTime > 3000 && slideTrack != null && slideTrack.size() < 10) {
                honeypotScore += 25;
                validationDetails.add("蜜罐:时间长但轨迹点少");
            }
            
            // 检测4：轨迹点数量蜜罐
            int serverTrackCount = slideTrack != null ? slideTrack.size() : 0;
            if (trackPointCount != null && Math.abs(trackPointCount - serverTrackCount) > 0) {
                honeypotScore += 35;
                validationDetails.add(String.format("蜜罐:轨迹数不匹配(前端%d,后端%d)", trackPointCount, serverTrackCount));
            }
            
            // 检测5：平均速度蜜罐
            if (avgSpeed != null && slideTrack != null && slideTrack.size() >= 2) {
                double serverAvgSpeed = calculateSlideAverageSpeed(slideTrack);
                double speedDiff = Math.abs(avgSpeed - serverAvgSpeed);
                if (speedDiff > 0.5) {
                    honeypotScore += 30;
                    validationDetails.add(String.format("蜜罐:速度不匹配(前端%.3f,后端%.3f)", avgSpeed, serverAvgSpeed));
                }
            }
            
            // 检测6：回退次数蜜罐
            if (backtrackCount != null && slideTrack != null && slideTrack.size() >= 2) {
                int serverBacktrackCount = calculateBacktrackCount(slideTrack);
                if (Math.abs(backtrackCount - serverBacktrackCount) > 0) {
                    honeypotScore += 25;
                    validationDetails.add(String.format("蜜罐:回退数不匹配(前端%d,后端%d)", backtrackCount, serverBacktrackCount));
                }
            }
            
            if (honeypotScore >= 40) {
                isValid = false;
                log.warn("滑动蜜罐触发! IP: {}, 分数: {}, 详情: {}", clientIp, honeypotScore, validationDetails);
            }
            // ========== 蜜罐检测结束 ==========
            
            // 1. 时间检测（使用后端计算的时间）
            if (serverCalculatedTotalTime > 0) {
                if (serverCalculatedTotalTime < 300) {
                    isValid = false;
                    validationDetails.add(String.format("滑动过快: %dms", serverCalculatedTotalTime));
                } else if (serverCalculatedTotalTime > 10000) {
                    isValid = false;
                    validationDetails.add(String.format("滑动过慢: %dms", serverCalculatedTotalTime));
                }
            } else if (totalTime != null) {
                // 没有轨迹时间戳时才用前端的值（降级处理）
                if (totalTime < 500) {
                    isValid = false;
                    validationDetails.add(String.format("滑动过快: %dms", totalTime));
                } else if (totalTime > 10000) {
                    isValid = false;
                    validationDetails.add(String.format("滑动过慢: %dms", totalTime));
                }
            }
            
            // 2. 浏览器指纹检测
            if (browserFingerprint != null && !browserFingerprint.isEmpty() && clientIp != null) {
                if (!checkFingerprintConsistency(clientIp, browserFingerprint)) {
                    isValid = false;
                    validationDetails.add("检测到频繁切换设备/浏览器");
                }
            }
            
            // 3. 轨迹特征分析
            if (slideTrack != null && slideTrack.size() >= 2) {
                // 计算平均速度
                double computedAvgSpeed = calculateSlideAverageSpeed(slideTrack);
                
                // 计算速度方差
                double speedVariance = calculateSlideSpeedVariance(slideTrack, computedAvgSpeed);
                
                // 检测恒速滑动（机器人特征）
                if (speedVariance < 0.05 && computedAvgSpeed > 0.5) {
                    isValid = false;
                    validationDetails.add(String.format("速度过于恒定: variance=%.3f (疑似脚本)", speedVariance));
                }
                
                // 检测回退次数（人类会有微调）
                int computedBacktrackCount = calculateBacktrackCount(slideTrack);
                
                // 检测加速度变化
                double avgAcceleration = calculateAverageAcceleration(slideTrack);
                
                
                // 完全匀速且无回退 = 可疑
                if (speedVariance < 0.1 && computedBacktrackCount == 0 && slideTrack.size() > 5) {
                    isValid = false;
                    validationDetails.add("滑动轨迹过于完美（疑似程序控制）");
                }
            }
            
            log.info("滑动验证结果: {}, IP: {}, 耗时: {}ms, 详情: {}", 
                isValid, clientIp, totalTime, validationDetails);
            
            // 生成令牌
            String token = "";
            if (isValid) {
                try {
                    String tokenValue = buildCaptchaProofValue(action, clientIp, browserFingerprint);
                    if (tokenValue == null || tokenValue.isEmpty()) {
                        isValid = false;
                    } else {
                        token = generateVerificationToken();
                        redisTemplate.opsForValue().set(
                            CAPTCHA_TOKEN_PREFIX + token,
                            tokenValue,
                            TOKEN_EXPIRY,
                            TimeUnit.MINUTES
                        );
                    }
                } catch (Exception e) {
                    log.error("存储滑动验证令牌失败", e);
                    isValid = false;
                }
            }
            
            result.put("success", isValid);
            result.put("token", token);
            if (!isValid && !validationDetails.isEmpty()) {
                result.put("message", validationDetails.get(0));
            }
            
        } catch (Exception e) {
            log.error("滑动验证异常", e);
            result.put("success", false);
            result.put("message", "验证失败");
        }
        
        return result;
    }

    private String buildCaptchaProofValue(String action, String clientIp, String browserFingerprint) {
        CaptchaProof proof = new CaptchaProof();
        proof.setVersion(CAPTCHA_PROOF_VERSION);
        proof.setAction(normalizeAction(action));
        proof.setIssuedAt(System.currentTimeMillis());
        proof.setFingerprintHash(sha256Hex(browserFingerprint));
        proof.setIpHash(sha256Hex(clientIp));
        return JsonUtils.toJsonString(proof);
    }

    private CaptchaProof parseCaptchaProof(String value) {
        if (value == null || value.isEmpty() || "1".equals(value)) {
            return null;
        }
        try {
            CaptchaProof proof = JsonUtils.parseObject(value, CaptchaProof.class);
            if (proof == null || proof.getVersion() == null) {
                return null;
            }
            return proof;
        } catch (Exception e) {
            return null;
        }
    }

    private String sha256Hex(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeAction(String action) {
        if (action == null) {
            return null;
        }
        String trimmed = action.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if ("regist".equalsIgnoreCase(trimmed) || "register".equalsIgnoreCase(trimmed)) {
            return "register";
        }
        if ("thirdPartyLogin".equalsIgnoreCase(trimmed)) {
            return "login";
        }
        if ("login".equalsIgnoreCase(trimmed)) {
            return "login";
        }
        if ("comment".equalsIgnoreCase(trimmed)) {
            return "comment";
        }
        if ("reset_password".equalsIgnoreCase(trimmed)) {
            return "reset_password";
        }
        return trimmed;
    }

    private String buildClientKey(String clientIp, String browserFingerprint) {
        if (clientIp == null || clientIp.trim().isEmpty()) {
            return null;
        }
        String ip = clientIp.trim();
        if (browserFingerprint == null || browserFingerprint.trim().isEmpty()) {
            return ip;
        }
        String fingerprintHash = sha256Hex(browserFingerprint.trim());
        if (fingerprintHash == null || fingerprintHash.isEmpty()) {
            return ip;
        }
        String tag = fingerprintHash.substring(0, Math.min(12, fingerprintHash.length()));
        return ip + ":" + tag;
    }
    
    @Override
    public Map<String, Object> getPublicCaptchaConfig() {
        try {
            Map<String, Object> config = captchaConfigService.getCaptchaConfig();
            
            // 只返回前端验证组件必要的配置信息
            Map<String, Object> publicConfig = new HashMap<>();
            publicConfig.put("enable", config.get("enable"));
            publicConfig.put("screenSizeThreshold", config.get("screenSizeThreshold"));
            publicConfig.put("forceSlideForMobile", config.get("forceSlideForMobile"));
            publicConfig.put("slide", config.get("slide"));
            publicConfig.put("checkbox", config.get("checkbox"));
            
            return publicConfig;
        } catch (Exception e) {
            log.error("获取公共验证码配置失败", e);
            
            // 返回默认配置
            Map<String, Object> defaultConfig = new HashMap<>();
            defaultConfig.put("enable", false);
            defaultConfig.put("screenSizeThreshold", 768);
            defaultConfig.put("forceSlideForMobile", true);
            
            Map<String, Object> slide = new HashMap<>();
            slide.put("accuracy", 5);
            slide.put("successThreshold", 0.95);
            defaultConfig.put("slide", slide);
            
            Map<String, Object> checkbox = new HashMap<>();
            checkbox.put("trackSensitivity", 0.90);
            checkbox.put("minTrackPoints", 2);
            checkbox.put("replyCommentSensitivity", 0.85);
            checkbox.put("maxRetryCount", 5);
            checkbox.put("retryDecrement", 0.02);
            defaultConfig.put("checkbox", checkbox);
            
            return defaultConfig;
        }
    }
    
    @Override
    public String generateVerificationToken() {
        try {
            String uniqueStr = UUID.randomUUID().toString() + "-" + System.currentTimeMillis() + "-" + 
                new Random().nextInt(100000);
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(uniqueStr.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            log.error("生成验证令牌失败", e);
            return UUID.randomUUID().toString();
        }
    }
    
    /**
     * 检查IP是否被封禁
     */
    private boolean isIpBlocked(String ip) {
        try {
            String key = IP_BLOCK_PREFIX + ip;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查IP封禁状态失败", e);
            return false;
        }
    }
    
    /**
     * 获取IP封禁剩余时间（分钟）
     */
    private long getIpBlockRemainingTime(String ip) {
        try {
            String key = IP_BLOCK_PREFIX + ip;
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MINUTES);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception e) {
            log.error("获取IP封禁剩余时间失败", e);
            return 0;
        }
    }
    
    /**
     * 获取IP当前验证次数
     */
    private int getIpVerifyCount(String ip) {
        try {
            String key = IP_VERIFY_COUNT_PREFIX + ip;
            String countStr = redisTemplate.opsForValue().get(key);
            return countStr != null ? Integer.parseInt(countStr) : 0;
        } catch (Exception e) {
            log.error("获取IP验证次数失败", e);
            return 0;
        }
    }
    
    /**
     * 检查IP验证频率限制
     * @return true表示在限制内，false表示超出限制
     */
    private boolean checkIpRateLimit(String ip) {
        try {
            String key = IP_VERIFY_COUNT_PREFIX + ip;
            String countStr = redisTemplate.opsForValue().get(key);
            
            int count = countStr != null ? Integer.parseInt(countStr) : 0;
            
            if (count >= MAX_VERIFY_PER_IP) {
                return false;
            }
            
            // 增加计数
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, IP_COUNT_WINDOW, TimeUnit.MINUTES);
            
            return true;
        } catch (Exception e) {
            log.error("检查IP频率失败", e);
            return true; // 出错时允许通过
        }
    }
    
    /**
     * 封禁IP
     */
    private void blockIp(String ip) {
        try {
            String key = IP_BLOCK_PREFIX + ip;
            redisTemplate.opsForValue().set(key, "1", IP_BLOCK_DURATION, TimeUnit.MINUTES);
            log.warn("IP已被封禁{}分钟: {}", IP_BLOCK_DURATION, ip);
        } catch (Exception e) {
            log.error("封禁IP失败", e);
        }
    }
    
    /**
     * 检查浏览器指纹一致性
     * 检测同一IP是否频繁切换浏览器指纹（疑似自动化攻击）
     * @param ip 客户端IP
     * @param fingerprint 当前浏览器指纹
     * @return true表示正常，false表示可疑
     */
    private boolean checkFingerprintConsistency(String ip, String fingerprint) {
        try {
            String key = FINGERPRINT_PREFIX + ip;
            
            // 获取该IP历史使用的指纹列表（用Set存储，JSON序列化）
            String fingerprintsJson = redisTemplate.opsForValue().get(key);
            
            Set<String> fingerprints = new HashSet<>();
            if (fingerprintsJson != null && !fingerprintsJson.isEmpty()) {
                // 简单解析：用逗号分隔
                String[] arr = fingerprintsJson.split(",");
                fingerprints.addAll(Arrays.asList(arr));
            }
            
            // 添加当前指纹
            fingerprints.add(fingerprint);
            
            // 检查是否超过允许的切换次数
            if (fingerprints.size() > MAX_FINGERPRINT_SWITCHES) {
                log.warn("IP {} 使用了过多不同的浏览器指纹: {} 个", ip, fingerprints.size());
                return false;
            }
            
            // 更新指纹列表，保存24小时
            String newFingerprintsJson = String.join(",", fingerprints);
            redisTemplate.opsForValue().set(key, newFingerprintsJson, 24, TimeUnit.HOURS);
            
            return true;
        } catch (Exception e) {
            log.error("检查指纹一致性失败", e);
            return true; // 出错时允许通过
        }
    }
    
    /**
     * 计算鼠标轨迹直线率（后端独立计算，不信任前端）
     * 直线率 = 直线距离 / 实际路径长度
     * 越接近1表示越直，机器人的轨迹通常非常直
     */
    private double calculateStraightRatio(List<Map<String, Object>> mouseTrack) {
        if (mouseTrack == null || mouseTrack.size() < 3) {
            return 1.0; // 点数不足，返回1（最直）
        }
        
        Map<String, Object> firstPoint = mouseTrack.get(0);
        Map<String, Object> lastPoint = mouseTrack.get(mouseTrack.size() - 1);
        
        double x1 = getDoubleFromMap(firstPoint, "x", 0.0);
        double y1 = getDoubleFromMap(firstPoint, "y", 0.0);
        double x2 = getDoubleFromMap(lastPoint, "x", 0.0);
        double y2 = getDoubleFromMap(lastPoint, "y", 0.0);
        
        // 计算直线距离
        double directDistance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        
        // 计算实际路径长度
        double pathDistance = 0.0;
        for (int i = 1; i < mouseTrack.size(); i++) {
            Map<String, Object> prev = mouseTrack.get(i - 1);
            Map<String, Object> curr = mouseTrack.get(i);
            
            double px = getDoubleFromMap(prev, "x", 0.0);
            double py = getDoubleFromMap(prev, "y", 0.0);
            double cx = getDoubleFromMap(curr, "x", 0.0);
            double cy = getDoubleFromMap(curr, "y", 0.0);
            
            pathDistance += Math.sqrt(Math.pow(cx - px, 2) + Math.pow(cy - py, 2));
        }
        
        // 计算直线率（越接近1越直）
        return pathDistance > 0 ? directDistance / pathDistance : 1.0;
    }
    
    /**
     * 计算平均速度
     */
    private double calculateAverageSpeed(List<Map<String, Object>> mouseTrack) {
        if (mouseTrack == null || mouseTrack.size() < 2) {
            return 0.0;
        }
        
        double totalSpeed = 0.0;
        int validSegments = 0;
        
        for (int i = 1; i < mouseTrack.size(); i++) {
            Map<String, Object> prev = mouseTrack.get(i - 1);
            Map<String, Object> curr = mouseTrack.get(i);
            
            double x1 = getDoubleFromMap(prev, "x", 0.0);
            double y1 = getDoubleFromMap(prev, "y", 0.0);
            long t1 = getLongFromMap(prev, "timestamp", 0L);
            
            double x2 = getDoubleFromMap(curr, "x", 0.0);
            double y2 = getDoubleFromMap(curr, "y", 0.0);
            long t2 = getLongFromMap(curr, "timestamp", 0L);
            
            double distance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
            long timeDiff = t2 - t1;
            
            if (timeDiff > 0) {
                double speed = distance / timeDiff; // px/ms
                totalSpeed += speed;
                validSegments++;
            }
        }
        
        return validSegments > 0 ? totalSpeed / validSegments : 0.0;
    }
    
    /**
     * 计算速度方差
     */
    private double calculateSpeedVariance(List<Map<String, Object>> mouseTrack, double avgSpeed) {
        if (mouseTrack == null || mouseTrack.size() < 2) {
            return 0.0;
        }
        
        double sumSquaredDiff = 0.0;
        int validSegments = 0;
        
        for (int i = 1; i < mouseTrack.size(); i++) {
            Map<String, Object> prev = mouseTrack.get(i - 1);
            Map<String, Object> curr = mouseTrack.get(i);
            
            double x1 = getDoubleFromMap(prev, "x", 0.0);
            double y1 = getDoubleFromMap(prev, "y", 0.0);
            long t1 = getLongFromMap(prev, "timestamp", 0L);
            
            double x2 = getDoubleFromMap(curr, "x", 0.0);
            double y2 = getDoubleFromMap(curr, "y", 0.0);
            long t2 = getLongFromMap(curr, "timestamp", 0L);
            
            double distance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
            long timeDiff = t2 - t1;
            
            if (timeDiff > 0) {
                double speed = distance / timeDiff;
                sumSquaredDiff += Math.pow(speed - avgSpeed, 2);
                validSegments++;
            }
        }
        
        return validSegments > 0 ? Math.sqrt(sumSquaredDiff / validSegments) : 0.0;
    }
    
    /**
     * 计算方向变化次数
     */
    private int calculateDirectionChanges(List<Map<String, Object>> mouseTrack) {
        if (mouseTrack == null || mouseTrack.size() < 3) {
            return 0;
        }
        
        int changes = 0;
        double prevAngle = 0.0;
        
        for (int i = 2; i < mouseTrack.size(); i++) {
            Map<String, Object> p1 = mouseTrack.get(i - 2);
            Map<String, Object> p2 = mouseTrack.get(i - 1);
            Map<String, Object> p3 = mouseTrack.get(i);
            
            double x1 = getDoubleFromMap(p1, "x", 0.0);
            double y1 = getDoubleFromMap(p1, "y", 0.0);
            double x2 = getDoubleFromMap(p2, "x", 0.0);
            double y2 = getDoubleFromMap(p2, "y", 0.0);
            double x3 = getDoubleFromMap(p3, "x", 0.0);
            double y3 = getDoubleFromMap(p3, "y", 0.0);
            
            // 计算两段的角度
            double angle1 = Math.atan2(y2 - y1, x2 - x1);
            double angle2 = Math.atan2(y3 - y2, x3 - x2);
            
            // 角度差异
            double angleDiff = Math.abs(angle2 - angle1);
            
            // 归一化到0-π
            if (angleDiff > Math.PI) {
                angleDiff = 2 * Math.PI - angleDiff;
            }
            
            // 角度变化超过30度认为是方向改变
            if (angleDiff > Math.PI / 6) {
                changes++;
            }
        }
        
        return changes;
    }
    
    /**
     * 计算滑动平均速度
     */
    private double calculateSlideAverageSpeed(List<Map<String, Object>> slideTrack) {
        if (slideTrack == null || slideTrack.size() < 2) {
            return 0.0;
        }
        
        double totalSpeed = 0.0;
        int validSegments = 0;
        
        for (int i = 1; i < slideTrack.size(); i++) {
            Map<String, Object> prev = slideTrack.get(i - 1);
            Map<String, Object> curr = slideTrack.get(i);
            
            double x1 = getDoubleFromMap(prev, "x", 0.0);
            double x2 = getDoubleFromMap(curr, "x", 0.0);
            long t1 = getLongFromMap(prev, "timestamp", 0L);
            long t2 = getLongFromMap(curr, "timestamp", 0L);
            
            double distance = Math.abs(x2 - x1);
            long timeDiff = t2 - t1;
            
            if (timeDiff > 0) {
                double speed = distance / timeDiff; // px/ms
                totalSpeed += speed;
                validSegments++;
            }
        }
        
        return validSegments > 0 ? totalSpeed / validSegments : 0.0;
    }
    
    /**
     * 计算滑动速度方差
     */
    private double calculateSlideSpeedVariance(List<Map<String, Object>> slideTrack, double avgSpeed) {
        if (slideTrack == null || slideTrack.size() < 2) {
            return 0.0;
        }
        
        double sumSquaredDiff = 0.0;
        int validSegments = 0;
        
        for (int i = 1; i < slideTrack.size(); i++) {
            Map<String, Object> prev = slideTrack.get(i - 1);
            Map<String, Object> curr = slideTrack.get(i);
            
            double x1 = getDoubleFromMap(prev, "x", 0.0);
            double x2 = getDoubleFromMap(curr, "x", 0.0);
            long t1 = getLongFromMap(prev, "timestamp", 0L);
            long t2 = getLongFromMap(curr, "timestamp", 0L);
            
            double distance = Math.abs(x2 - x1);
            long timeDiff = t2 - t1;
            
            if (timeDiff > 0) {
                double speed = distance / timeDiff;
                sumSquaredDiff += Math.pow(speed - avgSpeed, 2);
                validSegments++;
            }
        }
        
        return validSegments > 0 ? Math.sqrt(sumSquaredDiff / validSegments) : 0.0;
    }
    
    /**
     * 计算回退次数（向后滑动的次数）
     */
    private int calculateBacktrackCount(List<Map<String, Object>> slideTrack) {
        if (slideTrack == null || slideTrack.size() < 2) {
            return 0;
        }
        
        int backtrackCount = 0;
        
        for (int i = 1; i < slideTrack.size(); i++) {
            double prevX = getDoubleFromMap(slideTrack.get(i - 1), "x", 0.0);
            double currX = getDoubleFromMap(slideTrack.get(i), "x", 0.0);
            
            // 向左滑动（回退）
            if (currX < prevX) {
                backtrackCount++;
            }
        }
        
        return backtrackCount;
    }
    
    /**
     * 计算平均加速度
     */
    private double calculateAverageAcceleration(List<Map<String, Object>> slideTrack) {
        if (slideTrack == null || slideTrack.size() < 3) {
            return 0.0;
        }
        
        double totalAcceleration = 0.0;
        int validSegments = 0;
        
        for (int i = 2; i < slideTrack.size(); i++) {
            Map<String, Object> p1 = slideTrack.get(i - 2);
            Map<String, Object> p2 = slideTrack.get(i - 1);
            Map<String, Object> p3 = slideTrack.get(i);
            
            double x1 = getDoubleFromMap(p1, "x", 0.0);
            double x2 = getDoubleFromMap(p2, "x", 0.0);
            double x3 = getDoubleFromMap(p3, "x", 0.0);
            long t1 = getLongFromMap(p1, "timestamp", 0L);
            long t2 = getLongFromMap(p2, "timestamp", 0L);
            long t3 = getLongFromMap(p3, "timestamp", 0L);
            
            // 计算两段的速度
            double v1 = (t2 - t1) > 0 ? (x2 - x1) / (t2 - t1) : 0;
            double v2 = (t3 - t2) > 0 ? (x3 - x2) / (t3 - t2) : 0;
            
            // 计算加速度
            long timeDiff = t3 - t1;
            if (timeDiff > 0) {
                double acceleration = (v2 - v1) / timeDiff;
                totalAcceleration += Math.abs(acceleration);
                validSegments++;
            }
        }
        
        return validSegments > 0 ? totalAcceleration / validSegments : 0.0;
    }
    
    /**
     * 从Map获取Double值
     */
    private double getDoubleFromMap(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
    
    /**
     * 从Map获取Long值
     */
    private long getLongFromMap(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }
    
    /**
     * 从Map获取整数值
     */
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    /**
     * 从Map获取双精度值
     */
    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        }
        return defaultValue;
    }
    
    /**
     * 解除IP封禁
     */
    @Override
    public boolean unblockIp(String ip) {
        try {
            Set<String> blockKeys = new HashSet<>();
            ScanOptions blockOptions = ScanOptions.scanOptions()
                .match(IP_BLOCK_PREFIX + ip + "*")
                .count(200)
                .build();

            try (Cursor<String> cursor = redisTemplate.scan(blockOptions)) {
                while (cursor.hasNext()) {
                    blockKeys.add(cursor.next());
                }
            }

            Set<String> countKeys = new HashSet<>();
            ScanOptions countOptions = ScanOptions.scanOptions()
                .match(IP_VERIFY_COUNT_PREFIX + ip + "*")
                .count(200)
                .build();

            try (Cursor<String> cursor = redisTemplate.scan(countOptions)) {
                while (cursor.hasNext()) {
                    countKeys.add(cursor.next());
                }
            }

            Long deletedBlocks = blockKeys.isEmpty() ? 0L : redisTemplate.delete(blockKeys);
            Long deletedCounts = countKeys.isEmpty() ? 0L : redisTemplate.delete(countKeys);

            if (deletedBlocks != null && deletedBlocks > 0) {
                log.info("IP封禁已解除: {}, 删除封禁记录: {}, 删除计数记录: {}", ip, deletedBlocks, deletedCounts);
                return true;
            }

            log.info("IP未被封禁: {}", ip);
            return false;
        } catch (Exception e) {
            log.error("解除IP封禁失败: {}", ip, e);
            return false;
        }
    }
    
    /**
     * 获取所有被封禁的IP列表
     */
    @Override
    public List<Map<String, Object>> getBlockedIpList() {
        List<Map<String, Object>> blockedList = new ArrayList<>();
        
        try {
            // 使用SCAN命令替代KEYS命令，避免阻塞Redis
            Set<String> keys = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions()
                .match(IP_BLOCK_PREFIX + "*")
                .count(100)
                .build();
            
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
            
            if (!keys.isEmpty()) {
                for (String key : keys) {
                    String clientId = key.replace(IP_BLOCK_PREFIX, "");
                    String ip = clientId;
                    String fingerprintTag = null;
                    int splitIndex = clientId.indexOf(':');
                    if (splitIndex > 0) {
                        ip = clientId.substring(0, splitIndex);
                        fingerprintTag = clientId.substring(splitIndex + 1);
                    }
                    
                    // 获取剩余时间（秒）
                    Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                    
                    if (ttlSeconds != null && ttlSeconds > 0) {
                        Map<String, Object> ipInfo = new HashMap<>();
                        ipInfo.put("ip", ip);
                        if (fingerprintTag != null && !fingerprintTag.isEmpty()) {
                            ipInfo.put("fingerprintTag", fingerprintTag);
                        }
                        ipInfo.put("remainingSeconds", ttlSeconds);
                        ipInfo.put("remainingMinutes", (ttlSeconds + 59) / 60); // 向上取整
                        
                        // 获取验证失败次数（如果还在计数窗口内）
                        String countKey = IP_VERIFY_COUNT_PREFIX + clientId;
                        String countStr = redisTemplate.opsForValue().get(countKey);
                        int failCount = countStr != null ? Integer.parseInt(countStr) : 0;
                        ipInfo.put("failCount", failCount);
                        
                        blockedList.add(ipInfo);
                    }
                }
                
                // 按剩余时间降序排序（时间长的在前）
                blockedList.sort((a, b) -> {
                    Long timeA = (Long) a.get("remainingSeconds");
                    Long timeB = (Long) b.get("remainingSeconds");
                    return timeB.compareTo(timeA);
                });
            }
            
            return blockedList;
        } catch (Exception e) {
            log.error("获取封禁IP列表失败", e);
            return blockedList;
        }
    }
}
