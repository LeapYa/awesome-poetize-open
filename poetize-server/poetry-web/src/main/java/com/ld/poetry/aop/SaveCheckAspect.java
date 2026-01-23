package com.ld.poetry.aop;

import com.ld.poetry.constants.CacheConstants;
import com.ld.poetry.entity.User;
import com.ld.poetry.handle.PoetryRuntimeException;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.service.CacheService;
import com.ld.poetry.utils.PoetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Aspect
@Component
@Order(1)
@Slf4j
public class SaveCheckAspect {

    @Autowired
    private CacheService cacheService;

    @Around("@annotation(saveCheck)")
    public Object around(ProceedingJoinPoint joinPoint, SaveCheck saveCheck) throws Throwable {
        boolean flag = false;

        try {
            User user = PoetryUtil.getCurrentUser();
            if (user != null) {
                // 管理员用户不受保存频率限制
                User adminUser = PoetryUtil.getAdminUser();
                if (adminUser != null && user.getId().intValue() == adminUser.getId().intValue()) {
                    return joinPoint.proceed();
                }

                // 使用Redis缓存管理用户保存计数
                String saveCountKey = CacheConstants.buildSaveCountUserKey(user.getId());
                Object countObj = cacheService.get(saveCountKey);
                int currentCount = countObj != null ? (Integer) countObj : 0;

                if (currentCount >= CommonConst.SAVE_MAX_COUNT) {
                    log.info("用户保存超限：userId={}, 当前次数：{}", user.getId(), currentCount);
                    flag = true;
                } else {
                    // 增加保存计数
                    cacheService.set(saveCountKey, currentCount + 1, CommonConst.SAVE_EXPIRE);
                }
            }
        } catch (Exception e) {
            log.error("检查用户保存频率限制时发生错误", e);
            // 发生错误时不阻止操作，但记录日志
        }

        // IP保存频率限制
        try {
            String ip = PoetryUtil.getIpAddr(PoetryUtil.getRequest());
            String ipCountKey = CacheConstants.buildSaveCountIpKey(ip);
            Object ipCountObj = cacheService.get(ipCountKey);
            int currentIpCount = ipCountObj != null ? (Integer) ipCountObj : 0;

            if (currentIpCount > CommonConst.SAVE_MAX_COUNT) {
                log.info("IP保存超限：ip={}, 当前次数：{}", ip, currentIpCount);
                flag = true;
            } else {
                // 增加IP保存计数
                cacheService.set(ipCountKey, currentIpCount + 1, CommonConst.SAVE_EXPIRE);
            }
        } catch (Exception e) {
            log.error("检查IP保存频率限制时发生错误", e);
            // 发生错误时不阻止操作，但记录日志
        }

        if (flag) {
            throw new PoetryRuntimeException("今日提交次数已用尽，请一天后再来！");
        }

        return joinPoint.proceed();
    }
}
