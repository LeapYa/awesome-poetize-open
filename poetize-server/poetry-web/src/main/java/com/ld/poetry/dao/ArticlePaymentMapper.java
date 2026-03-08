package com.ld.poetry.dao;

import com.ld.poetry.entity.ArticlePayment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

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

}
