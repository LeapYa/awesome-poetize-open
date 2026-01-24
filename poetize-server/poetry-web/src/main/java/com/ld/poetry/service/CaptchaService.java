package com.ld.poetry.service;

import java.util.List;
import java.util.Map;

/**
 * 验证码验证服务接口
 */
public interface CaptchaService {
    
    /**
     * 检查特定操作是否需要验证码
     * @param action 操作类型 (login, register, comment, reset_password)
     * @return 是否需要验证码
     */
    boolean isCaptchaRequired(String action);
    
    /**
     * 验证复选框验证码
     * @param mouseTrack 鼠标轨迹
     * @param straightRatio 直线率
     * @param isReplyComment 是否回复评论
     * @param retryCount 重试次数
     * @param frontendSensitivity 前端传递的敏感度
     * @param frontendMinPoints 前端传递的最小点数
     * @param clickDelay 点击延迟（毫秒）- 从开始移动鼠标到点击
     * @param thinkingTime 用户思考时间（毫秒）- 从组件显示到点击
     * @param isTouchDevice 是否为触屏设备
     * @param browserFingerprint 浏览器指纹
     * @param clientIp 客户端IP
     * @return 验证结果Map（包含success和token）
     */
    Map<String, Object> verifyCheckboxCaptcha(
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
        String action
    );
    
    /**
     * 验证令牌有效性
     * @param token 验证令牌
     * @return 是否有效
     */
    boolean verifyToken(String token);

    /**
     * 验证令牌有效性（带上下文）
     * @param action 操作类型
     * @param token 验证令牌
     * @param clientIp 客户端IP
     * @param browserFingerprint 浏览器指纹
     * @return 是否有效
     */
    boolean verifyToken(String action, String token, String clientIp, String browserFingerprint);
    
    /**
     * 验证滑动验证码
     * @param slideTrack 滑动轨迹
     * @param totalTime 总耗时（毫秒）
     * @param maxDistance 最大滑动距离
     * @param finalPosition 最终位置
     * @param browserFingerprint 浏览器指纹
     * @param clientIp 客户端IP
     * @param avgSpeed 前端计算的平均速度（蜜罐字段）
     * @param backtrackCount 前端计算的回退次数（蜜罐字段）
     * @param trackPointCount 前端计算的轨迹点数量（蜜罐字段）
     * @return 验证结果Map
     */
    Map<String, Object> verifySlideCaptcha(
        List<Map<String, Object>> slideTrack,
        Long totalTime,
        Double maxDistance,
        Double finalPosition,
        String browserFingerprint,
        String clientIp,
        Double avgSpeed,
        Integer backtrackCount,
        Integer trackPointCount,
        String action
    );
    
    /**
     * 获取公共验证码配置（供前端使用）
     * @return 公共配置Map
     */
    Map<String, Object> getPublicCaptchaConfig();
    
    /**
     * 生成验证令牌
     * @return 验证令牌
     */
    String generateVerificationToken();
    
    /**
     * 解除IP封禁
     * @param ip IP地址
     * @return 是否成功
     */
    boolean unblockIp(String ip);
    
    /**
     * 获取所有被封禁的IP列表
     * @return 封禁IP列表（包含IP、剩余时间等信息）
     */
    List<Map<String, Object>> getBlockedIpList();
}
