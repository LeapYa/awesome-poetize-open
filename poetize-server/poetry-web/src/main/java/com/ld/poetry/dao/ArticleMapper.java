package com.ld.poetry.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ld.poetry.entity.Article;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 文章表 Mapper 接口
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    @Update("update article set view_count=view_count+1 where id=#{id}")
    int updateViewCount(@Param("id") Integer id);

    @Update("update article set view_count = case when view_count >= #{count} then view_count - #{count} else 0 end where id=#{id}")
    int decrementViewCount(@Param("id") Integer id, @Param("count") Integer count);

    @Select("""
            SELECT id FROM article
            WHERE deleted = 0 AND article_title REGEXP #{regexPattern}
            LIMIT 100
            """)
    List<Integer> selectIdsByTitleRegex(@Param("regexPattern") String regexPattern);

    @Select("""
            SELECT id FROM article
            WHERE deleted = 0 AND article_content REGEXP #{regexPattern}
            LIMIT 100
            """)
    List<Integer> selectIdsByContentRegex(@Param("regexPattern") String regexPattern);

    // ================================ 回收站 ================================

    /**
     * 移入回收站：置逻辑删除标记并记录删除时间（绕过 @TableLogic 的手工 SQL）。
     * {@code deleted = 0} 守卫避免对已进入回收站/已物理删除的行重复置位并刷新删除时间。
     */
    @Update("update article set deleted = 1, deleted_time = NOW() where id = #{id} and deleted = 0")
    int softDeleteToTrash(@Param("id") Integer id);

    /**
     * 从回收站恢复：清除逻辑删除标记与删除时间。
     * {@code deleted = 1} 守卫保证只恢复仍在回收站中的行。
     */
    @Update("update article set deleted = 0, deleted_time = NULL where id = #{id} and deleted = 1")
    int restoreFromTrash(@Param("id") Integer id);

    /**
     * 彻底删除：物理删除文章行（翻译与版本的清理由服务层负责）。
     * {@code deleted = 1} 守卫保证不会误删已被并发恢复成活文章的行。
     */
    @Delete("delete from article where id = #{id} and deleted = 1")
    int physicalDeleteById(@Param("id") Integer id);

    /**
     * 超期彻底删除：在 {@link #physicalDeleteById} 基础上追加保留期守卫，
     * 避免"过期后被恢复、又被重新删除"的行被这次清理提前物理删除。
     */
    @Delete("""
            delete from article
            where id = #{id} and deleted = 1
              and deleted_time is not null and deleted_time < #{cutoff}
            """)
    int physicalDeleteExpiredTrash(@Param("id") Integer id, @Param("cutoff") LocalDateTime cutoff);

    /**
     * 查回收站中的文章（含所有列，用于恢复前校验与权限判断）
     */
    @Select("select * from article where id = #{id} and deleted = 1")
    Article selectTrashedById(@Param("id") Integer id);

    /**
     * slug 冲突检查：包含已删除行（唯一索引跨软删行存在，恢复时据此加后缀）。
     * excludeId 为 null 时（新建文章）不排除任何行。
     */
    @Select("""
            <script>
            select count(*) from article where article_slug = #{slug}
            <if test="excludeId != null">
                and id != #{excludeId}
            </if>
            </script>
            """)
    int countBySlugIncludingTrashed(@Param("slug") String slug, @Param("excludeId") Integer excludeId);

    /**
     * 分页查询回收站文章（deleted=1，绕过 @TableLogic 自动过滤）
     */
    @Select("""
            <script>
            SELECT id, user_id, sort_id, label_id, article_cover, article_title, article_slug, summary,
                   view_status, publish_time, create_time, update_time, deleted_time
            FROM article
            WHERE deleted = 1
            <if test="userId != null"> AND user_id = #{userId}</if>
            <if test="searchKey != null and searchKey != ''"> AND article_title LIKE CONCAT('%', #{searchKey}, '%')</if>
            ORDER BY deleted_time DESC, id DESC
            </script>
            """)
    IPage<Article> selectTrashPage(Page<Article> page, @Param("userId") Integer userId,
                                   @Param("searchKey") String searchKey);

    /**
     * 查询回收站中超期未清理的文章ID（保留期倒计时清理用），按批返回避免全量进内存
     */
    @Select("""
            select id from article
            where deleted = 1 and deleted_time is not null and deleted_time < #{cutoff}
            order by id
            limit #{limit}
            """)
    List<Integer> selectExpiredTrashIds(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);

    /**
     * 事务内锁定文章行，用于串行化同一篇文章的并发版本快照（避免 version_no 重号）。
     * 无事务时退化为普通查询，仅起存在性校验作用。
     */
    @Select("select id from article where id = #{id} for update")
    Integer lockRowById(@Param("id") Integer id);
}
