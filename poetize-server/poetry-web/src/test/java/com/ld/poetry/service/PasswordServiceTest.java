package com.ld.poetry.service;

import com.ld.poetry.config.PasswordSecurityPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;

import static org.springframework.util.DigestUtils.md5DigestAsHex;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码服务测试类
 *
 * @author LeapYa
 * @since 2025-07-20
 */
@SpringBootTest
@ActiveProfiles("test")
class PasswordServiceTest {

    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        // 创建测试用的安全策略配置
        PasswordSecurityPolicy securityPolicy = PasswordSecurityPolicy.getCompatibleModeConfig();
        passwordService = new PasswordService(securityPolicy);
    }

    @Test
    void testBCryptEncoding() {
        String rawPassword = "TestPassword123!";
        String encoded = passwordService.encodeBCrypt(rawPassword);
        
        assertNotNull(encoded);
        assertTrue(passwordService.isBCryptPassword(encoded));
        assertTrue(passwordService.matches(rawPassword, encoded));
        
        // 测试相同密码生成不同的哈希值（盐值不同）
        String encoded2 = passwordService.encodeBCrypt(rawPassword);
        assertNotEquals(encoded, encoded2);
        assertTrue(passwordService.matches(rawPassword, encoded2));
    }

    @Test
    void testMD5Compatibility() {
        String rawPassword = "hello";
        String md5Hash = "5d41402abc4b2a76b9719d911017c592"; // "hello"的MD5
        
        assertTrue(passwordService.isMD5Password(md5Hash));
        assertTrue(passwordService.matches(rawPassword, md5Hash));
        assertFalse(passwordService.isBCryptPassword(md5Hash));
    }

    @Test
    void testPasswordFormatDetection() {
        // BCrypt格式测试
        String bcryptPassword = "$2a$12$abcdefghijklmnopqrstuvwxyz123456789";
        assertTrue(passwordService.isBCryptPassword(bcryptPassword));
        assertFalse(passwordService.isMD5Password(bcryptPassword));
        
        // MD5格式测试
        String md5Password = "5d41402abc4b2a76b9719d911017c592";
        assertTrue(passwordService.isMD5Password(md5Password));
        assertFalse(passwordService.isBCryptPassword(md5Password));
        
        // 无效格式测试
        String invalidPassword = "invalid_password";
        assertFalse(passwordService.isBCryptPassword(invalidPassword));
        assertFalse(passwordService.isMD5Password(invalidPassword));
    }

    @Test
    void testPasswordUpgrade() {
        String rawPassword = "TestPassword123!";
        String md5Hash = md5(rawPassword);
        
        assertTrue(passwordService.needsUpgrade(md5Hash));
        
        String upgradedPassword = passwordService.upgradePassword(rawPassword, md5Hash);
        assertTrue(passwordService.isBCryptPassword(upgradedPassword));
        assertTrue(passwordService.matches(rawPassword, upgradedPassword));
        assertFalse(passwordService.needsUpgrade(upgradedPassword));
    }

    @Test
    void testPasswordValidation() {
        // 有效密码测试（仅检查非空）
        assertTrue(passwordService.isPasswordValid("any_password"));
        assertTrue(passwordService.isPasswordValid("123"));
        assertTrue(passwordService.isPasswordValid("weak"));
        assertTrue(passwordService.isPasswordValid("StrongPass123!"));

        // 无效密码测试
        assertFalse(passwordService.isPasswordValid(""));
        assertFalse(passwordService.isPasswordValid(null));
        assertFalse(passwordService.isPasswordValid("   "));  // 仅空格
    }

    @Test
    void testDecryptFromFrontend() {
        // 这里需要模拟前端AES加密的数据
        // 实际测试中需要使用真实的加密数据
        String testPassword = "TestPassword123!";
        
        // 注意：这里只是测试方法存在，实际测试需要真实的AES加密数据
        assertThrows(IllegalArgumentException.class, () -> {
            passwordService.decryptFromFrontend("");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            passwordService.decryptFromFrontend(null);
        });
    }

    @Test
    void testPasswordMatching() {
        String rawPassword = "TestPassword123!";
        
        // BCrypt匹配测试
        String bcryptHash = passwordService.encodeBCrypt(rawPassword);
        assertTrue(passwordService.matches(rawPassword, bcryptHash));
        assertFalse(passwordService.matches("wrongpassword", bcryptHash));
        
        // MD5匹配测试
        String md5Hash = md5(rawPassword);
        assertTrue(passwordService.matches(rawPassword, md5Hash));
        assertFalse(passwordService.matches("wrongpassword", md5Hash));
        
        // 空值测试
        assertFalse(passwordService.matches("", bcryptHash));
        assertFalse(passwordService.matches(rawPassword, ""));
        assertFalse(passwordService.matches(null, bcryptHash));
        assertFalse(passwordService.matches(rawPassword, null));
    }

    @Test
    void testUpgradePasswordWithWrongPassword() {
        String rawPassword = "TestPassword123!";
        String wrongPassword = "WrongPassword123!";
        String md5Hash = md5(rawPassword);
        
        // 使用错误的密码尝试升级应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            passwordService.upgradePassword(wrongPassword, md5Hash);
        });
    }

    @Test
    void testPasswordEncodingWithEmptyInput() {
        // 测试空输入
        assertThrows(IllegalArgumentException.class, () -> {
            passwordService.encodeBCrypt("");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            passwordService.encodeBCrypt(null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            md5("");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            md5(null);
        });
    }

    private String md5(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
    }
}
