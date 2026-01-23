package com.ld.poetry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 平台线程池配置
 * 用于不适合虚拟线程的场景（如：CPU密集型任务、涉及Native调用的任务）
 *
 * @author LeapYa
 * @since 2026-01-17
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * CPU密集型任务专用线程池（平台线程）
     * 适用于二维码生成、图像处理等涉及大量计算或Native调用的任务
     * 避免在虚拟线程中执行此类任务导致Carrier Thread被Pin住
     */
    @Bean("cpuIntensiveExecutor")
    public Executor cpuIntensiveExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 获取CPU核心数
        int processors = Runtime.getRuntime().availableProcessors();
        
        // 核心线程数：CPU核心数 + 1
        executor.setCorePoolSize(processors + 1);
        // 最大线程数：CPU核心数 * 2
        executor.setMaxPoolSize(processors * 2);
        // 队列容量
        executor.setQueueCapacity(100);
        // 线程名前缀
        executor.setThreadNamePrefix("cpu-platform-");
        // 拒绝策略：由调用者所在的线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }
}
