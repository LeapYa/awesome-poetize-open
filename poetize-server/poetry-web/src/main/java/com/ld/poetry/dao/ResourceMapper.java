package com.ld.poetry.dao;

import com.ld.poetry.entity.Resource;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 资源信息 Mapper 接口
 * </p>
 *
 * @author sara
 * @since 2022-03-06
 */
@Mapper
public interface ResourceMapper extends BaseMapper<Resource> {

    Page<Resource> selectOrphanResources(Page<Resource> page,
                                         @Param("excludedTypes") List<String> excludedTypes,
                                         @Param("orderColumn") String orderColumn,
                                         @Param("asc") boolean asc);

    @Select("SELECT * FROM resource WHERE id = #{id} FOR UPDATE")
    Resource selectByIdForUpdate(@Param("id") Integer id);

    @Select("SELECT * FROM resource WHERE public_id = #{publicId} LIMIT 1")
    Resource findByPublicId(@Param("publicId") String publicId);

    @Select("SELECT * FROM resource WHERE public_id = #{publicId} LIMIT 1 FOR UPDATE")
    Resource findByPublicIdForUpdate(@Param("publicId") String publicId);

    @Select("SELECT * FROM resource WHERE path_hash = SHA2(#{path}, 256) AND path = #{path} LIMIT 1")
    Resource findByPath(@Param("path") String path);

    @Select("SELECT * FROM resource WHERE path_hash = SHA2(#{path}, 256) AND path = #{path} LIMIT 1 FOR UPDATE")
    Resource findByPathForUpdate(@Param("path") String path);

    int countResourceReferences(@Param("path") String path);

    // ================================ 回收站 ================================

    /**
     * 分页查询回收站中的资源（content_state=TRASH）
     */
    @Select("""
            <script>
            SELECT * FROM resource
            WHERE content_state = 'TRASH'
            <if test="searchKey != null and searchKey != ''">
              AND (path LIKE CONCAT('%', #{searchKey}, '%') OR original_name LIKE CONCAT('%', #{searchKey}, '%'))
            </if>
            ORDER BY trash_time DESC, id DESC
            </script>
            """)
    Page<Resource> selectTrashPage(Page<Resource> page, @Param("searchKey") String searchKey);

    /**
     * 分页查询可用资源（排除回收站中的资源，供自动化 API 浏览定位）
     */
    @Select("""
            <script>
            SELECT * FROM resource
            WHERE status = 1
              AND (content_state IS NULL OR content_state = 'ACTIVE')
            <if test="searchKey != null and searchKey != ''">
              AND (path LIKE CONCAT('%', #{searchKey}, '%') OR original_name LIKE CONCAT('%', #{searchKey}, '%'))
            </if>
            ORDER BY id DESC
            </script>
            """)
    Page<Resource> selectActivePage(Page<Resource> page, @Param("searchKey") String searchKey);

    /**
     * 查询"删除已无进展"却仍停留在 DELETION_PENDING 的资源：全部物理副本均已到达终态，
     * 说明 finalizeDeletion 未完成（进程中断或引用校验失败），需要退回回收站让用户可见可处理。
     */
    @Select("""
            select r.id from resource r
            where r.content_state = 'DELETION_PENDING'
              and r.status = 0
              and r.deletion_pending_time is not null
              and r.deletion_pending_time < #{cutoff}
              and not exists (
                  select 1 from resource_location l
                  where l.resource_id = r.id
                    and l.status not in ('DELETED', 'MISSING', 'DETACHED')
              )
            """)
    List<Integer> selectUnfinalizedPendingIds(@Param("cutoff") java.time.LocalDateTime cutoff);

    /**
     * 查询回收站中超期未清理的资源ID（保留期倒计时清理用），按批返回避免全量进内存
     */
    @Select("""
            select id from resource
            where content_state = 'TRASH' and trash_time is not null and trash_time < #{cutoff}
            order by id
            limit #{limit}
            """)
    List<Integer> selectExpiredTrashIds(@Param("cutoff") java.time.LocalDateTime cutoff,
                                        @Param("limit") int limit);

}
