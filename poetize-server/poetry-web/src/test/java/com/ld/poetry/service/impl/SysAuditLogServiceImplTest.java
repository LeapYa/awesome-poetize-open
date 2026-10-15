package com.ld.poetry.service.impl;

import com.ld.poetry.config.AsyncUserContext;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.dao.SysAuditLogMapper;
import com.ld.poetry.entity.SysAuditLog;
import com.ld.poetry.entity.User;
import com.ld.poetry.service.LocationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SysAuditLogServiceImplTest {

    private SysAuditLogServiceImpl service;
    private SysAuditLogMapper mapper;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        service = new SysAuditLogServiceImpl();
        mapper = mock(SysAuditLogMapper.class);
        LocationService locationService = mock(LocationService.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "locationService", locationService);
        when(mapper.insert(any(SysAuditLog.class))).thenReturn(1);
        when(locationService.getLocationByIpOffline(any())).thenReturn("测试地区");
        request = new MockHttpServletRequest("POST", "/api/article/update");
        request.setRemoteAddr("198.51.100.87");
        request.addHeader("User-Agent", "test-api-client");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        AsyncUserContext.clear();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        AsyncUserContext.clear();
    }

    @Test
    void authenticatedApiUsesItsOwnIdentityEvenWithLoggedInUser() {
        setCurrentUser();
        request.setAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE, true);
        Map<String, Object> detail = Map.of("taskId", "task-42");

        service.recordOperation("OPERATION", "ARTICLE_UPDATE_ASYNC", true, "ARTICLE", "42",
                "API提交文章更新任务", detail);

        SysAuditLog log = savedLog();
        assertEquals("开放 API", log.getUsername());
        assertNull(log.getUserId());
        assertEquals("OPERATION", log.getLogType());
        assertTrue(log.getSuccess());
        assertEquals("42", log.getTargetId());
        assertEquals("198.51.100.87", log.getIp());
        assertEquals("test-api-client", log.getUserAgent());
        assertEquals(request.getRequestURI(), log.getRequestUri());
        assertTrue(log.getDetail().contains("\"source\":\"OPEN_API\""));
        assertTrue(log.getDetail().contains("\"authenticated\":true"));
        assertTrue(log.getDetail().contains("\"taskId\":\"task-42\""));
        assertEquals(Map.of("taskId", "task-42"), detail);
    }

    @Test
    void apiAuthRejectionIsNotRecordedByServiceToAvoidDuplicate() {
        setCurrentUser();
        request.setAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE, false);

        service.recordOperation("OPERATION", "ARTICLE_UPDATE", false, "ARTICLE", "42", "API更新文章", Map.of());

        // 认证失败已由 ApiController#validateApiKey 记录，service 不再重复落库
        verifyNoInteractions(mapper);
    }

    @Test
    void apiAuthRejectionDedupSuppressesRepeatedAttemptsFromSameIp() {
        for (int i = 0; i < 3; i++) {
            service.recordSecurityDedup("API_AUTH_REJECTED", false, null, null, null,
                    "开放 API 认证未通过", Map.of("authenticated", false), "198.51.100.87", "198.51.100.87", 0);
        }

        SysAuditLog log = savedLog();
        assertEquals("SECURITY", log.getLogType());
        assertEquals("API_AUTH_REJECTED", log.getAction());
        assertFalse(log.getSuccess());
        assertNull(log.getUserId());
        assertNull(log.getUsername());
        assertTrue(log.getDetail().contains("\"authenticated\":false"));
    }

    @Test
    void apiHeaderAndPathAloneDoNotAuthenticateCaller() {
        request.addHeader("X-API-KEY", "untrusted-test-value");
        request.addHeader(CommonConst.API_AUTHENTICATED_ATTRIBUTE, "true");
        request.addParameter(CommonConst.API_AUTHENTICATED_ATTRIBUTE, "true");

        recordUpdate(false);

        SysAuditLog log = savedLog();
        assertNull(log.getUsername());
        assertNull(log.getUserId());
        assertNull(log.getDetail());
    }

    @Test
    void normalAdminAuditKeepsExistingIdentity() {
        setCurrentUser();
        recordUpdate(true);
        SysAuditLog log = savedLog();
        assertEquals(7, log.getUserId());
        assertEquals("editor", log.getUsername());
        assertNull(log.getDetail());
    }

    @Test
    void authenticatedBusinessFailureRemainsApiOperationFailure() {
        request.setAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE, true);
        recordUpdate(false);
        SysAuditLog log = savedLog();
        assertEquals("OPERATION", log.getLogType());
        assertEquals("ARTICLE_UPDATE", log.getAction());
        assertEquals("开放 API", log.getUsername());
        assertFalse(log.getSuccess());
    }

    @Test
    void auditWithoutRequestStillSupportsExistingBackgroundUser() {
        setCurrentUser();
        RequestContextHolder.resetRequestAttributes();
        recordUpdate(true);
        SysAuditLog log = savedLog();
        assertEquals(7, log.getUserId());
        assertEquals("unknown", log.getIp());
        assertNull(log.getRequestUri());
    }

    private void recordUpdate(boolean success) {
        service.recordOperation("OPERATION", "ARTICLE_UPDATE", success, "ARTICLE", "42", "更新文章", null);
    }

    private SysAuditLog savedLog() {
        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(mapper).insert(captor.capture());
        return captor.getValue();
    }

    private void setCurrentUser() {
        User user = new User();
        user.setId(7);
        user.setUsername("editor");
        AsyncUserContext.setUser(user);
    }
}
