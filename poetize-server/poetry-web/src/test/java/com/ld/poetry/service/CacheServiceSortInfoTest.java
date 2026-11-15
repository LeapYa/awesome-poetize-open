package com.ld.poetry.service;

import com.ld.poetry.constants.CacheConstants;
import com.ld.poetry.entity.Sort;
import com.ld.poetry.utils.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定空库启动污染分类缓存事故的修复：空分类列表不得写入永久缓存，
 * 且读取时把已缓存的空列表视为未命中，让脏数据无需手动 DEL 即可自愈。
 */
class CacheServiceSortInfoTest {

    private CacheService newCacheService(RedisUtil redisUtil) {
        CacheService cacheService = new CacheService();
        ReflectionTestUtils.setField(cacheService, "redisUtil", redisUtil);
        return cacheService;
    }

    @Test
    void doesNotCacheEmptySortInfo() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        CacheService cacheService = newCacheService(redisUtil);

        cacheService.cacheSortInfo(new ArrayList<Sort>());

        verify(redisUtil, never()).set(any(), any(), anyLong());
    }

    @Test
    void doesNotCacheNullSortInfo() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        CacheService cacheService = newCacheService(redisUtil);

        cacheService.cacheSortInfo(null);

        verify(redisUtil, never()).set(any(), any(), anyLong());
    }

    @Test
    void cachesNonEmptySortInfoPermanently() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        CacheService cacheService = newCacheService(redisUtil);
        List<Sort> sorts = List.of(new Sort());

        cacheService.cacheSortInfo(sorts);

        verify(redisUtil).set(eq(CacheConstants.SORT_INFO_KEY), eq(sorts),
                eq(CacheConstants.PERMANENT_EXPIRE_TIME));
    }

    @Test
    void treatsCachedEmptyListAsMiss() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        CacheService cacheService = newCacheService(redisUtil);
        when(redisUtil.get(CacheConstants.SORT_INFO_KEY)).thenReturn(new ArrayList<Sort>());

        assertNull(cacheService.getCachedSortInfo());
    }

    @Test
    void returnsCachedNonEmptyList() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        CacheService cacheService = newCacheService(redisUtil);
        List<Sort> sorts = List.of(new Sort());
        when(redisUtil.get(CacheConstants.SORT_INFO_KEY)).thenReturn(sorts);

        assertSame(sorts, cacheService.getCachedSortInfo());
    }
}