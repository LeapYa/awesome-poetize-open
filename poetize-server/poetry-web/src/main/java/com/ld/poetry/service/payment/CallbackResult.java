package com.ld.poetry.service.payment;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付回调解析结果
 *
 * @author LeapYa
 * @since 2026-02-18
 */
@Data
public class CallbackResult {

    /**
     * 是否验签成功
     */
    private boolean verified;

    /**
     * 平台订单号
     */
    private String platformOrderId;

    /**
     * 平台用户ID
     */
    private String platformUserId;

    /**
     * 自定义订单ID（如爱发电的 custom_order_id = "u42_a15"）
     */
    private String customOrderId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付状态（2=成功 for 爱发电，TRADE_SUCCESS for 支付宝等）
     */
    private int status;

    /**
     * 方案ID（爱发电特有）
     */
    private String planId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 从 customOrderId 解析出的博客用户ID
     */
    private Integer parsedUserId;

    /**
     * 从 customOrderId 解析出的文章ID
     */
    private Integer parsedArticleId;

    /**
     * 解析 customOrderId（格式: u{userId}_a{articleId}）
     */
    public boolean parseCustomOrderId() {
        if (customOrderId == null || customOrderId.isEmpty()) {
            return false;
        }
        try {
            // 格式: u42_a15
            String[] parts = customOrderId.split("_");
            if (parts.length == 2 && parts[0].startsWith("u") && parts[1].startsWith("a")) {
                this.parsedUserId = Integer.parseInt(parts[0].substring(1));
                this.parsedArticleId = Integer.parseInt(parts[1].substring(1));
                return true;
            }
        } catch (NumberFormatException e) {
            // 解析失败
        }
        return false;
    }
}
