package com.ld.poetry.utils.mail;

import com.ld.poetry.config.AsyncUserContext;
import com.ld.poetry.constants.CacheConstants;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.entity.ResourcePath;
import com.ld.poetry.entity.User;
import com.ld.poetry.entity.WebInfo;
import com.ld.poetry.service.CacheService;
import com.ld.poetry.utils.PoetryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MailSendUtilFriendMailTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private MailUtil mailUtil;

    @InjectMocks
    private MailSendUtil mailSendUtil;

    private User adminUser;
    private ResourcePath friend;
    private String mailCountKey;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1);
        adminUser.setUsername("boss");
        adminUser.setEmail("boss@example.com");

        User applicant = new User();
        applicant.setId(1001);
        applicant.setUsername("申请同学");
        AsyncUserContext.setUser(applicant);

        WebInfo webInfo = new WebInfo();
        webInfo.setWebName("小站");

        friend = new ResourcePath();
        friend.setTitle("某人的博客");
        friend.setIntroduction("写点东西");
        friend.setUrl("https://example.com");

        mailCountKey = CacheConstants.CACHE_PREFIX + "friend:mail:" + adminUser.getEmail();

        when(cacheService.getCachedAdminUser()).thenReturn(adminUser);
        when(cacheService.getCachedWebInfo()).thenReturn(webInfo);
        when(mailUtil.getMailText()).thenReturn("%s|%s|%s|%s|%s|%s");

        ReflectionTestUtils.setField(PoetryUtil.class, "staticCacheService", cacheService);
        ReflectionTestUtils.setField(PoetryUtil.class, "staticUserMapper", null);
        ReflectionTestUtils.setField(PoetryUtil.class, "staticUserCacheManager", null);
    }

    @AfterEach
    void tearDown() {
        AsyncUserContext.clear();
        ReflectionTestUtils.setField(PoetryUtil.class, "staticCacheService", null);
    }

    @Test
    void sendFriendMail_shouldNotifyAdminWithFriendDetails() {
        mailSendUtil.sendFriendMail(friend);

        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(mailUtil).sendMailMessage(eq(List.of("boss@example.com")),
                eq("您有一封来自小站的回执！"), content.capture());
        assertTrue(content.getValue().contains("你收到来自 申请同学 的友链申请"));
        assertTrue(content.getValue().contains("某人的博客"));
        assertTrue(content.getValue().contains("写点东西"));
        assertTrue(content.getValue().contains("https://example.com"));

        ArgumentCaptor<Object> counter = ArgumentCaptor.forClass(Object.class);
        verify(cacheService).set(eq(mailCountKey), counter.capture(), eq(CommonConst.CODE_EXPIRE));
        assertEquals(1, ((AtomicInteger) counter.getValue()).get());
    }

    @Test
    void sendFriendMail_shouldSkipWhenAdminEmailMissing() {
        adminUser.setEmail(null);

        mailSendUtil.sendFriendMail(friend);

        verify(mailUtil, never()).sendMailMessage(any(), anyString(), anyString());
    }

    @Test
    void sendFriendMail_shouldSkipWhenApplicantIsAdmin() {
        AsyncUserContext.setUser(adminUser);

        mailSendUtil.sendFriendMail(friend);

        verify(mailUtil, never()).sendMailMessage(any(), anyString(), anyString());
    }

    @Test
    void sendFriendMail_shouldSkipWhenDailyLimitReached() {
        when(cacheService.get(mailCountKey)).thenReturn(new AtomicInteger(CommonConst.COMMENT_IM_MAIL_COUNT));

        mailSendUtil.sendFriendMail(friend);

        verify(mailUtil, never()).sendMailMessage(any(), anyString(), anyString());
    }
}