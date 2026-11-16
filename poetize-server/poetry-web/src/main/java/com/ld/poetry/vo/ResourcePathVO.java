package com.ld.poetry.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class ResourcePathVO {

    /**
     * id
     */
    private Integer id;

    /**
     * 标题
     */
    private String title;

    /**
     * 分类
     */
    private String classify;

    /**
     * 封面
     */
    private String cover;

    /**
     * 链接
     */
    private String url;

    /**
     * 资源类型
     */
    private String type;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否启用[0:否，1:是]
     */
    private Boolean status;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 排序优先级：数字小的在前面，未填写的按原有顺序排在后面
     */
    private Integer priority;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 按钮宽度（快捷入口专用，前端临时字段，不存数据库）
     */
    private String btnWidth;

    /**
     * 按钮高度（快捷入口专用，前端临时字段，不存数据库）
     */
    private String btnHeight;

    /**
     * 按钮圆角（快捷入口专用，前端临时字段，不存数据库）
     */
    private String btnRadius;

    /**
     * 额外背景层（侧边栏背景专用，前端临时字段，不存数据库）
     */
    private String extraBackground;
}
