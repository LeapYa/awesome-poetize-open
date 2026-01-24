package com.ld.poetry.service;

import com.ld.poetry.service.impl.CaptchaServiceImpl;
import com.ld.poetry.utils.JsonUtils;
import com.ld.poetry.vo.CaptchaProof;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CaptchaServiceImplTokenTest {

    @Test
    public void verifyToken_shouldConsumeLegacyToken() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        String token = "abc";
        String key = "captcha:token:" + token;
        when(valueOps.get(key)).thenReturn("1");
        when(redisTemplate.delete(key)).thenReturn(true);

        CaptchaServiceImpl service = new CaptchaServiceImpl();
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);

        assertFalse(service.verifyToken("login", token, null, null));
        verify(redisTemplate, times(1)).delete(key);
    }

    @Test
    public void verifyToken_shouldAcceptNormalizedAction() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        CaptchaProof proof = new CaptchaProof();
        proof.setVersion(1);
        proof.setAction("thirdPartyLogin");
        proof.setIssuedAt(System.currentTimeMillis());
        String proofJson = JsonUtils.toJsonString(proof);

        String token = "def";
        String key = "captcha:token:" + token;
        when(valueOps.get(key)).thenReturn(proofJson);
        when(redisTemplate.delete(key)).thenReturn(true);

        CaptchaServiceImpl service = new CaptchaServiceImpl();
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);

        assertTrue(service.verifyToken("login", token, null, null));
        verify(redisTemplate, times(1)).delete(key);
    }

    @Test
    public void verifyToken_shouldRejectActionMismatchAndConsume() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        CaptchaProof proof = new CaptchaProof();
        proof.setVersion(1);
        proof.setAction("register");
        proof.setIssuedAt(System.currentTimeMillis());
        String proofJson = JsonUtils.toJsonString(proof);

        String token = "ghi";
        String key = "captcha:token:" + token;
        when(valueOps.get(key)).thenReturn(proofJson);
        when(redisTemplate.delete(key)).thenReturn(true);

        CaptchaServiceImpl service = new CaptchaServiceImpl();
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);

        assertFalse(service.verifyToken("login", token, null, null));
        verify(redisTemplate, times(1)).delete(key);
    }

    @Test
    public void verifyToken_shouldReturnFalseWhenMissing() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        String token = "missing";
        String key = "captcha:token:" + token;
        when(valueOps.get(key)).thenReturn(null);

        CaptchaServiceImpl service = new CaptchaServiceImpl();
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);

        assertFalse(service.verifyToken("login", token, null, null));
        verify(redisTemplate, never()).delete(anyString());
    }
}
