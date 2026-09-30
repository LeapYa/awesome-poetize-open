package com.ld.poetry.dao;

import com.ld.poetry.entity.ArticlePayment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 文章付费记录表 Mapper 接口
 * </p>
 *
 * @author LeapYa
 * @since 2026-02-18
 */
@Mapper
public interface ArticlePaymentMapper extends BaseMapper<ArticlePayment> {

    /**
     * 按文章物理删除付费记录（文章彻底删除时清理，避免孤儿行累积）
     */
    @Delete("DELETE FROM article_payment WHERE article_id = #{articleId}")
    int physicalDeleteByArticleId(@Param("articleId") Integer articleId);
}
