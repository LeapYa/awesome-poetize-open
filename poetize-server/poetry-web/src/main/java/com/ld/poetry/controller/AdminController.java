package com.ld.poetry.controller;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ld.poetry.aop.LoginCheck;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.dao.TreeHoleMapper;
import com.ld.poetry.dao.WebInfoMapper;
import com.ld.poetry.entity.*;
import com.ld.poetry.vo.BaseRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import com.ld.poetry.enums.PoetryEnum;
import org.springframework.core.env.Environment;
import lombok.extern.slf4j.Slf4j;
import com.ld.poetry.service.CacheService;
import com.ld.poetry.service.WebInfoService;
import com.ld.poetry.config.PoetryApplicationRunner;
import java.util.HashMap;

/**
 * <p>
 * 后台 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@RestController
@RequestMapping("/admin")
@Slf4j
public class AdminController {

    @Autowired
    private WebInfoMapper webInfoMapper;

    @Autowired
    private TreeHoleMapper treeHoleMapper;

    @Autowired
    private Environment env;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private com.ld.poetry.service.PasswordUpgradeService passwordUpgradeService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private WebInfoService webInfoService;

    @Autowired
    private PoetryApplicationRunner poetryApplicationRunner;

    @Autowired
    private com.ld.poetry.service.SitemapService sitemapService;

    @Autowired
    private com.ld.poetry.service.SearchEnginePushService searchEnginePushService;

    @Autowired
    private com.ld.poetry.service.RobotsService robotsService;

    /**
     * 获取网站信息
     */
    @GetMapping("/webInfo/getAdminWebInfo")
    @LoginCheck(0)
    public PoetryResult<WebInfo> getWebInfo() {
        LambdaQueryChainWrapper<WebInfo> wrapper = new LambdaQueryChainWrapper<>(webInfoMapper);
        List<WebInfo> list = wrapper.list();
        if (!CollectionUtils.isEmpty(list)) {
            return PoetryResult.success(list.get(0));
        } else {
            return PoetryResult.success();
        }
    }

    /**
     * Boss查询树洞
     */
    @PostMapping("/treeHole/boss/list")
    @LoginCheck(1)
    public PoetryResult<Page> listBossTreeHole(@RequestBody BaseRequestVO baseRequestVO) {
        LambdaQueryChainWrapper<TreeHole> wrapper = new LambdaQueryChainWrapper<>(treeHoleMapper);
        Page<TreeHole> page = new Page<>(baseRequestVO.getCurrent(), baseRequestVO.getSize());
        Page<TreeHole> resultPage = wrapper.orderByDesc(TreeHole::getCreateTime).page(page);
        return PoetryResult.success(resultPage);
    }

    /**
     * 获取密码升级统计信息
     */
    @GetMapping("/password/upgrade/statistics")
    @LoginCheck(0)
    public PoetryResult<Map<String, Object>> getPasswordUpgradeStatistics() {
        try {
            Map<String, Object> statistics = passwordUpgradeService.getUpgradeStatistics();
            return PoetryResult.success(statistics);
        } catch (Exception e) {
            log.error("获取密码升级统计失败", e);
            return PoetryResult.fail("获取密码升级统计失败，请稍后重试");
        }
    }

    /**
     * 获取密码安全报告
     */
    @GetMapping("/password/security/report")
    @LoginCheck(0)
    public PoetryResult<String> getPasswordSecurityReport() {
        try {
            String report = passwordUpgradeService.generateSecurityReport();
            return PoetryResult.success(report);
        } catch (Exception e) {
            log.error("生成密码安全报告失败", e);
            return PoetryResult.fail("生成密码安全报告失败，请稍后重试");
        }
    }

    /**
     * 获取管理员网站详细信息（包含敏感配置）
     * 替代Python端的getAdminWebInfoDetails端点
     */
    @GetMapping("/webInfo/getAdminWebInfoDetails")
    @LoginCheck(0)
    public PoetryResult<WebInfo> getAdminWebInfoDetails(
            @RequestParam(value = "refresh", defaultValue = "false") boolean forceRefresh) {
        try {
            if (forceRefresh) {
                // 强制刷新缓存
                cacheService.evictWebInfo();
                log.info("强制刷新网站信息缓存");
            }

            // 从缓存获取网站信息
            WebInfo webInfo = cacheService.getCachedWebInfo();

            if (webInfo == null) {
                log.info("缓存中未找到网站信息，从数据库重新加载");
                // 缓存为空，从数据库重新加载
                LambdaQueryChainWrapper<WebInfo> wrapper = new LambdaQueryChainWrapper<>(webInfoMapper);
                List<WebInfo> list = wrapper.list();
                if (!CollectionUtils.isEmpty(list)) {
                    webInfo = list.get(0);
                    cacheService.cacheWebInfo(webInfo);
                    log.info("从数据库重新加载网站信息并缓存 - webName: {}, webTitle: {}",
                            webInfo.getWebName(), webInfo.getWebTitle());
                } else {
                    log.error("数据库中未找到网站信息");
                    return PoetryResult.fail("网站信息不存在");
                }
            }

            // 返回完整信息（包含randomAvatar, randomName, waifuJson等配置）
            return PoetryResult.success(webInfo);
        } catch (Exception e) {
            log.error("获取管理员网站详细信息失败", e);
            return PoetryResult.fail("获取网站信息失败，请稍后重试");
        }
    }

    /**
     * 刷新管理员缓存
     * 替代Python端的refreshAdminWebInfoCache端点
     */
    @PostMapping("/webInfo/refreshCache")
    @LoginCheck(0)
    public PoetryResult<Map<String, Object>> refreshAdminCache() {
        try {
            // 清理网站信息缓存
            cacheService.evictWebInfo();

            // 重新加载并缓存
            LambdaQueryChainWrapper<WebInfo> wrapper = new LambdaQueryChainWrapper<>(webInfoMapper);
            List<WebInfo> list = wrapper.list();
            if (!CollectionUtils.isEmpty(list)) {
                cacheService.cacheWebInfo(list.get(0));
                log.info("网站信息缓存刷新成功");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("cleared_count", 1);
            result.put("message", "缓存刷新成功");

            log.info("管理员缓存刷新完成");
            return PoetryResult.success(result);
        } catch (Exception e) {
            log.error("刷新管理员缓存失败", e);
            return PoetryResult.fail("缓存刷新失败，请稍后重试");
        }
    }

    /**
     * 更新看板娘状态
     * 替代Python端的updateWaifuStatus端点
     */
    @PostMapping("/webInfo/updateWaifuStatus")
    @LoginCheck(0)
    public PoetryResult<Map<String, Object>> updateWaifuStatus(@RequestBody Map<String, Object> request) {
        try {
            // 验证请求参数
            if (!request.containsKey("enableWaifu")) {
                return PoetryResult.fail("缺少enableWaifu字段");
            }

            Boolean enableWaifu = (Boolean) request.get("enableWaifu");
            Integer id = (Integer) request.get("id");

            log.info("收到更新看板娘状态请求: enableWaifu={}, id={}", enableWaifu, id);

            // 获取当前网站信息
            LambdaQueryChainWrapper<WebInfo> wrapper = new LambdaQueryChainWrapper<>(webInfoMapper);
            List<WebInfo> list = wrapper.list();

            if (CollectionUtils.isEmpty(list)) {
                return PoetryResult.fail("网站信息不存在");
            }

            WebInfo webInfo = list.get(0);

            // 如果提供了id，验证id是否匹配
            if (id != null && !id.equals(webInfo.getId())) {
                return PoetryResult.fail("网站信息ID不匹配");
            }

            // 更新看板娘状态
            webInfo.setEnableWaifu(enableWaifu);

            // 保存到数据库
            webInfoService.updateById(webInfo);

            // 清理并重新缓存
            cacheService.evictWebInfo();
            cacheService.cacheWebInfo(webInfo);

            log.info("看板娘状态更新成功: enableWaifu={}", enableWaifu);

            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("enableWaifu", enableWaifu);
            result.put("id", webInfo.getId());

            return PoetryResult.success(result);
        } catch (Exception e) {
            log.error("更新看板娘状态失败", e);
            return PoetryResult.fail("更新看板娘状态失败，请稍后重试");
        }
    }
}
