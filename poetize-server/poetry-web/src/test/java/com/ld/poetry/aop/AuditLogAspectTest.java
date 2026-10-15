package com.ld.poetry.aop;

import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.service.SysAuditLogService;
import com.ld.poetry.vo.ArticleVO;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditLogAspectTest {

    private AuditLogAspect aspect;
    private SysAuditLogService service;
    private ProceedingJoinPoint joinPoint;
    private AuditLog annotation;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        aspect = new AuditLogAspect();
        service = mock(SysAuditLogService.class);
        ReflectionTestUtils.setField(aspect, "sysAuditLogService", service);
        joinPoint = mock(ProceedingJoinPoint.class);
        annotation = mock(AuditLog.class);
        when(annotation.type()).thenReturn("OPERATION");
        when(annotation.action()).thenReturn("ARTICLE_UPDATE");
        when(annotation.targetType()).thenReturn("ARTICLE");
        when(annotation.targetIdParam()).thenReturn("id");
        when(annotation.summary()).thenReturn("API更新文章");
        when(joinPoint.getArgs()).thenReturn(new Object[]{Map.of("id", 42, "content", "private article body")});
        request = new MockHttpServletRequest("POST", "/api/article/update");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void apiTaskSubmissionKeepsOnlySafeMetadata() throws Throwable {
        request.setAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE, true);
        when(annotation.action()).thenReturn("ARTICLE_UPDATE_ASYNC");
        when(annotation.summary()).thenReturn("API提交文章更新任务");
        Map<String, Object> data = Map.of("taskId", "task-42", "articleContent", "private article body",
                "pluginConfig", Map.of("apiKey", "private-key"));
        PoetryResult<?> response = PoetryResult.success(data);
        when(joinPoint.proceed()).thenReturn(response);

        assertSame(response, aspect.around(joinPoint, annotation));

        Map<String, Object> detail = capturedDetail("ARTICLE_UPDATE_ASYNC", true, "42", "API提交文章更新任务");
        assertEquals(Map.of("method", "POST", "resultCode", 200, "taskId", "task-42"), detail);
    }

    @Test
    void apiCreationUsesReturnedArticleId() throws Throwable {
        request.setAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE, true);
        when(annotation.action()).thenReturn("ARTICLE_CREATE");
        when(annotation.targetIdParam()).thenReturn("");
        when(annotation.summary()).thenReturn("API创建文章");
        when(joinPoint.proceed()).thenReturn(PoetryResult.success(Map.of("articleId", 99)));

        aspect.around(joinPoint, annotation);

        capturedDetail("ARTICLE_CREATE", true, "99", "API创建文章");
    }

    @Test
    void apiBodyTargetCannotBeOverriddenByUnrelatedQueryParameter() throws Throwable {
        request.setAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE, true);
        request.addParameter("id", "999");
        ArticleVO article = new ArticleVO();
        article.setId(42);
        when(joinPoint.getArgs()).thenReturn(new Object[]{article, request});
        when(joinPoint.proceed()).thenReturn(PoetryResult.success());

        aspect.around(joinPoint, annotation);

        capturedDetail("ARTICLE_UPDATE", true, "42", "API更新文章");
    }

    @Test
    void apiFailureDoesNotPersistUpstreamSecrets() throws Throwable {
        request.setAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE, true);
        when(joinPoint.proceed()).thenReturn(PoetryResult.fail("upstream private-key rejected"));

        aspect.around(joinPoint, annotation);

        Map<String, Object> detail = capturedDetail("ARTICLE_UPDATE", false, "42", "API更新文章");
        assertFalse(detail.containsKey("message"));
        assertTrue(detail.containsKey("resultCode"));
        assertFalse(detail.toString().contains("private-key"));
    }

    @Test
    void deniedAuthenticationStillReachesAuditServiceWithoutPayload() throws Throwable {
        request.setAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE, false);
        when(joinPoint.proceed()).thenReturn(PoetryResult.fail("无效的API密钥"));

        aspect.around(joinPoint, annotation);

        Map<String, Object> detail = capturedDetail("ARTICLE_UPDATE", false, "42", "API更新文章");
        assertEquals(2, detail.size());
        assertFalse(detail.containsKey("message"));
    }

    @Test
    void apiDryRunIsExplicitlyMarkedAsPreview() throws Throwable {
        request.setAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE, true);
        when(annotation.action()).thenReturn("ARTICLE_SECTION_UPDATE");
        when(annotation.targetIdParam()).thenReturn("articleId");
        when(joinPoint.getArgs()).thenReturn(new Object[]{Map.of("articleId", 42, "dryRun", true)});
        when(joinPoint.proceed()).thenReturn(PoetryResult.success(Map.of(
                "articleId", 42, "changed", true, "originalContent", "old private body", "updatedContent", "new private body")));

        aspect.around(joinPoint, annotation);

        Map<String, Object> detail = capturedDetail("ARTICLE_SECTION_UPDATE", true, "42", "API预览文章章节更新（未写入）");
        assertEquals(Map.of("method", "POST", "resultCode", 200, "changed", true, "dryRun", true), detail);
    }

    @Test
    void unchangedSectionRequestDoesNotPretendToChangeContent() throws Throwable {
        request.setAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE, true);
        when(annotation.action()).thenReturn("ARTICLE_SECTION_UPDATE");
        when(joinPoint.proceed()).thenReturn(PoetryResult.success(Map.of("articleId", 42, "changed", false)));

        aspect.around(joinPoint, annotation);

        Map<String, Object> detail = capturedDetail("ARTICLE_SECTION_UPDATE", true, "42", "API更新文章");
        assertEquals(false, detail.get("changed"));
        assertEquals(false, detail.get("dryRun"));
    }

    @Test
    void exceptionIsAuditedAndRethrownWithoutLeakingApiMessage() throws Throwable {
        request.setAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE, true);
        IllegalStateException failure = new IllegalStateException("upstream private-key");
        when(joinPoint.proceed()).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class, () -> aspect.around(joinPoint, annotation)));

        assertEquals(Map.of("method", "POST"), capturedDetail("ARTICLE_UPDATE", false, "42", "API更新文章"));
    }

    @Test
    void ordinaryAdminAuditKeepsMessagesAndPathTargetResolution() throws Throwable {
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("id", "17"));
        when(joinPoint.proceed()).thenReturn(PoetryResult.fail("文章不存在"));

        aspect.around(joinPoint, annotation);

        Map<String, Object> detail = capturedDetail("ARTICLE_UPDATE", false, "17", "API更新文章");
        assertEquals("文章不存在", detail.get("message"));
    }

    @Test
    void failedAuditCannotChangeControllerResponse() throws Throwable {
        PoetryResult<?> response = PoetryResult.success();
        when(joinPoint.proceed()).thenReturn(response);
        doThrow(new IllegalStateException("storage unavailable")).when(service)
                .recordOperation(anyString(), anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyMap());

        assertSame(response, aspect.around(joinPoint, annotation));
    }

    @Test
    void realAnnotationProxyTriggersAuditAfterApiAuthentication() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new AuditedApi());
        factory.addAspect(aspect);
        AuditedApi proxy = factory.getProxy();

        PoetryResult<?> response = proxy.submit(request);

        assertTrue(response.isSuccess());
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(service).recordOperation(eq("OPERATION"), eq("ARTICLE_CREATE_ASYNC"), eq(true),
                eq("ARTICLE"), isNull(), eq("API提交文章创建任务"), captor.capture());
        assertEquals("task-created", captor.getValue().get("taskId"));
        assertEquals(true, request.getAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE));
    }

    static class AuditedApi {
        @AuditLog(action = "ARTICLE_CREATE_ASYNC", targetType = "ARTICLE", summary = "API提交文章创建任务")
        public PoetryResult<?> submit(MockHttpServletRequest request) {
            request.setAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE, true);
            return PoetryResult.success(Map.of("taskId", "task-created"));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedDetail(String action, boolean success, String targetId, String summary) {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(service).recordOperation(eq("OPERATION"), eq(action), eq(success), eq("ARTICLE"), eq(targetId), eq(summary), captor.capture());
        return captor.getValue();
    }
}
