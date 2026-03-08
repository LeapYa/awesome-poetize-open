package com.ld.poetry.controller;

import com.ld.poetry.aop.LoginCheck;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.service.payment.PaymentService;
import com.ld.poetry.utils.PoetryUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 支付控制器
 * <p>
 * 处理 Webhook 回调和支付状态查询
 * </p>
 *
 * @author LeapYa
 * @since 2026-02-18
 */
@Slf4j
@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // ============ Webhook 回调（公开，无需登录） ============

    /**
     * 支付平台 Webhook 回调入口
     * URL: /payment/webhook/{platform}
     * 例如: /payment/webhook/afdian
     */
    @PostMapping("/webhook/{platform}")
    public String handleWebhook(@PathVariable String platform, HttpServletRequest request) {
        log.info("收到支付回调, platform={}", platform);

        String response = paymentService.handleCallback(platform, request);
        if (response != null) {
            return response;
        }

        // 回调处理失败，仍然返回成功避免平台重试
        log.warn("支付回调处理失败, platform={}", platform);
        return "{\"ec\":200,\"em\":\"\"}";
    }

    // ============ 公开接口 ============

    /**
     * 获取文章的支付链接
     */
    @GetMapping("/getPaymentUrl")
    public PoetryResult<String> getPaymentUrl(@RequestParam Integer articleId) {
        Integer userId = PoetryUtil.getUserId();
        if (userId == null) {
            return PoetryResult.fail("请先登录后再进行付费操作");
        }

        try {
            String url = paymentService.getPaymentUrl(articleId, userId);
            return PoetryResult.success(url);
        } catch (Exception e) {
            log.error("生成支付链接失败", e);
            return PoetryResult.fail("生成支付链接失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户对某篇文章的付费状态
     */
    @GetMapping("/checkPayment")
    public PoetryResult<Boolean> checkPayment(@RequestParam Integer articleId) {
        Integer userId = PoetryUtil.getUserId();
        if (userId == null) {
            return PoetryResult.success(false);
        }

        boolean paid = paymentService.hasPaid(userId, articleId);
        return PoetryResult.success(paid);
    }

    // ============ 管理员接口 ============

    /**
     * 测试当前激活支付平台的连接
     */
    @LoginCheck(0)
    @PostMapping("/admin/testConnection")
    public PoetryResult<Boolean> testConnection() {
        boolean connected = paymentService.testConnection();
        if (connected) {
            return PoetryResult.success(true);
        } else {
            return PoetryResult.fail("连接失败，请检查配置");
        }
    }
}
