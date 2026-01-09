package com.ld.poetry.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ld.poetry.aop.LoginCheck;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.entity.SysPlugin;
import com.ld.poetry.service.SysPluginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 插件配置表 前端控制器
 * </p>
 *
 * @author LeapYa
 * @since 2026-01-06
 */
@RestController
@RequestMapping("/sysPlugin")
@Slf4j
public class SysPluginController {

    @Autowired
    private SysPluginService sysPluginService;

    // ============ 公开接口（前端网站调用） ============

    /**
     * 获取鼠标点击效果列表（公开接口）
     * 返回所有启用的鼠标点击效果插件
     */
    @GetMapping("/getMouseClickEffects")
    public PoetryResult<List<SysPlugin>> getMouseClickEffects() {
        List<SysPlugin> plugins = sysPluginService.getMouseClickEffectPlugins();
        return PoetryResult.success(plugins);
    }

    /**
     * 获取当前激活的鼠标点击效果（公开接口）
     */
    @GetMapping("/getActiveMouseClickEffect")
    public PoetryResult<Map<String, Object>> getActiveMouseClickEffect() {
        String activeKey = sysPluginService.getActiveMouseClickEffect();
        SysPlugin plugin = sysPluginService.getPluginByTypeAndKey(SysPlugin.TYPE_MOUSE_CLICK_EFFECT, activeKey);
        
        Map<String, Object> result = new HashMap<>();
        result.put("pluginKey", activeKey);
        if (plugin != null) {
            result.put("pluginName", plugin.getPluginName());
            result.put("pluginConfig", plugin.getPluginConfig());
        }
        return PoetryResult.success(result);
    }

    /**
     * 获取当前激活的看板娘模型（公开接口）
     * 前端网站用于获取应该显示的看板娘模型配置
     */
    @GetMapping("/getActiveWaifuModel")
    public PoetryResult<SysPlugin> getActiveWaifuModel() {
        SysPlugin plugin = sysPluginService.getActivePlugin("waifu_model");
        return PoetryResult.success(plugin);
    }

    /**
     * 获取所有启用的看板娘模型（公开接口）
     * 用于前端"换人"功能，返回模型列表
     * 会将当前激活的插件排在第一位，作为默认显示
     */
    @GetMapping("/getAllWaifuModels")
    public PoetryResult<List<SysPlugin>> getAllWaifuModels() {
        List<SysPlugin> plugins = sysPluginService.getPluginsByType("waifu_model");
        plugins.removeIf(p -> !Boolean.TRUE.equals(p.getEnabled()));
        
        // 获取当前激活的插件
        SysPlugin activePlugin = sysPluginService.getActivePlugin("waifu_model");
        if (activePlugin != null) {
            final String activeKey = activePlugin.getPluginKey(); // Final for lambda
            // 将激活的插件移到第一位
            plugins.sort((p1, p2) -> {
                if (p1.getPluginKey().equals(activeKey)) return -1;
                if (p2.getPluginKey().equals(activeKey)) return 1;
                // 其他按 sortOrder 排序
                return Integer.compare(
                    p1.getSortOrder() != null ? p1.getSortOrder() : 0, 
                    p2.getSortOrder() != null ? p2.getSortOrder() : 0
                );
            });
        }
        
        return PoetryResult.success(plugins);
    }

    // ============ 管理接口（需要登录） ============

    /**
     * 获取指定类型的所有插件（管理员接口）
     */
    @LoginCheck(0)
    @GetMapping("/listPlugins")
    public PoetryResult<List<SysPlugin>> listPlugins(@RequestParam String pluginType) {
        if (!StringUtils.hasText(pluginType)) {
            return PoetryResult.fail("插件类型不能为空");
        }
        List<SysPlugin> plugins = sysPluginService.getPluginsByType(pluginType);
        return PoetryResult.success(plugins);
    }

    /**
     * 获取插件详情
     */
    @LoginCheck(0)
    @GetMapping("/getPlugin")
    public PoetryResult<SysPlugin> getPlugin(@RequestParam Integer id) {
        SysPlugin plugin = sysPluginService.getById(id);
        if (plugin == null) {
            return PoetryResult.fail("插件不存在");
        }
        return PoetryResult.success(plugin);
    }

    /**
     * 获取当前激活的插件
     */
    @LoginCheck(0)
    @GetMapping("/getActivePlugin")
    public PoetryResult<SysPlugin> getActivePlugin(@RequestParam String pluginType) {
        if (!StringUtils.hasText(pluginType)) {
            return PoetryResult.fail("插件类型不能为空");
        }
        SysPlugin plugin = sysPluginService.getActivePlugin(pluginType);
        return PoetryResult.success(plugin);
    }

    /**
     * 设置激活的插件
     */
    @LoginCheck(0)
    @PostMapping("/setActivePlugin")
    public PoetryResult<Void> setActivePlugin(@RequestBody Map<String, String> params) {
        String pluginType = params.get("pluginType");
        String pluginKey = params.get("pluginKey");
        
        if (!StringUtils.hasText(pluginType) || !StringUtils.hasText(pluginKey)) {
            return PoetryResult.fail("参数不完整");
        }
        
        boolean success = sysPluginService.setActivePlugin(pluginType, pluginKey);
        if (success) {
            return PoetryResult.success();
        } else {
            return PoetryResult.fail("设置失败");
        }
    }

    /**
     * 新增插件
     */
    @LoginCheck(0)
    @PostMapping("/addPlugin")
    public PoetryResult<SysPlugin> addPlugin(@RequestBody SysPlugin plugin) {
        // 参数验证
        if (!StringUtils.hasText(plugin.getPluginType())) {
            return PoetryResult.fail("插件类型不能为空");
        }
        if (!StringUtils.hasText(plugin.getPluginKey())) {
            return PoetryResult.fail("插件标识符不能为空");
        }
        if (!StringUtils.hasText(plugin.getPluginName())) {
            return PoetryResult.fail("插件名称不能为空");
        }
        
        // 检查是否已存在
        SysPlugin existing = sysPluginService.getPluginByTypeAndKey(plugin.getPluginType(), plugin.getPluginKey());
        if (existing != null) {
            return PoetryResult.fail("该插件标识符已存在");
        }
        
        // 设置默认值
        plugin.setId(null);
        plugin.setIsSystem(false); // 用户创建的插件不是系统内置
        plugin.setCreateTime(LocalDateTime.now());
        plugin.setUpdateTime(LocalDateTime.now());
        if (plugin.getEnabled() == null) {
            plugin.setEnabled(true);
        }
        if (plugin.getSortOrder() == null) {
            // 获取当前最大排序号
            LambdaQueryWrapper<SysPlugin> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysPlugin::getPluginType, plugin.getPluginType())
                        .orderByDesc(SysPlugin::getSortOrder)
                        .last("LIMIT 1");
            SysPlugin maxSortPlugin = sysPluginService.getOne(queryWrapper);
            plugin.setSortOrder(maxSortPlugin != null ? maxSortPlugin.getSortOrder() + 1 : 0);
        }
        
        boolean success = sysPluginService.save(plugin);
        if (success) {
            log.info("新增插件成功: type={}, key={}, name={}", plugin.getPluginType(), plugin.getPluginKey(), plugin.getPluginName());
            return PoetryResult.success(plugin);
        } else {
            return PoetryResult.fail("新增失败");
        }
    }

    /**
     * 更新插件
     */
    @LoginCheck(0)
    @PostMapping("/updatePlugin")
    public PoetryResult<SysPlugin> updatePlugin(@RequestBody SysPlugin plugin) {
        if (plugin.getId() == null) {
            return PoetryResult.fail("插件ID不能为空");
        }
        
        // 检查插件是否存在
        SysPlugin existing = sysPluginService.getById(plugin.getId());
        if (existing == null) {
            return PoetryResult.fail("插件不存在");
        }
        
        // 系统内置插件可以修改所有字段，但不能修改pluginKey和pluginType
        // 因为系统插件的JS代码现在也完全存储在数据库中
        plugin.setPluginKey(existing.getPluginKey()); // 不允许修改标识符
        plugin.setPluginType(existing.getPluginType()); // 不允许修改类型
        plugin.setIsSystem(existing.getIsSystem()); // 保持系统插件标识不变
        plugin.setUpdateTime(LocalDateTime.now());
        
        boolean success = sysPluginService.updateById(plugin);
        if (success) {
            log.info("更新插件成功: id={}, name={}", plugin.getId(), plugin.getPluginName());
            return PoetryResult.success(plugin);
        } else {
            return PoetryResult.fail("更新失败");
        }
    }

    /**
     * 删除插件
     */
    @LoginCheck(0)
    @PostMapping("/deletePlugin")
    public PoetryResult<Void> deletePlugin(@RequestBody Map<String, Integer> params) {
        Integer id = params.get("id");
        if (id == null) {
            return PoetryResult.fail("插件ID不能为空");
        }
        
        // 检查插件是否存在
        SysPlugin existing = sysPluginService.getById(id);
        if (existing == null) {
            return PoetryResult.fail("插件不存在");
        }
        
        // 系统内置插件不能删除
        if (Boolean.TRUE.equals(existing.getIsSystem())) {
            return PoetryResult.fail("系统内置插件不能删除");
        }
        
        // 检查是否为当前激活的插件
        SysPlugin activePlugin = sysPluginService.getActivePlugin(existing.getPluginType());
        if (activePlugin != null && activePlugin.getId().equals(id)) {
            return PoetryResult.fail("不能删除当前激活的插件，请先切换到其他插件");
        }
        
        boolean success = sysPluginService.removeById(id);
        if (success) {
            log.info("删除插件成功: id={}, name={}", id, existing.getPluginName());
            return PoetryResult.success();
        } else {
            return PoetryResult.fail("删除失败");
        }
    }

    /**
     * 切换插件启用状态
     */
    @LoginCheck(0)
    @PostMapping("/togglePluginStatus")
    public PoetryResult<Void> togglePluginStatus(@RequestBody Map<String, Object> params) {
        Integer id = (Integer) params.get("id");
        Boolean enabled = (Boolean) params.get("enabled");
        
        if (id == null || enabled == null) {
            return PoetryResult.fail("参数不完整");
        }
        
        SysPlugin existing = sysPluginService.getById(id);
        if (existing == null) {
            return PoetryResult.fail("插件不存在");
        }
        
        // 如果要禁用插件，检查是否为当前激活的插件
        if (!enabled) {
            SysPlugin activePlugin = sysPluginService.getActivePlugin(existing.getPluginType());
            if (activePlugin != null && activePlugin.getId().equals(id)) {
                return PoetryResult.fail("不能禁用当前激活的插件，请先切换到其他插件");
            }
        }
        
        existing.setEnabled(enabled);
        existing.setUpdateTime(LocalDateTime.now());
        boolean success = sysPluginService.updateById(existing);
        
        if (success) {
            log.info("切换插件状态成功: id={}, enabled={}", id, enabled);
            return PoetryResult.success();
        } else {
            return PoetryResult.fail("操作失败");
        }
    }
}
