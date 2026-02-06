package com.ld.poetry.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;



/**
 * Redis配置类
 * 
 * @author LeapYa
 * @since 2024-12-20
 */
@Configuration
@Slf4j
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:123456}")
    private String redisPassword;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    /**
     * 自定义Redis连接工厂
     * 解决Spring Boot自动配置的认证问题
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("创建自定义Redis连接工厂 - Host: {}, Port: {}, Database: {}, Password: {}",
                redisHost, redisPort, redisDatabase,
                redisPassword.isEmpty() ? "未设置" : "已设置(" + redisPassword.length() + "位)");

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setDatabase(redisDatabase);

        if (!redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
            log.info("Redis密码认证已配置");
        } else {
            log.info("Redis无密码认证");
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();

        log.info("Redis连接工厂创建完成");
        return factory;
    }

    /**
     * 配置RedisTemplate
     * 使用Jackson2JsonRedisSerializer进行序列化
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
        ObjectMapper objectMapper = new ObjectMapper();

        // 添加Java 8时间模块支持
        objectMapper.registerModule(new JavaTimeModule());

        // 配置序列化设置
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        // 禁用将日期写为时间戳
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用jackson
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        
        log.info("Redis模板配置完成");
        return template;
    }

    /**
     * 配置StringRedisTemplate
     * 用于简单的字符串操作
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        log.info("StringRedis模板配置完成");
        return template;
    }

    // 注意：项目现在完全使用Redis缓存，不再使用Spring Cache框架
}
