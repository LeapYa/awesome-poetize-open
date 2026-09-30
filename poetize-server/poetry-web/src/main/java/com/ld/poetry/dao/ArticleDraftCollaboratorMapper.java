package com.ld.poetry.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ld.poetry.entity.ArticleDraftCollaborator;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleDraftCollaboratorMapper extends BaseMapper<ArticleDraftCollaborator> {

    @Delete("DELETE FROM article_draft_collaborator WHERE draft_id = #{draftId}")
    int physicalDeleteByDraftId(@Param("draftId") String draftId);

    @Delete("DELETE FROM article_draft_collaborator WHERE draft_id = #{draftId} AND user_id = #{userId}")
    int physicalDeleteByDraftIdAndUserId(@Param("draftId") String draftId, @Param("userId") Integer userId);

    /**
     * 按关联文章物理删除其全部修订草稿的协作者（须在草稿行删除前调用，子查询依赖草稿行仍存在）
     */
    @Delete("""
            DELETE FROM article_draft_collaborator WHERE draft_id IN
            (SELECT id FROM article_draft WHERE article_id = #{articleId})
            """)
    int physicalDeleteByArticleId(@Param("articleId") Integer articleId);
}
