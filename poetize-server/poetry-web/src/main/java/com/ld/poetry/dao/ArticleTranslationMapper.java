package com.ld.poetry.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ld.poetry.entity.ArticleTranslation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 文章翻译内容表 Mapper 接口
 * </p>
 *
 * @author leapya
 * @since 2024-05-10
 */
@Mapper
public interface ArticleTranslationMapper extends BaseMapper<ArticleTranslation> {

    @Select("""
            SELECT article_id FROM article_translation
            WHERE title REGEXP #{regexPattern}
            LIMIT 100
            """)
    List<Integer> selectArticleIdsByTitleRegex(@Param("regexPattern") String regexPattern);

    @Select("""
            SELECT article_id FROM article_translation
            WHERE content REGEXP #{regexPattern}
            LIMIT 100
            """)
    List<Integer> selectArticleIdsByContentRegex(@Param("regexPattern") String regexPattern);
} 
