package com.ld.poetry.service;

import com.ld.poetry.entity.SysPlugin;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 插件配置表 服务类
 * </p>
 *
 * @author LeapYa
 * @since 2026-01-06
 */
public interface SysPluginService extends IService<SysPlugin> {
    
    /**
     * 根据插件类型获取所有插件
     * 
     * @param pluginType 插件类型
     * @return 插件列表
     */
    List<SysPlugin> getPluginsByType(String pluginType);
    
    /**
     * 根据插件类型和标识符获取插件
     * 
     * @param pluginType 插件类型
     * @param pluginKey 插件标识符
     * @return 插件
     */
    SysPlugin getPluginByTypeAndKey(String pluginType, String pluginKey);
    
    /**
     * 获取当前激活的插件
     * 
     * @param pluginType 插件类型
     * @return 当前激活的插件
     */
    SysPlugin getActivePlugin(String pluginType);
    
    /**
     * 设置激活的插件
     * 
     * @param pluginType 插件类型
     * @param pluginKey 插件标识符
     * @return 是否成功
     */
    boolean setActivePlugin(String pluginType, String pluginKey);
    
    /**
     * 获取鼠标点击效果插件列表（公开接口，前端调用）
     * 
     * @return 插件列表（只返回启用的插件）
     */
    List<SysPlugin> getMouseClickEffectPlugins();
    
    /**
     * 获取当前激活的鼠标点击效果
     * 
     * @return 当前激活的插件key
     */
    String getActiveMouseClickEffect();
}
