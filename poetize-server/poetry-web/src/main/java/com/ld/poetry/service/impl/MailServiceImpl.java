package com.ld.poetry.service.impl;

import com.ld.poetry.entity.dto.MailConfigDTO;
import com.ld.poetry.service.MailService;
import com.ld.poetry.service.SysConfigService;
import com.ld.poetry.service.SysMailConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
/**
 * 邮件服务实现类
 */
@Service
@Slf4j
public class MailServiceImpl implements MailService {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private SysConfigService sysConfigService;
    
    @Autowired
    private SysMailConfigService sysMailConfigService;
    
    // 邮箱配置现在从数据库获取
    
    /**
     * 获取所有邮箱配置
     */
    @Override
    public List<MailConfigDTO> getMailConfigs() {
        try {
            // 从数据库获取配置
            List<MailConfigDTO> configs = sysMailConfigService.getAllConfigs();
            log.info("从数据库获取到{}个邮箱配置", configs.size());
            return configs;
        } catch (Exception e) {
            log.error("获取邮箱配置失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取默认邮箱配置索引
     */
    @Override
    public int getDefaultMailConfigIndex() {
        try {
            // 从数据库获取默认索引
            Integer defaultIndex = sysMailConfigService.getDefaultConfigIndex();
            if (defaultIndex != null) {
                log.info("获取默认邮箱配置索引: {}", defaultIndex);
                return defaultIndex;
            }
            
            log.warn("没有默认邮箱配置，返回-1");
            return -1;
        } catch (Exception e) {
            log.error("获取默认邮箱索引失败", e);
            return -1;
        }
    }
    
    /**
     * 随机获取一个启用的邮箱配置
     */
    @Override
    public MailConfigDTO getRandomMailConfig() {
        List<MailConfigDTO> configs = getMailConfigs();
        
        // 过滤出启用的配置
        List<MailConfigDTO> enabledConfigs = configs.stream()
                .filter(config -> Boolean.TRUE.equals(config.getEnabled()))
                .collect(Collectors.toList());
        
        if (enabledConfigs.isEmpty()) {
            log.error("没有启用的邮箱配置");
            return null;
        }
        
        // 随机选择一个配置
        Random random = new Random();
        int index = random.nextInt(enabledConfigs.size());
        return enabledConfigs.get(index);
    }
    
    /**
     * 获取默认邮箱配置
     */
    @Override
    public MailConfigDTO getDefaultMailConfig() {
        int defaultIndex = getDefaultMailConfigIndex();
        List<MailConfigDTO> configs = getMailConfigs();
        
        if (defaultIndex >= 0 && defaultIndex < configs.size()) {
            MailConfigDTO config = configs.get(defaultIndex);
            if (Boolean.TRUE.equals(config.getEnabled())) {
                return config;
            }
        }
        
        // 如果默认配置不可用，则返回第一个启用的配置
        return configs.stream()
                .filter(config -> Boolean.TRUE.equals(config.getEnabled()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 发送测试邮件
     */
    @Override
    public boolean sendTestEmail(MailConfigDTO config, String testEmail) {
        if (config == null || testEmail == null || testEmail.trim().isEmpty()) {
            log.error("测试邮件参数错误，配置为空或测试邮箱为空");
            return false;
        }
        
        try {
            // 生成当前时间
            LocalDateTime now = LocalDateTime.now();
            // 定义格式化模式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String currentTime = now.format(formatter);
            
            // 构建测试邮件内容
            String subject = "邮箱配置测试";
            StringBuilder content = new StringBuilder();
            content.append("<div style=\"font-family: serif;line-height: 22px;padding: 30px\">");
            content.append("<div style=\"display: flex;flex-direction: column;align-items: center\">");
            content.append("<div style=\"margin: 10px auto 20px;text-align: center\">");
            content.append("<div style=\"line-height: 32px;font-size: 26px;font-weight: bold;color: #000000\">");
            content.append("邮箱配置测试");
            content.append("</div>");
            content.append("<div style=\"font-size: 16px;font-weight: bold;color: rgba(0, 0, 0, 0.19);margin-top: 21px\">");
            content.append("配置测试信息");
            content.append("</div>");
            content.append("</div>");
            content.append("<div style=\"min-width: 250px;max-width: 800px;min-height: 128px;background: #F7F7F7;border-radius: 10px;padding: 32px\">");
            content.append("<div>");
            content.append("<div style=\"font-size: 18px;font-weight: bold;color: #C5343E\">");
            content.append("邮箱配置信息");
            content.append("</div>");
            content.append("<div style=\"margin-top: 6px;font-size: 16px;color: #000000\">");
            content.append("<p>这是一封测试邮件，用于验证您的邮箱配置是否正确。</p>");
            content.append("</div>");
            content.append("</div>");
            
            content.append("<hr style=\"border: 1px dashed #ef859d2e;margin: 20px 0\">");
            content.append("<div>");
            content.append("<div style=\"font-size: 18px;font-weight: bold;color: #C5343E\">");
            content.append("基本配置");
            content.append("</div>");
            content.append("<div style=\"margin-top: 6px;font-size: 16px;color: #000000\">");
            content.append("<p><ul style=\"list-style-type: none; padding-left: 0;\">");
            content.append("<li>📧 <strong>邮箱服务器:</strong> ").append(config.getHost()).append("</li>");
            content.append("<li>📮 <strong>端口:</strong> ").append(config.getPort()).append("</li>");
            content.append("<li>👤 <strong>账号:</strong> ").append(config.getUsername()).append("</li>");
            content.append("<li>👔 <strong>发件人名称:</strong> ").append(config.getSenderName()).append("</li>");
            content.append("<li>🔒 <strong>SSL:</strong> ").append(config.getUseSsl() ? "启用" : "禁用").append("</li>");
            content.append("<li>🔄 <strong>STARTTLS:</strong> ").append(config.getUseStarttls() ? "启用" : "禁用").append("</li>");
            content.append("<li>📝 <strong>认证:</strong> ").append(config.getAuth() ? "启用" : "禁用").append("</li>");
            content.append("<li>⏱️ <strong>测试时间:</strong> ").append(currentTime).append("</li>");
            content.append("</ul></p>");
            content.append("</div>");
            content.append("</div>");
            
            content.append("<hr style=\"border: 1px dashed #ef859d2e;margin: 20px 0\">");
            content.append("<div>");
            content.append("<div style=\"font-size: 18px;font-weight: bold;color: #C5343E\">");
            content.append("高级配置");
            content.append("</div>");
            content.append("<div style=\"margin-top: 6px;font-size: 16px;color: #000000\">");
            content.append("<p><ul style=\"list-style-type: none; padding-left: 0;\">");
            
            // 添加协议信息
            content.append("<li>🌐 <strong>协议:</strong> ").append(config.getProtocol() != null ? config.getProtocol() : "smtp").append("</li>");
            
            // 添加超时设置
            content.append("<li>⏱️ <strong>连接超时:</strong> ").append(config.getConnectionTimeout() != null ? config.getConnectionTimeout() + "ms" : "默认").append("</li>");
            content.append("<li>⏱️ <strong>读取超时:</strong> ").append(config.getTimeout() != null ? config.getTimeout() + "ms" : "默认").append("</li>");
            
            // 添加认证机制
            content.append("<li>🔑 <strong>认证机制:</strong> ").append(config.getAuthMechanism() != null ? config.getAuthMechanism() : "默认").append("</li>");
            
            // 调试模式
            content.append("<li>🔍 <strong>调试模式:</strong> ").append(config.getDebug() != null && config.getDebug() ? "启用" : "禁用").append("</li>");
            
            // 信任所有证书
            content.append("<li>🔒 <strong>信任所有证书:</strong> ").append(config.getTrustAllCerts() != null && config.getTrustAllCerts() ? "是" : "否").append("</li>");
            
            // 代理配置
            if (config.getUseProxy() != null && config.getUseProxy()) {
                content.append("<li>🔄 <strong>代理设置:</strong> ").append(config.getProxyHost()).append(":").append(config.getProxyPort()).append("</li>");
                if (config.getProxyUser() != null && !config.getProxyUser().isEmpty()) {
                    content.append("<li>👤 <strong>代理认证:</strong> 已配置</li>");
                }
            } else {
                content.append("<li>🔄 <strong>代理设置:</strong> 未使用</li>");
            }
            
            // 自定义属性
            if (config.getCustomProperties() != null && !config.getCustomProperties().isEmpty()) {
                content.append("<li>⚙️ <strong>自定义属性:</strong> ").append(config.getCustomProperties().size()).append("个</li>");
                // 显示自定义属性，但过滤掉可能包含敏感信息的项
                content.append("<li><ul style=\"list-style-type: disc; padding-left: 20px;\">");
                config.getCustomProperties().forEach((key, value) -> {
                    // 跳过包含password、secret、key等敏感信息的属性
                    if (!key.toLowerCase().contains("password") && 
                        !key.toLowerCase().contains("secret") && 
                        !key.toLowerCase().contains("key")) {
                        content.append("<li>").append(key).append(": ").append(value).append("</li>");
                    } else {
                        content.append("<li>").append(key).append(": ******</li>");
                    }
                });
                content.append("</ul></li>");
            } else {
                content.append("<li>⚙️ <strong>自定义属性:</strong> 无</li>");
            }
            
            content.append("</ul></p>");
            content.append("</div>");
            content.append("</div>");
            
            content.append("<hr style=\"border: 1px dashed #ef859d2e;margin: 20px 0\">");
            content.append("<div>");
            content.append("<div style=\"font-size: 18px;font-weight: bold;color: #C5343E\">");
            content.append("提示");
            content.append("</div>");
            content.append("<div style=\"margin-top: 6px;font-size: 16px;color: #000000\">");
            content.append("<p>邮件发送成功，配置有效！您可以保存并使用此配置。</p>");
            content.append("</div>");
            content.append("</div>");
            
            content.append("</div>");
            content.append("</div>");
            content.append("</div>");
            
            // 发送邮件
            List<String> toList = Collections.singletonList(testEmail);
            return sendMail(toList, subject, content.toString(), true, config);
        } catch (Exception e) {
            log.error("测试邮件发送失败", e);
            return false;
        }
    }
    
    @Override
    public boolean sendVerificationCode(String email, String code) {
        if (email == null || email.trim().isEmpty() || code == null || code.trim().isEmpty()) {
            log.error("验证码邮件参数错误，邮箱为空或验证码为空");
            return false;
        }
        
        try {
            // 构建验证码邮件内容
            // 从数据库获取验证码邮件主题
            String subject = sysConfigService.getConfigValueByKey("user.code.subject");
            if (subject == null || subject.trim().isEmpty()) {
                // 如果数据库中没有配置，使用默认主题
                subject = "POETIZE验证码";
                log.warn("数据库中未找到验证码邮件主题配置，使用默认主题");
            }
            
            // 从数据库获取验证码模板
            String template = sysConfigService.getConfigValueByKey("user.code.format");
            if (template == null || template.trim().isEmpty()) {
                // 如果数据库中没有配置，使用默认模板
                template = "【POETIZE】%s为本次验证的验证码，请在5分钟内完成验证。为保证账号安全，请勿泄漏此验证码。";
                log.warn("数据库中未找到验证码模板配置，使用默认模板");
            }
            
            log.info("使用验证码模板: {}", template); // 添加日志记录使用的模板
            String content = String.format(template, code);
            
            // 发送邮件 - 使用顺序重试机制
            List<String> toList = Collections.singletonList(email);
            return sendMailWithSequentialRetry(toList, subject, content, true);
        } catch (Exception e) {
            log.error("验证码邮件发送失败", e);
            return false;
        }
    }
    
    /**
     * 发送普通邮件
     */
    @Override
    public boolean sendMail(List<String> to, String subject, String content, boolean html, MailConfigDTO config) {
        if (to == null || to.isEmpty() || subject == null || content == null) {
            log.error("邮件参数错误，收件人为空或主题为空或内容为空");
            return false;
        }
        
        try {
            // 如果没有提供配置，使用默认配置
            if (config == null) {
                config = getDefaultMailConfig();
                if (config == null) {
                    log.error("没有默认邮箱配置");
                    return false;
                }
            }
            
            // 使用配置创建JavaMailSenderImpl
            JavaMailSenderImpl mailSender = createMailSender(config);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // 设置发件人
            helper.setFrom(new InternetAddress(config.getUsername(), config.getSenderName(), "UTF-8"));
            
            // 设置收件人
            helper.setTo(to.toArray(new String[0]));
            helper.setSubject(subject);
            
            // 直接使用提供的内容，不再进行格式化
            helper.setText(content, html);
            
            // 发送邮件
            mailSender.send(message);
            
            log.info("邮件发送成功: {}", to);
            return true;
        } catch (Exception e) {
            log.error("邮件发送失败", e);
            return false;
        }
    }
    
    /**
     * 按顺序尝试所有可用的邮箱配置发送邮件
     * 该方法会依次尝试每个启用的邮箱配置，直到成功发送邮件或尝试完所有配置
     * 
     * @param to 收件人列表
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param html 是否HTML格式
     * @return 是否发送成功
     */
    @Override
    public boolean sendMailWithSequentialRetry(List<String> to, String subject, String content, boolean html) {
        if (to == null || to.isEmpty() || subject == null || content == null) {
            log.error("邮件参数错误，收件人为空或主题为空或内容为空");
            return false;
        }
        
        // 获取所有邮箱配置
        List<MailConfigDTO> allConfigs = getMailConfigs();
        if (allConfigs.isEmpty()) {
            log.error("没有可用的邮箱配置");
            return false;
        }
        
        // 过滤出启用的配置
        List<MailConfigDTO> enabledConfigs = allConfigs.stream()
                .filter(config -> Boolean.TRUE.equals(config.getEnabled()))
                .collect(Collectors.toList());
        
        if (enabledConfigs.isEmpty()) {
            log.error("没有启用的邮箱配置");
            return false;
        }
        
        log.info("开始按顺序尝试发送邮件，共{}个可用配置", enabledConfigs.size());
        
        // 按顺序尝试每个配置
        for (int i = 0; i < enabledConfigs.size(); i++) {
            MailConfigDTO config = enabledConfigs.get(i);
            int configIndex = i + 1;
            
            log.info("尝试使用第{}个邮箱配置发送邮件: {} ({})", 
                    configIndex, config.getSenderName(), config.getUsername());
            
            try {
                // 尝试使用当前配置发送邮件
                boolean success = sendMail(to, subject, content, html, config);
                
                if (success) {
                    log.info("邮件发送成功，使用第{}个配置: {} ({})", 
                            configIndex, config.getSenderName(), config.getUsername());
                    return true;
                } else {
                    log.warn("第{}个配置发送失败，继续尝试下一个配置: {} ({})", 
                            configIndex, config.getSenderName(), config.getUsername());
                }
            } catch (Exception e) {
                log.error("第{}个配置发送异常，继续尝试下一个配置: {} ({})", 
                        configIndex, config.getSenderName(), config.getUsername(), e);
            }
        }
        
        // 所有配置都尝试失败
        log.error("所有{}个邮箱配置都尝试失败，邮件发送失败", enabledConfigs.size());
        return false;
    }
    
    /**
     * 创建JavaMailSender
     */
    private JavaMailSenderImpl createMailSender(MailConfigDTO config) {
        if (config == null) {
            throw new IllegalArgumentException("邮箱配置不能为空");
        }
        
        if (config.getHost() == null || config.getHost().trim().isEmpty()) {
            throw new IllegalArgumentException("邮箱服务器地址不能为空");
        }
        
        if (config.getUsername() == null || config.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("邮箱账号不能为空");
        }
        
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        
        // 默认端口
        int port = 25;
        if (config.getPort() != null) {
            try {
                port = config.getPort();
            } catch (Exception e) {
                log.warn("端口号解析错误，使用默认端口25");
            }
        }
        sender.setPort(port);
        
        sender.setUsername(config.getUsername());
        
        // 密码可以为空（某些邮箱服务器不需要密码）
        if (config.getPassword() != null) {
            sender.setPassword(config.getPassword());
        }
        
        Properties props = new Properties();
        
        // 处理布尔类型属性，避免NPE
        boolean auth = config.getAuth() != null ? config.getAuth() : false;
        boolean useStarttls = config.getUseStarttls() != null ? config.getUseStarttls() : false;
        boolean useSsl = config.getUseSsl() != null ? config.getUseSsl() : false;
        boolean trustAllCerts = config.getTrustAllCerts() != null ? config.getTrustAllCerts() : false;
        
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", useStarttls);
        props.put("mail.smtp.ssl.enable", useSsl);
        
        // 超时设置
        if (config.getConnectionTimeout() != null) {
            try {
                int timeout = config.getConnectionTimeout();
                props.put("mail.smtp.connectiontimeout", timeout);
            } catch (Exception e) {
                log.warn("连接超时解析错误，使用默认值");
            }
        }
        
        if (config.getTimeout() != null) {
            try {
                int timeout = config.getTimeout();
                props.put("mail.smtp.timeout", timeout);
            } catch (Exception e) {
                log.warn("读取超时解析错误，使用默认值");
            }
        }
        
        if (trustAllCerts) {
            props.put("mail.smtp.ssl.trust", "*");
        }
        
        log.info("创建邮件发送器: 服务器={}, 端口={}, 用户={}, 认证={}, SSL={}, TLS={}",
                config.getHost(), port, config.getUsername(), auth, useSsl, useStarttls);
        
        sender.setJavaMailProperties(props);
        return sender;
    }
}