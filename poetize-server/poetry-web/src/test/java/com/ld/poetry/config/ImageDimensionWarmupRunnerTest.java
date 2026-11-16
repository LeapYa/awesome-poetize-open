package com.ld.poetry.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ld.poetry.dao.ResourceMapper;
import com.ld.poetry.entity.Resource;
import com.ld.poetry.service.ResourceService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageDimensionWarmupRunnerTest {

    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceMapper resourceMapper;

    private final List<Resource> rows = new ArrayList<>();
    private final List<Integer> cursors = new ArrayList<>();

    private ImageDimensionWarmupRunner runner;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Resource.class);
        runner = new ImageDimensionWarmupRunner(resourceService);
        ReflectionTestUtils.setField(runner, "warmupBatchSize", 100);
        ReflectionTestUtils.setField(runner, "localUploadUrl", "");
        ReflectionTestUtils.setField(runner, "localDownloadUrl", "");

        doAnswer(invocation -> new LambdaQueryChainWrapper<>(resourceMapper, new CursorRecordingWrapper()))
                .when(resourceService).lambdaQuery();
        // 按已记录的 id 游标分页；width 始终为 null，模拟"文件读不到 → 永不回写"的孤儿记录
        when(resourceMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            Page<Resource> page = invocation.getArgument(0);
            int cursor = cursors.isEmpty() ? 0 : cursors.get(cursors.size() - 1);
            page.setRecords(rows.stream()
                    .filter(row -> row.getId() > cursor)
                    .limit(page.getSize())
                    .toList());
            return page;
        });
    }

    @Test
    void shouldStopWhenEveryImageFileIsMissing(@TempDir Path tempDir) {
        for (int id = 1; id <= 109; id++) {
            rows.add(row(id, tempDir.resolve("missing-" + id + ".jpg").toString(), "image/jpeg"));
        }

        assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> ReflectionTestUtils.invokeMethod(runner, "warmupMissingDimensions"));

        assertEquals(List.of(0, 100), cursors);
        verify(resourceService, never()).updateById(any(Resource.class));
    }

    @Test
    void shouldBackfillDimensionsForReadableImages(@TempDir Path tempDir) throws Exception {
        for (int id = 1; id <= 3; id++) {
            File image = tempDir.resolve("image-" + id + ".png").toFile();
            ImageIO.write(new BufferedImage(8, 6, BufferedImage.TYPE_INT_RGB), "png", image);
            rows.add(row(id, image.getAbsolutePath(), "image/png"));
        }
        when(resourceService.updateById(any(Resource.class))).thenReturn(true);

        ReflectionTestUtils.invokeMethod(runner, "warmupMissingDimensions");

        ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceService, times(3)).updateById(captor.capture());
        for (Resource updated : captor.getAllValues()) {
            assertEquals(8, updated.getWidth().intValue());
            assertEquals(6, updated.getHeight().intValue());
        }
        assertEquals(List.of(0), cursors);
    }

    /**
     * 记录每轮查询传入的 id 游标；不调用 gt 时游标列表为空，分页永远停在第 1 页。
     */
    private class CursorRecordingWrapper extends LambdaQueryWrapper<Resource> {
        @Override
        public LambdaQueryWrapper<Resource> gt(boolean condition, SFunction<Resource, ?> column, Object val) {
            cursors.add((Integer) val);
            return super.gt(condition, column, val);
        }
    }

    private Resource row(int id, String path, String mimeType) {
        Resource resource = new Resource();
        resource.setId(id);
        resource.setPath(path);
        resource.setStoreType("local");
        resource.setMimeType(mimeType);
        return resource;
    }
}