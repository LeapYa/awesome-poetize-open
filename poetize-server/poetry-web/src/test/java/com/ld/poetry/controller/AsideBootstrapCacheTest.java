package com.ld.poetry.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ld.poetry.dao.ResourcePathMapper;
import com.ld.poetry.entity.ResourcePath;
import com.ld.poetry.service.CacheService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 侧边栏首屏聚合缓存的读写口径。
 * 站长没配快捷入口是合法状态，空结果必须照样缓存，否则每次首屏请求都要打 DB。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsideBootstrapCacheTest {

    @Mock
    private ResourcePathMapper resourcePathMapper;

    @Mock
    private CacheService cacheService;

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ResourcePath.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void emptyQuickEntryIsStillCached() {
        ResourceAggregationController controller = newController();
        // Mockito 对 Map 返回值默认给空 Map（非 null），会命中缓存分支，必须显式置空
        when(cacheService.getCachedAsideBootstrap()).thenReturn(null);
        when(resourcePathMapper.selectList(any())).thenReturn(List.of());

        controller.loadAsideBootstrapData();

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(cacheService).cacheAsideBootstrap(captor.capture());
        assertTrue(((List<?>) captor.getValue().get("quickEntryList")).isEmpty());
    }

    @Test
    void cacheHitSkipsDatabase() {
        ResourceAggregationController controller = newController();
        when(cacheService.getCachedAsideBootstrap()).thenReturn(Map.of("quickEntryList", List.of()));

        controller.loadAsideBootstrapData();

        verify(resourcePathMapper, never()).selectList(any());
        verify(cacheService, never()).cacheAsideBootstrap(any());
    }

    private ResourceAggregationController newController() {
        ResourceAggregationController controller = new ResourceAggregationController();
        ReflectionTestUtils.setField(controller, "resourcePathMapper", resourcePathMapper);
        ReflectionTestUtils.setField(controller, "cacheService", cacheService);
        return controller;
    }
}