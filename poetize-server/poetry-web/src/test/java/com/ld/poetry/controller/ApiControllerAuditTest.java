package com.ld.poetry.controller;

import com.ld.poetry.aop.AuditLog;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.entity.WebInfo;
import com.ld.poetry.handle.PoetryRuntimeException;
import com.ld.poetry.service.CacheService;
import com.ld.poetry.service.SysAuditLogService;
import com.ld.poetry.service.WebInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiControllerAuditTest {

    private static final String API_KEY = "test-api-key";
    private static final String CLIENT_IP = "203.0.113.10";

    private static final List<AuditExpectation> WRITE_ENDPOINTS = List.of(
            new AuditExpectation("createArticle", "/article/create", "ARTICLE_CREATE", "ARTICLE", "", "API创建文章"),
            new AuditExpectation("createArticleAsync", "/article/createAsync", "ARTICLE_CREATE_ASYNC", "ARTICLE", "", "API提交文章创建任务"),
            new AuditExpectation("updateArticle", "/article/update", "ARTICLE_UPDATE", "ARTICLE", "id", "API更新文章"),
            new AuditExpectation("updateArticleAsync", "/article/updateAsync", "ARTICLE_UPDATE_ASYNC", "ARTICLE", "id", "API提交文章更新任务"),
            new AuditExpectation("deleteArticleToTrash", "/article/delete", "ARTICLE_DELETE", "ARTICLE", "id", "API删除文章（移入回收站）"),
            new AuditExpectation("restoreArticleFromTrash", "/article/restore", "ARTICLE_RESTORE", "ARTICLE", "id", "API从回收站恢复文章"),
            new AuditExpectation("restoreArticleVersion", "/article/restoreVersion", "ARTICLE_VERSION_RESTORE", "ARTICLE", "articleId", "API恢复文章历史版本"),
            new AuditExpectation("deleteResourceToTrash", "/resource/delete", "RESOURCE_DELETE", "RESOURCE", "id", "API删除资源（移入回收站）"),
            new AuditExpectation("restoreResourceFromTrash", "/resource/restore", "RESOURCE_TRASH_RESTORE", "RESOURCE", "id", "API从回收站恢复资源"),
            new AuditExpectation("restoreResourceBackup", "/resource/restoreBackup", "RESOURCE_BACKUP_RESTORE", "RESOURCE_TRASH", "id", "API恢复替换前的旧版文件"),
            new AuditExpectation("activateArticleTheme", "/article-theme/activate", "PLUGIN_ACTIVE_SET", "PLUGIN", "pluginKey", "API切换文章主题"),
            new AuditExpectation("updateSeoConfig", "/seo/config", "SEO_CONFIG_UPDATE", "SEO", "", "API更新SEO配置"),
            new AuditExpectation("updateSeoSitemap", "/seo/sitemap/update", "SEO_SITEMAP_UPDATE", "SEO", "", "API触发站点地图更新"),
            new AuditExpectation("configurePaymentPlugin", "/payment/plugin/configure", "PAYMENT_CONFIG_UPDATE", "PLUGIN", "pluginKey", "API配置支付插件"),
            new AuditExpectation("testPaymentPluginConnection", "/payment/plugin/testConnection", "PAYMENT_CONNECTION_TEST", "PLUGIN", "pluginKey", "API测试支付插件连接"),
            new AuditExpectation("uploadResource", "/resource/upload", "RESOURCE_UPLOAD", "RESOURCE", "relativePath", "API上传资源"),
            new AuditExpectation("saveComment", "/comment/save", "COMMENT_SAVE", "COMMENT", "", "API发表或回复评论"),
            new AuditExpectation("saveTranslation", "/translation/save", "ARTICLE_TRANSLATION_SAVE", "ARTICLE", "articleId", "API保存文章翻译"),
            new AuditExpectation("deleteTranslation", "/translation/delete", "ARTICLE_TRANSLATION_DELETE", "ARTICLE", "articleId", "API删除文章翻译"),
            new AuditExpectation("regenerateTranslation", "/translation/regenerate", "ARTICLE_TRANSLATION_REGENERATE", "ARTICLE", "articleId", "API重新生成文章翻译"),
            new AuditExpectation("updateArticleSection", "/article/updateSection", "ARTICLE_SECTION_UPDATE", "ARTICLE", "articleId", "API更新文章章节"),
            new AuditExpectation("createSort", "/sort/create", "SORT_SAVE", "SORT", "id", "API新增分类"),
            new AuditExpectation("updateSort", "/sort/update", "SORT_UPDATE", "SORT", "id", "API更新分类"),
            new AuditExpectation("deleteSort", "/sort/delete", "SORT_DELETE", "SORT", "id", "API删除分类"),
            new AuditExpectation("createLabel", "/label/create", "LABEL_SAVE", "LABEL", "id", "API新增标签"),
            new AuditExpectation("updateLabel", "/label/update", "LABEL_UPDATE", "LABEL", "id", "API更新标签"),
            new AuditExpectation("deleteLabel", "/label/delete", "LABEL_DELETE", "LABEL", "id", "API删除标签")
    );

    @Mock
    private CacheService cacheService;

    @Mock
    private WebInfoService webInfoService;

    @Mock
    private SysAuditLogService sysAuditLogService;

    @InjectMocks
    private ApiController controller;

    @ParameterizedTest(name = "{0}")
    @MethodSource("writeEndpoints")
    void postWriteEndpointShouldHaveExpectedAuditMetadata(AuditExpectation expected) {
        Method method = findMethod(expected.methodName());
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.method()).containsExactly(RequestMethod.POST);
        assertThat(mapping.path()).containsExactly(expected.path());

        AuditLog audit = method.getAnnotation(AuditLog.class);
        assertThat(audit).as(expected.methodName()).isNotNull();
        assertAll(
                () -> assertThat(audit.type()).isEqualTo("OPERATION"),
                () -> assertThat(audit.action()).isEqualTo(expected.action()),
                () -> assertThat(audit.targetType()).isEqualTo(expected.targetType()),
                () -> assertThat(audit.targetIdParam()).isEqualTo(expected.targetIdParam()),
                () -> assertThat(audit.summary()).isEqualTo(expected.summary()).startsWith("API")
        );
        if (expected.methodName().endsWith("Async")) {
            assertThat(audit.summary()).contains("提交", "任务").doesNotContain("完成");
        }
    }

    @Test
    void allPostMappingsShouldBeCoveredExceptCommentList() {
        List<String> expectedMethods = Stream.concat(
                WRITE_ENDPOINTS.stream().map(AuditExpectation::methodName),
                Stream.of("getCommentList")
        ).toList();
        assertThat(mappedMethods(RequestMethod.POST).map(Method::getName).toList())
                .containsExactlyInAnyOrderElementsOf(expectedMethods);

        Method commentList = findMethod("getCommentList");
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(commentList, RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.path()).containsExactly("/comment/list");
        assertThat(commentList.getAnnotation(AuditLog.class)).isNull();
    }

    @ParameterizedTest(name = "GET {0} is not audited")
    @MethodSource("getEndpoints")
    void getEndpointShouldNotHaveAuditAnnotation(Method method) {
        assertThat(method.getAnnotation(AuditLog.class)).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {CLIENT_IP, "203.0.113.0/24"})
    void authenticationShouldBeMarkedTrueOnlyAfterAllChecksPass(String whitelist) {
        WebInfo webInfo = webInfo(Boolean.TRUE, whitelist);
        MockHttpServletRequest request = request(API_KEY, Boolean.TRUE);
        stubCachedWebInfo(webInfo, request);

        WebInfo result = ReflectionTestUtils.invokeMethod(controller, "validateApiKey", request);

        assertThat(result).isSameAs(webInfo);
        assertThat(request.getAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
        assertOnlyAuditAttribute(request);
        verify(cacheService).getCachedWebInfo();
        verifyNoMoreInteractions(cacheService);
        verifyNoInteractions(webInfoService);
        verifyNoInteractions(sysAuditLogService);
    }

    @ParameterizedTest(name = "{0}, initial marker={1}")
    @MethodSource("rejectedAuthentication")
    void rejectedAuthenticationShouldLeaveMarkerFalse(Rejection rejection, Boolean initialMarker) {
        WebInfo webInfo = webInfo(rejection.enabled(), rejection.whitelist());
        MockHttpServletRequest request = request(rejection.key(), initialMarker);
        stubCachedWebInfo(webInfo, request);

        PoetryRuntimeException exception = assertThrows(PoetryRuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(controller, "validateApiKey", request));

        assertThat(exception.getMessage()).isEqualTo(rejection.message());
        assertThat(request.getAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE)).isEqualTo(Boolean.FALSE);
        assertOnlyAuditAttribute(request);
        verify(cacheService).getCachedWebInfo();
        verifyNoMoreInteractions(cacheService);
        verifyNoInteractions(webInfoService);
        verify(sysAuditLogService).recordSecurityDedup(eq("API_AUTH_REJECTED"), eq(false), isNull(), isNull(),
                isNull(), eq("开放 API 认证未通过"), anyMap(), eq(CLIENT_IP), eq(CLIENT_IP), eq(0L));
    }

    @Test
    void readOnlyEndpointRejectionIsAuditedDespiteNoAuditAnnotation() {
        // /categories 是只读端点，未加 @AuditLog，原先认证拒绝不会被审计切面覆盖
        assertThat(findMethod("getCategoryList").getAnnotation(AuditLog.class)).isNull();
        WebInfo webInfo = webInfo(Boolean.TRUE, null);
        MockHttpServletRequest request = request("wrong-api-key", null);
        stubCachedWebInfo(webInfo, request);

        PoetryResult result = controller.getCategoryList(request);

        assertThat(result.getCode()).isNotEqualTo(200);
        verify(sysAuditLogService).recordSecurityDedup(eq("API_AUTH_REJECTED"), eq(false), isNull(), isNull(),
                isNull(), eq("开放 API 认证未通过"), anyMap(), eq(CLIENT_IP), eq(CLIENT_IP), eq(0L));
    }

    private void stubCachedWebInfo(WebInfo webInfo, MockHttpServletRequest request) {
        when(cacheService.getCachedWebInfo()).thenAnswer(invocation -> {
            // 读取配置前就必须清除旧的成功标记，认证途中不得提前标记成功。
            assertThat(request.getAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE)).isEqualTo(Boolean.FALSE);
            return webInfo;
        });
    }

    private static MockHttpServletRequest request(String key, Boolean initialMarker) {
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public String getHeader(String name) {
                assertThat(getAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE)).isEqualTo(Boolean.FALSE);
                return super.getHeader(name);
            }

            @Override
            public String getRemoteAddr() {
                assertThat(getAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE)).isEqualTo(Boolean.FALSE);
                return super.getRemoteAddr();
            }
        };
        request.setRemoteAddr(CLIENT_IP);
        if (key != null) {
            request.addHeader("X-API-KEY", key);
        }
        if (initialMarker != null) {
            request.setAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE, initialMarker);
        }
        return request;
    }

    private static WebInfo webInfo(Boolean enabled, String whitelist) {
        WebInfo webInfo = new WebInfo();
        webInfo.setApiEnabled(enabled);
        webInfo.setApiKey(API_KEY);
        webInfo.setApiIpWhitelist(whitelist);
        return webInfo;
    }

    private static void assertOnlyAuditAttribute(MockHttpServletRequest request) {
        assertThat(Collections.list(request.getAttributeNames()))
                .containsExactly(CommonConst.API_AUTHENTICATED_ATTRIBUTE);
    }

    private static Method findMethod(String name) {
        List<Method> methods = Arrays.stream(ApiController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .toList();
        assertThat(methods).hasSize(1);
        return methods.get(0);
    }

    private static Stream<Method> mappedMethods(RequestMethod requestMethod) {
        return Arrays.stream(ApiController.class.getDeclaredMethods()).filter(method -> {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            return mapping != null && Arrays.asList(mapping.method()).contains(requestMethod);
        });
    }

    private static Stream<AuditExpectation> writeEndpoints() {
        return WRITE_ENDPOINTS.stream();
    }

    private static Stream<Method> getEndpoints() {
        return mappedMethods(RequestMethod.GET);
    }

    private static Stream<Arguments> rejectedAuthentication() {
        return Stream.of(
                new Rejection(Boolean.FALSE, API_KEY, CLIENT_IP, "API未启用"),
                new Rejection(null, API_KEY, CLIENT_IP, "API未启用"),
                new Rejection(Boolean.TRUE, null, CLIENT_IP, "无效的API密钥"),
                new Rejection(Boolean.TRUE, "   ", CLIENT_IP, "无效的API密钥"),
                new Rejection(Boolean.TRUE, "wrong-api-key", CLIENT_IP, "无效的API密钥"),
                new Rejection(Boolean.TRUE, API_KEY, "203.0.113.11", "当前IP不在API白名单中")
        ).flatMap(rejection -> Stream.<Boolean>of(null, Boolean.FALSE, Boolean.TRUE)
                .map(initialMarker -> Arguments.of(rejection, initialMarker)));
    }

    private record AuditExpectation(String methodName, String path, String action, String targetType,
                                    String targetIdParam, String summary) {
    }

    private record Rejection(Boolean enabled, String key, String whitelist, String message) {
    }
}
