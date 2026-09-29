package com.campus.campusqaweb.task;

import com.campus.campusqaservice.service.RankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * ClassName: RankRefreshTask
 * Description: 榜单重算任务（启动预热 + 定时刷新，带 Redis 分布式锁）
 * Author: SuperXia
 * Datetime :2026/9/24
 * Version:1.0
 */
@Component
public class RankRefreshTask implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RankRefreshTask.class);

    /**
     * 锁 key 按"要刷新哪张榜"细分，而不是所有任务共用一把锁
     * 共用一把锁的后果：回答者榜（每小时整点）如果恰好撞上正在跑的热榜（每 5 分钟），
     * 它会抢不到锁 → 直接跳过 → 这一小时的贡献榜就不刷新了，而且没有任何报错。
     * 锁的粒度要对齐资源的粒度，别做"一把大锁管所有"。
     */
    private static final String LOCK_KEY_HOT = "lock:rank:refresh:hot";
    private static final String LOCK_KEY_ANSWERER = "lock:rank:refresh:answerer";

    /**
     * 锁的兜底过期时间，两个方向都要卡住：
     * ① 必须远大于任务最大耗时（否则任务没跑完锁就自动过期 → 别的实例同时进来 → 锁失效）
     * ② 必须小于调度间隔（否则进程崩了留下的锁会挡住下一轮调度）
     * 热榜任务耗时约 1 秒、调度间隔 5 分钟，取 1 分钟：耗时的 60 倍、间隔的 1/5。
     * 两个风险里"锁挡住后续调度"更严重（榜单直接不更新了），所以宁可取短一点 ——
     * 我们的重算是全量覆盖写，幂等的，重复跑的代价只是浪费资源，不会写坏数据。
     */
    private static final Duration LOCK_TTL = Duration.ofMinutes(1);

    /**
     * 释放锁的 Lua 脚本：先比对持有者，确认是自己的锁才删
     * 为什么必须用 Lua：释放要"比较 + 删除"两步，而 Redis 的单条命令才保证原子性。
     * 如果写成先 GET 再 DEL，两步之间锁可能已经因为超时被自动释放、且被别的实例重新持有，
     * 此时 DEL 掉的是"别人的锁" —— 第三个实例就能进来，锁形同虚设。
     */
    private static final RedisScript<Long> UNLOCK_SCRIPT = RedisScript.of(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final RankService rankService;

    private final StringRedisTemplate stringRedisTemplate;

    public RankRefreshTask(RankService rankService, StringRedisTemplate stringRedisTemplate) {
        this.rankService = rankService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 启动预热：应用启动完成后立刻生成一次榜单
     * 为什么需要：定时任务有"空窗期" —— 服务在 14:02 启动，热榜要等到 14:05 的 cron
     * 触发才会出现，这 3 分钟里 GET /rank/hot 返回的是空列表。
     * Redis 重启后同理，最长要等一个完整周期。
     * 关键约束：run() 里抛异常会导致整个应用启动失败 —— 预热是辅助功能，
     * 绝不该拖垮主流程。所以这里调用的两个方法都自带 try-catch，异常在方法内部就被消化掉了
     * 多实例部署时预热也会重复执行 —— 靠下面的分布式锁保证集群里只有一个实例真正重算
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
        refreshWithLock(LOCK_KEY_HOT, "热榜", () -> rankService.refreshHotRank());
    }

    /**
     * 优秀回答者榜：每小时重算一次
     * <p>
     * 频率刻意比热榜低：它的聚合 SQL 是 GROUP BY 全表扫描，
     * 而"长期贡献"变化本来就慢，跑太勤纯属浪费
     */
    @Scheduled(cron = "0 0 * * * ?", zone = "Asia/Shanghai")
    public void refreshAnswererRank() {
        refreshWithLock(LOCK_KEY_ANSWERER, "优秀回答者榜", () -> rankService.refreshAnswererRank());
    }

    /**
     * 抢到锁才重算；抢不到说明集群里别的实例正在做同一件事，直接跳过
     *
     * @param lockKey  该榜对应的锁 key
     * @param rankName 榜单名（只用于日志）
     * @param task     真正的重算动作
     */
    private void refreshWithLock(String lockKey, String rankName, Runnable task) {
        // token 用来标识"这把锁是我加的"，释放时靠它避免误删别人的锁
        String token = UUID.randomUUID().toString();
        boolean locked = false;
        try {
            if (!tryLock(lockKey, token)) {
                // 这不是异常，是集群下的正常现象：别的实例已经在跑了，本实例跳过
                log.info("{}已有其他实例在重算，本实例跳过", rankName);
                return;
            }
            locked = true;
            task.run();
        } catch (Exception e) {
            // 整个方法体都在 try 里，包括"抢锁"这一步 —— Redis 不可用时 tryLock 会抛异常，
            // 如果它漏在外面，预热阶段就会让整个应用启动失败（注释里那条约束要求这里必须兜住）
            log.error("{}重算失败", rankName, e);
        } finally {
            // 必须放 finally：任务抛异常时也要把锁释放掉，否则要等 TTL 到期才解锁
            // locked 判断：没抢到锁时不能去删（否则会删掉别人的锁）
            if (locked) {
                unlock(lockKey, token);
            }
        }
    }

    /**
     * 加锁：SET key token NX PX ttl
     * <p>
     * 一条命令同时完成"key 不存在才设置"和"设置过期时间"两件事。
     * 如果拆成 SETNX + EXPIRE 两步，中间进程挂掉就会留下一个没有过期时间的锁 —— 死锁。
     * setIfAbsent(key, value, timeout) 底层就是 SET ... NX PX ...
     *
     * @return true 加锁成功；false 锁已被别人持有
     */
    private boolean tryLock(String lockKey, String token) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, token, LOCK_TTL);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放锁：只有持有者自己才能释放
     * <p>
     * 用 Lua 保证"比较 + 删除"的原子性，详见 UNLOCK_SCRIPT 的注释
     */
    private void unlock(String lockKey, String token) {
        try {
            stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), token);
        } catch (Exception e) {
            // 释放锁失败不该影响主流程：锁有 TTL 兜底，最多 1 分钟后自动释放
            log.warn("释放锁失败，key={}", lockKey, e);
        }
    }
}
