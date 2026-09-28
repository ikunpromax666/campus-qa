package com.campus.campusqaweb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * ClassName: ScheduleConfig
 * Description: 定时任务（Spring Task）配置
 * Author: SuperXia
 * Datetime :2026/9/24
 * Version:1.0
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {

    /**
     * 定时任务线程池
     *
     * 不配这个 bean 的话，Spring Task 默认只有 1 个线程：
     * 所有 @Scheduled 任务串行排队执行 —— 一个任务卡住（比如查库超时 30 秒），
     * 会把其他所有任务的执行时间整体往后推，甚至直接错过执行窗口。
     * 也可以不写这段、改在 application.yml 里配 spring.task.scheduling.pool.size=4，
     * 但显式声明能顺带把"优雅停机"一起配上
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("campus-task-");
        // 优雅停机：应用关闭时等在跑的任务执行完，避免"写了一半就被 kill"
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
