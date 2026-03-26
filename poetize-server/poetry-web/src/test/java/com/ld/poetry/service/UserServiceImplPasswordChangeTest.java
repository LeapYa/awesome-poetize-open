package com.ld.poetry.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.ld.poetry.config.AsyncUserContext;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.constants.CacheConstants;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.entity.User;
import com.ld.poetry.entity.WebInfo;
import com.ld.poetry.service.impl.UserServiceImpl;
import com.ld.poetry.utils.PoetryUtil;
import com.ld.poetry.utils.mail.MailUtil;
import com.ld.poetry.vo.UserVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplPasswordChangeTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private MailUtil mailUtil;

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private PasswordService passwordService;

    private UserServiceImpl service;
    private User currentUser;
    private User adminUser;
    private User refreshedUser;
    private String expectedEmailCodeKey;

    @BeforeEach
    void setUp() {
        service = spy(new UserServiceImpl());

        ReflectionTestUtils.setField(service, "cacheService", cacheService);
        ReflectionTestUtils.setField(service, "mailUtil", mailUtil);
        ReflectionTestUtils.setField(service, "sysConfigService", sysConfigService);
        ReflectionTestUtils.setField(service, "passwordService", passwordService);

        currentUser = new User();
        currentUser.setId(1001);
        currentUser.setUsername("tester");
        currentUser.setEmail("tester@example.com");
        currentUser.setPassword("$2a$10$stored-hash");

        adminUser = new User();
        adminUser.setId(1);
        adminUser.setUsername("admin");

        refreshedUser = new User();
        refreshedUser.setId(currentUser.getId());
        refreshedUser.setUsername(currentUser.getUsername());
        refreshedUser.setEmail(currentUser.getEmail());
        refreshedUser.setPassword("$2a$10$new-hash");

        expectedEmailCodeKey = CacheConstants.buildUserCodeKey(currentUser.getId(), currentUser.getEmail(), "2");

        AsyncUserContext.setUser(currentUser);
        AsyncUserContext.setToken("user-access-token");

        ReflectionTestUtils.setField(PoetryUtil.class, "staticCacheService", cacheService);
        ReflectionTestUtils.setField(PoetryUtil.class, "staticUserMapper", null);
        ReflectionTestUtils.setField(PoetryUtil.class, "staticUserCacheManager", null);
        ReflectionTestUtils.setField(PoetryUtil.class, "staticAuthCookieUtil", null);

        WebInfo webInfo = new WebInfo();
        webInfo.setWebName("POETIZE");

        when(cacheService.getCachedAdminUser()).thenReturn(adminUser);
        when(cacheService.getCachedWebInfo()).thenReturn(webInfo);
        when(mailUtil.isEmailConfigured()).thenReturn(true);
        when(mailUtil.getMailText()).thenReturn("%s %s %s %s %s %s");
        when(sysConfigService.getConfigValueByKey("user.code.format")).thenReturn("验证码：%s");

        @SuppressWarnings("unchecked")
        LambdaQueryChainWrapper<User> queryWrapper =
                mock(LambdaQueryChainWrapper.class, Answers.RETURNS_SELF);
        doReturn(queryWrapper).when(queryWrapper).eq(any(), any());
        when(queryWrapper.one()).thenReturn(refreshedUser);
        doReturn(queryWrapper).when(service).lambdaQuery();
        doReturn(true).when(service).updateById(any(User.class));
    }

    @AfterEach
    void tearDown() {
        AsyncUserContext.clear();
        ReflectionTestUtils.setField(PoetryUtil.class, "staticCacheService", null);
        ReflectionTestUtils.setField(PoetryUtil.class, "staticUserMapper", null);
        ReflectionTestUtils.setField(PoetryUtil.class, "staticUserCacheManager", null);
        ReflectionTestUtils.setField(PoetryUtil.class, "staticAuthCookieUtil", null);
    }

    @Test
    void getCode_shouldStorePasswordVerificationCodeWithEmailBasedCacheKey() {
        PoetryResult result = service.getCode(2);

        assertTrue(result.isSuccess());
        verify(cacheService).set(eq(CacheConstants.buildCodeMailCountKey(currentUser.getEmail())),
                eq(1), eq((long) CommonConst.CODE_EXPIRE));
        verify(cacheService).set(eq(expectedEmailCodeKey), any(), eq(300L));
        verify(mailUtil).sendMailMessage(eq(java.util.List.of(currentUser.getEmail())), any(), any());
    }

    @Test
    void passwordChangeCodeCacheKey_shouldMatchBetweenSendAndVerify() {
        service.getCode(2);

        when(passwordService.decryptFromFrontend("newCipher")).thenReturn("newPassword");
        when(passwordService.decryptFromFrontend("oldCipher")).thenReturn("oldPassword");
        when(passwordService.matches("oldPassword", currentUser.getPassword())).thenReturn(true);
        when(passwordService.isPasswordValid("newPassword")).thenReturn(true);
        when(passwordService.encodeBCrypt("newPassword")).thenReturn("$2a$10$new-hash");
        when(cacheService.get(expectedEmailCodeKey)).thenReturn("654321");

        PoetryResult<UserVO> result = service.updateSecretInfo("oldCipher", 3, "654321", "newCipher");

        assertTrue(result.isSuccess());
        verify(cacheService).set(eq(expectedEmailCodeKey), any(), eq(300L));
        verify(cacheService).get(eq(expectedEmailCodeKey));
    }

    @Test
    void updateSecretInfo_shouldChangePasswordAndConsumeCodeWhenOldPasswordAndCodeAreValid() {
        when(passwordService.decryptFromFrontend("newCipher")).thenReturn("newPassword");
        when(passwordService.decryptFromFrontend("oldCipher")).thenReturn("oldPassword");
        when(passwordService.matches("oldPassword", currentUser.getPassword())).thenReturn(true);
        when(passwordService.isPasswordValid("newPassword")).thenReturn(true);
        when(passwordService.encodeBCrypt("newPassword")).thenReturn("$2a$10$new-hash");
        when(cacheService.get(expectedEmailCodeKey)).thenReturn("654321");

        PoetryResult<UserVO> result = service.updateSecretInfo("oldCipher", 3, "654321", "newCipher");

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(currentUser.getEmail(), result.getData().getEmail());

        ArgumentCaptor<User> updatedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(service).updateById(updatedUserCaptor.capture());
        assertEquals(currentUser.getId(), updatedUserCaptor.getValue().getId());
        assertEquals("$2a$10$new-hash", updatedUserCaptor.getValue().getPassword());

        verify(cacheService).deleteKey(expectedEmailCodeKey);
        verify(cacheService).evictAllUserTokens(currentUser.getId());
        verify(cacheService).evictUser(currentUser.getId());
    }

    @Test
    void updateSecretInfo_shouldRejectWrongVerificationCode() {
        when(passwordService.decryptFromFrontend("newCipher")).thenReturn("newPassword");
        when(passwordService.decryptFromFrontend("oldCipher")).thenReturn("oldPassword");
        when(passwordService.matches("oldPassword", currentUser.getPassword())).thenReturn(true);
        when(passwordService.isPasswordValid("newPassword")).thenReturn(true);
        when(cacheService.get(expectedEmailCodeKey)).thenReturn("111111");

        PoetryResult<UserVO> result = service.updateSecretInfo("oldCipher", 3, "654321", "newCipher");

        assertFalse(result.isSuccess());
        assertEquals("验证码错误！", result.getMessage());
        verify(service, never()).updateById(any(User.class));
        verify(cacheService, never()).evictAllUserTokens(any());
        verify(cacheService, never()).deleteKey(expectedEmailCodeKey);
    }

    @Test
    void updateSecretInfo_shouldRejectReusedVerificationCodeAfterSuccessfulPasswordChange() {
        when(passwordService.decryptFromFrontend("newCipher")).thenReturn("newPassword");
        when(passwordService.decryptFromFrontend("oldCipher")).thenReturn("oldPassword");
        when(passwordService.matches("oldPassword", currentUser.getPassword())).thenReturn(true);
        when(passwordService.isPasswordValid("newPassword")).thenReturn(true);
        when(passwordService.encodeBCrypt("newPassword")).thenReturn("$2a$10$new-hash");
        when(cacheService.get(expectedEmailCodeKey)).thenReturn("654321").thenReturn(null);

        PoetryResult<UserVO> firstResult = service.updateSecretInfo("oldCipher", 3, "654321", "newCipher");
        PoetryResult<UserVO> secondResult = service.updateSecretInfo("oldCipher", 3, "654321", "newCipher");

        assertTrue(firstResult.isSuccess());
        assertFalse(secondResult.isSuccess());
        assertEquals("验证码错误！", secondResult.getMessage());
        verify(service, times(1)).updateById(any(User.class));
        verify(cacheService, times(1)).deleteKey(expectedEmailCodeKey);
    }

    @Test
    void updateSecretInfo_shouldRejectWrongOldPasswordBeforeCheckingVerificationCode() {
        when(passwordService.decryptFromFrontend("newCipher")).thenReturn("newPassword");
        when(passwordService.decryptFromFrontend("oldCipher")).thenReturn("wrongOldPassword");
        when(passwordService.matches("wrongOldPassword", currentUser.getPassword())).thenReturn(false);

        PoetryResult<UserVO> result = service.updateSecretInfo("oldCipher", 3, "654321", "newCipher");

        assertFalse(result.isSuccess());
        assertEquals("旧密码错误！", result.getMessage());
        verify(cacheService, never()).get(expectedEmailCodeKey);
        verify(service, never()).updateById(any(User.class));
    }

    @Test
    void getCode_shouldRejectPasswordChangeWhenUserHasNoBoundEmail() {
        currentUser.setEmail(null);

        PoetryResult result = service.getCode(2);

        assertFalse(result.isSuccess());
        assertEquals("请先绑定邮箱！", result.getMessage());
        verify(cacheService, never()).set(eq(expectedEmailCodeKey), any(), anyLong());
        verify(mailUtil, never()).sendMailMessage(any(), anyString(), anyString());
    }
}
