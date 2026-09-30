package com.ld.poetry.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ld.poetry.entity.ArticleVersion;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 文章历史版本快照表 Mapper 接口
 * </p>
 */
@Mapper
public interface ArticleVersionMapper extends BaseMapper<ArticleVersion> {

    @Select("select COALESCE(MAX(version_no), 0) from article_version where article_id = #{articleId}")
    int selectMaxVersionNo(@Param("articleId") Integer articleId);

    @Select("select content_hash from article_version where article_id = #{articleId} order by version_no desc limit 1")
    String selectLatestContentHash(@Param("articleId") Integer articleId);

    @Delete("delete from article_version where article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Integer articleId);

    /**
     * 分批查询可裁剪的超龄版本ID：只处理非"该篇最新版"的行，
     * 保证每篇文章至少保留 1 个可回滚点（最新版即"上一次更新的前像"）。
     */
    @Select("""
            select v.id from article_version v
            where v.create_time < #{cutoff}
              and v.version_no < (
                  select max(n.version_no) from article_version n where n.article_id = v.article_id
              )
            order by v.id
            limit #{limit}
            """)
    List<Long> selectPrunableExpiredIds(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);
}
