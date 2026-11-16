package com.ld.poetry.config;

import com.ld.poetry.controller.FriendController;
import com.ld.poetry.controller.ResourceAggregationController;
import com.ld.poetry.service.CacheService;
import com.ld.poetry.utils.CommonQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.inOrder;

/**
 * 永久聚合缓存的启动预热必须先 evict 再回填。
 * 这几个 load 方法都是「缓存未命中才查 DB」，不先 evict 的话，
 * 空库启动或升级恢复数据留下的旧快照会一直活着，重启也救不回来。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PoetryApplicationRunnerCacheRebuildTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private ResourceAggregationController resourceAggregationController;

    @Mock
    private FriendController friendController;

    @Mock
    private CommonQuery commonQuery;

    @InjectMocks
    private PoetryApplicationRunner runner;

    @Test
    void asideBootstrapWarmupEvictsBeforeReload() {
        ReflectionTestUtils.invokeMethod(runner, "initAsideBootstrapCache");

        InOrder order = inOrder(cacheService, resourceAggregationController);
        order.verify(cacheService).evictAsideBootstrap();
        order.verify(resourceAggregationController).loadAsideBootstrapData();
    }

    @Test
    void friendListWarmupEvictsBeforeReload() {
        ReflectionTestUtils.invokeMethod(runner, "initFriendListCache");

        InOrder order = inOrder(cacheService, friendController);
        order.verify(cacheService).evictFriendList();
        order.verify(friendController).loadFriendListData();
    }

    @Test
    void sortInfoWarmupEvictsBeforeReload() {
        ReflectionTestUtils.invokeMethod(runner, "initSortInfoCache");

        InOrder order = inOrder(cacheService, commonQuery);
        order.verify(cacheService).evictSortInfo();
        order.verify(commonQuery).getSortInfo();
    }
}