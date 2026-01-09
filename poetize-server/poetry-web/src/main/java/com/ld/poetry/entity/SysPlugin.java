package com.ld.poetry.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 插件配置表
 * </p>
 *
 * @author LeapYa
 * @since 2026-01-06
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_plugin")
public class SysPlugin implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 插件ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 插件类型 [mouse_click_effect: 鼠标点击效果]
     */
    @TableField("plugin_type")
    private String pluginType;

    /**
     * 插件唯一标识符
     */
    @TableField("plugin_key")
    private String pluginKey;

    /**
     * 插件名称
     */
    @TableField("plugin_name")
    private String pluginName;

    /**
     * 插件描述
     */
    @TableField("plugin_description")
    private String pluginDescription;

    /**
     * 插件配置(JSON格式)
     */
    @TableField("plugin_config")
    private String pluginConfig;

    /**
     * 插件代码(JavaScript)
     */
    @TableField("plugin_code")
    private String pluginCode;

    /**
     * 是否启用 [0:禁用, 1:启用]
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 是否系统内置 [0:用户创建, 1:系统内置]
     */
    @TableField("is_system")
    private Boolean isSystem;

    /**
     * 排序顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    // 插件类型常量
    public static final String TYPE_MOUSE_CLICK_EFFECT = "mouse_click_effect";
}
