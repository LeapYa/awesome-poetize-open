package com.ld.poetry.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 文章付费记录表
 * </p>
 *
 * @author LeapYa
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("article_payment")
public class ArticlePayment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 博客用户ID
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 文章ID（0=全站会员）
     */
    @TableField("article_id")
    private Integer articleId;

    /**
     * 支付金额
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 支付平台 [afdian, alipay_f2f, epay, mianbaoduo, buymeacoffee]
     */
    @TableField("platform")
    private String platform;

    /**
     * 平台订单号
     */
    @TableField("platform_order_id")
    private String platformOrderId;

    /**
     * 平台用户ID
     */
    @TableField("platform_user_id")
    private String platformUserId;

    /**
     * 自定义订单ID（如爱发电的 custom_order_id）
     */
    @TableField("custom_order_id")
    private String customOrderId;

    /**
     * 支付状态 [0:待确认, 1:已支付, 2:已退款]
     */
    @TableField("payment_status")
    private Integer paymentStatus;

    /**
     * 付费类型（同 article.pay_type）
     */
    @TableField("pay_type")
    private Integer payType;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 会员过期时间（article_id=0 时使用，null 表示永久）
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;
}
