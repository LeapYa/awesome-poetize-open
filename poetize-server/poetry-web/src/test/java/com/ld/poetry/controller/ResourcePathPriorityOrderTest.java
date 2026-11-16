package com.ld.poetry.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ld.poetry.config.AsyncUserContext;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.dao.ResourcePathMapper;
import com.ld.poetry.entity.ResourcePath;
import com.ld.poetry.entity.User;
import com.ld.poetry.service.CacheService;
import com.ld.poetry.utils.PoetryUtil;
import com.ld.poetry.vo.BaseRequestVO;
import com.ld.poetry.vo.ResourcePathVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 资源聚合排序口径：优先级小的在前，未填写(NULL)的排最后。
 * MySQL/MariaDB 的 ASC 默认把 NULL 排最前，所以锁住传给 mapper 的 ORDER BY 表达式，
 * 避免有人改成 orderByAsc(priority) 后把全部未填排序号的存量数据顶到最前面。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResourcePathPriorityOrderTest {

    @Mock
    private ResourcePathMapper resourcePathMapper;

    @Mock
    private CacheService cacheService;

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ResourcePath.class);
    }

    @BeforeEach
    void setUp() {
        User admin = new User();
        admin.setId(1);
        admin.setUsername("boss");
        AsyncUserContext.setUser(admin);
        when(cacheService.getCachedAdminUser()).thenReturn(admin);

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
    @SuppressWarnings({"unchecked", "rawtypes"})
    void friendListOrdersByPriorityWithNullsLast() {
        FriendController controller = new FriendController();
        ReflectionTestUtils.setField(controller, "resourcePathMapper", resourcePathMapper);
        ReflectionTestUtils.setField(controller, "cacheService", cacheService);
        // Mockito 对 Map 返回值默认给空 Map（非 null），会命中缓存分支直接返回，必须显式置空
        when(cacheService.getCachedFriendList()).thenReturn(null);
        when(resourcePathMapper.selectList(any()))
                .thenReturn(List.of(resourcePath("已填排序号", 3), resourcePath("未填排序号", null)));

        Map<String, ?> data = controller.loadFriendListData();

        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(resourcePathMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.endsWith(CommonConst.RESOURCE_PATH_ORDER_PRIORITY_CREATE_TIME), sql);
        assertTrue(sql.indexOf("(priority IS NULL)") < sql.indexOf("priority,"), sql);

        List<ResourcePathVO> friends = (List<ResourcePathVO>) data.get(CommonConst.DEFAULT_FRIEND);
        assertEquals(3, friends.get(0).getPriority());
        assertNull(friends.get(1).getPriority());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void favoritesKeepTitleTiebreaker() {
        CollectController controller = new CollectController();
        ReflectionTestUtils.setField(controller, "resourcePathMapper", resourcePathMapper);
        ReflectionTestUtils.setField(controller, "cacheService", cacheService);
        when(resourcePathMapper.selectList(any())).thenReturn(List.of());

        controller.listCollect();

        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(resourcePathMapper).selectList(captor.capture());
        assertTrue(captor.getValue().getSqlSegment().endsWith(CommonConst.RESOURCE_PATH_ORDER_PRIORITY_TITLE),
                captor.getValue().getSqlSegment());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void adminListUsesPriorityOrderWhenNoColumnOrderRequested() {
        ResourceAggregationController controller = newAdminListController();
        when(resourcePathMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        BaseRequestVO request = new BaseRequestVO();
        request.setResourceType(CommonConst.RESOURCE_PATH_TYPE_FRIEND);
        controller.listResourcePath(request);

        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(resourcePathMapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());
        Page<ResourcePath> page = pageCaptor.getValue();
        assertTrue(wrapperCaptor.getValue().getSqlSegment()
                .endsWith(CommonConst.RESOURCE_PATH_ORDER_PRIORITY_CREATE_TIME), wrapperCaptor.getValue().getSqlSegment());
        // Page 带 OrderItem 时分页插件会覆盖 last() 里的 ORDER BY，默认排序必须留空
        assertTrue(page.orders().isEmpty());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void explicitColumnOrderOnlyBreaksPriorityTies() {
        ResourceAggregationController controller = newAdminListController();
        when(resourcePathMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        BaseRequestVO request = new BaseRequestVO();
        request.setResourceType(CommonConst.RESOURCE_PATH_TYPE_FUNNY);
        // 趣味页固定传 order=title，优先级仍须排在它前面
        request.setOrder("title");
        request.setDesc(false);
        controller.listResourcePath(request);

        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(resourcePathMapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment()
                .endsWith(CommonConst.RESOURCE_PATH_ORDER_PRIORITY_PREFIX + "title ASC"),
                wrapperCaptor.getValue().getSqlSegment());
        assertTrue(pageCaptor.getValue().orders().isEmpty());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void unknownOrderColumnFallsBackToDefaultOrder() {
        ResourceAggregationController controller = newAdminListController();
        when(resourcePathMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        BaseRequestVO request = new BaseRequestVO();
        request.setResourceType(CommonConst.RESOURCE_PATH_TYPE_FRIEND);
        request.setOrder("id; DROP TABLE resource_path");
        controller.listResourcePath(request);

        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(resourcePathMapper).selectPage(any(), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment()
                .endsWith(CommonConst.RESOURCE_PATH_ORDER_PRIORITY_CREATE_TIME), wrapperCaptor.getValue().getSqlSegment());
        assertFalse(wrapperCaptor.getValue().getSqlSegment().contains("DROP"));
    }

    private ResourceAggregationController newAdminListController() {
        ResourceAggregationController controller = new ResourceAggregationController();
        ReflectionTestUtils.setField(controller, "resourcePathMapper", resourcePathMapper);
        ReflectionTestUtils.setField(controller, "cacheService", cacheService);
        return controller;
    }

    private ResourcePath resourcePath(String title, Integer priority) {
        ResourcePath resourcePath = new ResourcePath();
        resourcePath.setTitle(title);
        resourcePath.setClassify(CommonConst.DEFAULT_FRIEND);
        resourcePath.setType(CommonConst.RESOURCE_PATH_TYPE_FRIEND);
        resourcePath.setStatus(Boolean.TRUE);
        resourcePath.setPriority(priority);
        return resourcePath;
    }
}