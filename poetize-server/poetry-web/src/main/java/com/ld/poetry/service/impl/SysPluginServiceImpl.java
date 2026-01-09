package com.ld.poetry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ld.poetry.dao.SysPluginActiveMapper;
import com.ld.poetry.dao.SysPluginMapper;
import com.ld.poetry.entity.SysPlugin;
import com.ld.poetry.entity.SysPluginActive;
import com.ld.poetry.service.SysPluginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 插件配置表 服务实现类
 * </p>
 *
 * @author LeapYa
 * @since 2026-01-06
 */
@Service
@Slf4j
public class SysPluginServiceImpl extends ServiceImpl<SysPluginMapper, SysPlugin> implements SysPluginService {

    @Autowired
    private SysPluginActiveMapper sysPluginActiveMapper;

    @Override
    public List<SysPlugin> getPluginsByType(String pluginType) {
        LambdaQueryWrapper<SysPlugin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysPlugin::getPluginType, pluginType)
                    .orderByAsc(SysPlugin::getSortOrder);
        return this.list(queryWrapper);
    }

    @Override
    public SysPlugin getPluginByTypeAndKey(String pluginType, String pluginKey) {
        LambdaQueryWrapper<SysPlugin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysPlugin::getPluginType, pluginType)
                    .eq(SysPlugin::getPluginKey, pluginKey);
        return this.getOne(queryWrapper);
    }

    @Override
    public SysPlugin getActivePlugin(String pluginType) {
        // 先获取激活记录
        LambdaQueryWrapper<SysPluginActive> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(SysPluginActive::getPluginType, pluginType);
        SysPluginActive active = sysPluginActiveMapper.selectOne(activeWrapper);
        
        if (active == null) {
            log.warn("未找到插件类型 {} 的激活记录", pluginType);
            return null;
        }
        
        // 获取对应的插件
        return getPluginByTypeAndKey(pluginType, active.getPluginKey());
    }

    @Override
    @Transactional
    public boolean setActivePlugin(String pluginType, String pluginKey) {
        try {
            // 验证插件是否存在
            SysPlugin plugin = getPluginByTypeAndKey(pluginType, pluginKey);
            if (plugin == null) {
                log.error("插件不存在: type={}, key={}", pluginType, pluginKey);
                return false;
            }
            
            // 验证插件是否启用
            if (!Boolean.TRUE.equals(plugin.getEnabled())) {
                log.error("插件未启用: type={}, key={}", pluginType, pluginKey);
                return false;
            }
            
            // 更新或插入激活记录
            LambdaQueryWrapper<SysPluginActive> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysPluginActive::getPluginType, pluginType);
            SysPluginActive active = sysPluginActiveMapper.selectOne(queryWrapper);
            
            if (active == null) {
                // 插入新记录
                active = new SysPluginActive();
                active.setPluginType(pluginType);
                active.setPluginKey(pluginKey);
                active.setUpdateTime(LocalDateTime.now());
                sysPluginActiveMapper.insert(active);
            } else {
                // 更新记录
                active.setPluginKey(pluginKey);
                active.setUpdateTime(LocalDateTime.now());
                sysPluginActiveMapper.updateById(active);
            }
            
            log.info("成功设置激活插件: type={}, key={}", pluginType, pluginKey);
            return true;
        } catch (Exception e) {
            log.error("设置激活插件失败: type={}, key={}, error={}", pluginType, pluginKey, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<SysPlugin> getMouseClickEffectPlugins() {
        LambdaQueryWrapper<SysPlugin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysPlugin::getPluginType, SysPlugin.TYPE_MOUSE_CLICK_EFFECT)
                    .eq(SysPlugin::getEnabled, true)
                    .orderByAsc(SysPlugin::getSortOrder);
        return this.list(queryWrapper);
    }

    @Override
    public String getActiveMouseClickEffect() {
        LambdaQueryWrapper<SysPluginActive> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysPluginActive::getPluginType, SysPlugin.TYPE_MOUSE_CLICK_EFFECT);
        SysPluginActive active = sysPluginActiveMapper.selectOne(queryWrapper);
        
        if (active == null) {
            return "none"; // 默认无效果
        }
        return active.getPluginKey();
    }
}
