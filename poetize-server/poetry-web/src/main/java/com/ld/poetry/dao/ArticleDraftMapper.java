package com.ld.poetry.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ld.poetry.entity.ArticleDraft;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleDraftMapper extends BaseMapper<ArticleDraft> {

    @Delete("DELETE FROM article_draft WHERE id = #{draftId}")
    int physicalDeleteById(@Param("draftId") String draftId);

    /**
     * 按关联文章物理删除修订草稿（含逻辑删除的草稿行：软删行仍占用 uk_article_draft_revision 唯一键）
     */
    @Delete("DELETE FROM article_draft WHERE article_id = #{articleId}")
    int physicalDeleteByArticleId(@Param("articleId") Integer articleId);
}
