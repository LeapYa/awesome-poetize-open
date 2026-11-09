package com.ld.poetry.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 黑名单 TTL 单调性测试：自动封禁只能延长，不能把更长的封禁减刑。
 *
 * <p>真实比较在 Redis Lua 内原子完成，这里验证 Java 侧对返回值的处理。
 */
class RedisUtilTtlTest {

    private RedisUtil redisUtil;
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        redisUtil = new RedisUtil();
        ReflectionTestUtils.setField(redisUtil, "stringRedisTemplate", stringRedisTemplate);
    }

    @Test
    @DisplayName("脚本返回 1：key 不存在或新 TTL 更长，写入成功")
    void scriptReturnsOne_writes() {
        stubResult(1L);

        assertTrue(redisUtil.setIfLonger("blacklist:1.2.3.4", "auto", 86400));
    }

    @Test
    @DisplayName("脚本返回 0：已有永久封禁或更长 TTL，不覆盖")
    void scriptReturnsZero_doesNotOverwrite() {
        stubResult(0L);

        assertFalse(redisUtil.setIfLonger("blacklist:1.2.3.4", "auto", 86400));
    }

    @Test
    @DisplayName("非正 TTL 不写入，避免误缩短无限期封禁")
    void nonPositiveTtl_doesNotWrite() {
        assertFalse(redisUtil.setIfLonger("blacklist:1.2.3.4", "auto", 0));
    }

    @SuppressWarnings("unchecked")
    private void stubResult(long result) {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .thenReturn(result);
    }
}
