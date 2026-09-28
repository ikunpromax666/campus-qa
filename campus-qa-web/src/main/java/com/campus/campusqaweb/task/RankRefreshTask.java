package com.campus.campusqaweb.task;

import com.campus.campusqaservice.service.RankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ClassName: RankRefreshTask
 * Description: 榜单重算任务（启动预热 + 定时刷新）
 * Author: SuperXia
 * Datetime :2026/9/24
 * Version:1.0
 */
@Component
public class RankRefreshTask implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RankRefreshTask.class);

    private final RankService rankService;

    public RankRefreshTask(RankService rankService) {
        this.rankService = rankService;
    }

    /**
     * 启动预热：应用启动完成后立刻生成一次榜单
     * 为什么需要：定时任务有"空窗期" —— 服务在 14:02 启动，热榜要等到 14:05 的 cron
     * 触发才会出现，这 3 分钟里 GET /rank/hot 返回的是空列表。
     * Redis 重启后同理，最长要等一个完整周期。
     * 关键约束：run() 里抛异常会导致整个应用启动失败 —— 预热是辅助功能，
     * 绝不该拖垮主流程。所以这里调用的两个方法都自带 try-catch，异常在方法内部就被消化掉了
     *多实例部署时预热也会重复执行（和定时任务同一个问题），
     * 需要靠分布式锁或调度中心保证只有一个实例预热
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("榜单启动预热开始");
        refreshHotRank();
        refreshAnswererRank();
        log.info("榜单启动预热结束");
    }

    /**
     * 热榜：每 5 分钟重算一次
     * <p>
     * cron 是 6 段：秒 分 时 日 月 周（和 Linux crontab 的 5 段不一样，最前面多了"秒"）
     * <p>
     * zone 显式指定东八区：不写就用 JVM 默认时区 —— 注意 yml 里的 jackson.time-zone
     * 只管 JSON 序列化，管不到 cron！服务器时区是 UTC 的话，"0 0 0 * * ?" 会在北京时间 8 点跑
     */
    @Scheduled(cron = "0 0/5 * * * ?", zone = "Asia/Shanghai")
    public void refreshHotRank() {
        try {
            rankService.refreshHotRank();
        } catch (Exception e) {
            // 任务内部兜底：让日志能说清"是哪个任务挂了"，也避免异常信息被框架的通用日志淹没
            log.error("热榜重算失败", e);
        }
    }

    /**
     * 优秀回答者榜：每小时重算一次
     * <p>
     * 频率刻意比热榜低：它的聚合 SQL 是 GROUP BY 全表扫描，
     * 而"长期贡献"变化本来就慢，跑太勤纯属浪费
     */
    @Scheduled(cron = "0 0 * * * ?", zone = "Asia/Shanghai")
    public void refreshAnswererRank() {
        try {
            rankService.refreshAnswererRank();
        } catch (Exception e) {
            log.error("优秀回答者榜重算失败", e);
        }
    }
}
