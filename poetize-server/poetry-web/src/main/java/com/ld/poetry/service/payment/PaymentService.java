package com.ld.poetry.service.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ld.poetry.dao.ArticlePaymentMapper;
import com.ld.poetry.entity.ArticlePayment;
import com.ld.poetry.entity.SysPlugin;
import com.ld.poetry.plugin.GroovyPluginEngine;
import com.ld.poetry.service.SysPluginService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 支付业务服务
 * <p>
 * 负责读取激活的支付插件 → 路由到对应 Provider → 处理支付逻辑
 * </p>
 *
 * @author LeapYa
 * @since 2026-02-18
 */
@Slf4j
@Service
public class PaymentService {

    @Autowired
    private SysPluginService sysPluginService;

    @Autowired
    private ArticlePaymentMapper articlePaymentMapper;

    @Autowired
    private List<PaymentProvider> providers;

    @Autowired
    private GroovyPluginEngine groovyPluginEngine;

    @Autowired
    private GroovyPaymentAdapter groovyPaymentAdapter;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取当前激活的 Provider
     * <p>
     * 查找顺序：Java 实现（编译期注册）→ Groovy 实现（运行期安装）
     * </p>
     */
    public PaymentProvider getActiveProvider() {
        SysPlugin activePlugin = sysPluginService.getActivePlugin(SysPlugin.TYPE_PAYMENT);
        if (activePlugin == null) {
            return null;
        }

        String pluginKey = activePlugin.getPluginKey();

        // 1. 先从 Java Provider 中找（排除 Groovy中间层自身）
        PaymentProvider javaProvider = providers.stream()
                .filter(p -> !(p instanceof GroovyPaymentAdapter))
                .filter(p -> p.getPlatformKey().equals(pluginKey))
                .findFirst()
                .orElse(null);
        if (javaProvider != null) {
            return javaProvider;
        }

        // 2. 从 Groovy 引擎中找
        if (groovyPluginEngine.isLoaded(pluginKey)) {
            groovyPaymentAdapter.setCurrentPluginKey(pluginKey);
            return groovyPaymentAdapter;
        }

        log.warn("未找到匹配的支付 Provider: pluginKey={}，请确认已安装对应支付插件", pluginKey);
        return null;
    }

    /**
     * 获取当前激活插件的配置
     */
    public Map<String, Object> getActiveConfig() {
        SysPlugin activePlugin = sysPluginService.getActivePlugin(SysPlugin.TYPE_PAYMENT);
        if (activePlugin == null || activePlugin.getPluginConfig() == null) {
            return null;
        }

        try {
            return objectMapper.readValue(activePlugin.getPluginConfig(),
                    new TypeReference<Map<String, Object>>() {
                    });
        } catch (Exception e) {
            log.error("解析支付插件配置失败", e);
            return null;
        }
    }

    /**
     * 生成支付链接
     */
    public String getPaymentUrl(Integer articleId, Integer userId) {
        PaymentProvider provider = getActiveProvider();
        if (provider == null) {
            throw new IllegalStateException("未配置或未激活支付插件");
        }

        Map<String, Object> config = getActiveConfig();
        if (config == null) {
            throw new IllegalStateException("支付插件配置为空");
        }

        return provider.getPaymentUrl(articleId, userId, config);
    }

    /**
     * 处理回调通知
     *
     * @return 应返回给平台的响应内容，null 表示处理失败
     */
    public String handleCallback(String platformKey, HttpServletRequest request) {
        // 1. 先从 Java Provider 中找
        PaymentProvider provider = providers.stream()
                .filter(p -> !(p instanceof GroovyPaymentAdapter))
                .filter(p -> p.getPlatformKey().equals(platformKey))
                .findFirst()
                .orElse(null);

        // 2. 如果 Java 中没有，尝试 Groovy 插件
        if (provider == null && groovyPluginEngine.isLoaded(platformKey)) {
            groovyPaymentAdapter.setCurrentPluginKey(platformKey);
            provider = groovyPaymentAdapter;
        }

        if (provider == null) {
            log.warn("未找到平台 Provider: {}", platformKey);
            return null;
        }

        // 获取该平台的配置（不一定是激活的，回调要支持已配置的平台）
        SysPlugin plugin = getPluginByKey(platformKey);
        if (plugin == null || plugin.getPluginConfig() == null) {
            log.warn("平台 {} 未配置", platformKey);
            return null;
        }

        Map<String, Object> config;
        try {
            config = objectMapper.readValue(plugin.getPluginConfig(),
                    new TypeReference<Map<String, Object>>() {
                    });
        } catch (Exception e) {
            log.error("解析平台 {} 配置失败", platformKey, e);
            return null;
        }

        // 验签并解析
        CallbackResult result = provider.verifyCallback(request, config);
        if (result == null || !result.isVerified()) {
            log.warn("平台 {} 回调验签失败", platformKey);
            return null;
        }

        // 幂等检查
        if (result.getPlatformOrderId() != null) {
            LambdaQueryWrapper<ArticlePayment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ArticlePayment::getPlatformOrderId, result.getPlatformOrderId())
                    .eq(ArticlePayment::getPlatform, platformKey);
            ArticlePayment existing = articlePaymentMapper.selectOne(wrapper);
            if (existing != null) {
                log.info("订单 {} 已存在，跳过（幂等）", result.getPlatformOrderId());
                return provider.getSuccessResponse();
            }
        }

        // 创建支付记录
        if (result.getParsedUserId() != null && result.getParsedArticleId() != null) {
            ArticlePayment payment = new ArticlePayment();
            payment.setUserId(result.getParsedUserId());
            payment.setArticleId(result.getParsedArticleId());
            payment.setAmount(result.getAmount());
            payment.setPlatform(platformKey);
            payment.setPlatformOrderId(result.getPlatformOrderId());
            payment.setPlatformUserId(result.getPlatformUserId());
            payment.setCustomOrderId(result.getCustomOrderId());
            payment.setPaymentStatus(1); // 已支付
            payment.setPayType(result.getParsedArticleId() == 0 ? 2 : 1);
            payment.setRemark(result.getRemark());
            payment.setCreateTime(LocalDateTime.now());
            payment.setUpdateTime(LocalDateTime.now());

            // 会员购买（articleId=0）设置过期时间
            if (result.getParsedArticleId() == 0) {
                int memberDays = 30; // 默认30天
                if (config.containsKey("memberDurationDays")) {
                    Object val = config.get("memberDurationDays");
                    if (val instanceof Number) {
                        memberDays = ((Number) val).intValue();
                    }
                }
                // 如果已有未过期的会员记录，在其基础上续期
                LambdaQueryWrapper<ArticlePayment> memberQuery = new LambdaQueryWrapper<>();
                memberQuery.eq(ArticlePayment::getUserId, result.getParsedUserId())
                        .eq(ArticlePayment::getArticleId, 0)
                        .eq(ArticlePayment::getPaymentStatus, 1)
                        .isNotNull(ArticlePayment::getExpireTime)
                        .gt(ArticlePayment::getExpireTime, LocalDateTime.now())
                        .orderByDesc(ArticlePayment::getExpireTime)
                        .last("LIMIT 1");
                ArticlePayment existingMember = articlePaymentMapper.selectOne(memberQuery);
                LocalDateTime baseTime = (existingMember != null) ? existingMember.getExpireTime()
                        : LocalDateTime.now();
                payment.setExpireTime(baseTime.plusDays(memberDays));
            }

            articlePaymentMapper.insert(payment);
            log.info("支付记录创建成功: userId={}, articleId={}, amount={}, orderId={}",
                    result.getParsedUserId(), result.getParsedArticleId(),
                    result.getAmount(), result.getPlatformOrderId());
        } else {
            log.warn("无法解析 customOrderId: {}，订单已记录但未关联用户和文章", result.getCustomOrderId());
            // 仍然记录，由管理员手动处理
            ArticlePayment payment = new ArticlePayment();
            payment.setUserId(0);
            payment.setArticleId(0);
            payment.setAmount(result.getAmount());
            payment.setPlatform(platformKey);
            payment.setPlatformOrderId(result.getPlatformOrderId());
            payment.setPlatformUserId(result.getPlatformUserId());
            payment.setCustomOrderId(result.getCustomOrderId());
            payment.setPaymentStatus(0); // 待确认
            payment.setPayType(1);
            payment.setRemark("无法解析用户和文章: " + result.getCustomOrderId());
            payment.setCreateTime(LocalDateTime.now());
            payment.setUpdateTime(LocalDateTime.now());

            articlePaymentMapper.insert(payment);
        }

        return provider.getSuccessResponse();
    }

    /**
     * 检查用户是否已付费
     */
    public boolean hasPaid(Integer userId, Integer articleId) {
        if (userId == null || articleId == null) {
            return false;
        }

        LambdaQueryWrapper<ArticlePayment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticlePayment::getUserId, userId)
                .eq(ArticlePayment::getArticleId, articleId)
                .eq(ArticlePayment::getPaymentStatus, 1); // 已支付

        return articlePaymentMapper.selectCount(wrapper) > 0;
    }

    /**
     * 检查用户是否为全站会员（articleId=0 的支付记录，且未过期）
     */
    public boolean isMember(Integer userId) {
        if (userId == null) {
            return false;
        }

        LambdaQueryWrapper<ArticlePayment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticlePayment::getUserId, userId)
                .eq(ArticlePayment::getArticleId, 0)
                .eq(ArticlePayment::getPaymentStatus, 1)
                .and(w -> w.isNull(ArticlePayment::getExpireTime)
                        .or().gt(ArticlePayment::getExpireTime, LocalDateTime.now()));

        return articlePaymentMapper.selectCount(wrapper) > 0;
    }

    /**
     * 获取文章付费人数
     */
    public int getPaidCount(Integer articleId) {
        if (articleId == null) {
            return 0;
        }

        LambdaQueryWrapper<ArticlePayment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticlePayment::getArticleId, articleId)
                .eq(ArticlePayment::getPaymentStatus, 1);

        return Math.toIntExact(articlePaymentMapper.selectCount(wrapper));
    }

    /**
     * 测试当前激活平台的连接
     */
    public boolean testConnection() {
        PaymentProvider provider = getActiveProvider();
        if (provider == null) {
            return false;
        }

        Map<String, Object> config = getActiveConfig();
        if (config == null) {
            return false;
        }

        return provider.testConnection(config);
    }

    /**
     * 根据 pluginKey 获取支付插件
     */
    private SysPlugin getPluginByKey(String pluginKey) {
        LambdaQueryWrapper<SysPlugin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPlugin::getPluginType, SysPlugin.TYPE_PAYMENT)
                .eq(SysPlugin::getPluginKey, pluginKey);

        List<SysPlugin> plugins = sysPluginService.list(wrapper);
        return plugins.isEmpty() ? null : plugins.get(0);
    }
}
