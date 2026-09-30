package com.ld.poetry.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ld.poetry.entity.ResourceTrash;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 资源回收站（替换旧版备份）表 Mapper 接口
 * </p>
 */
@Mapper
public interface ResourceTrashMapper extends BaseMapper<ResourceTrash> {

    @Delete("delete from resource_trash where resource_id = #{resourceId}")
    int deleteByResourceId(@Param("resourceId") Integer resourceId);

    @Select("""
            select id from resource_trash
            where create_time < #{cutoff}
            order by id
            limit #{limit}
            """)
    List<Long> selectExpiredIds(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);
}
