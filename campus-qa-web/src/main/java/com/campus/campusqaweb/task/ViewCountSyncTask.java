package com.campus.campusqaweb.task;

import com.campus.campusqamapper.mapper.QuestionMapper;
import com.campus.campusqaservice.service.impl.QuestionServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * ClassName: ViewCountSyncTask
 * Description: 浏览量缓冲回写任务（Redis 增量 → MySQL 定时批量落库）
 *
 * 设计要点：
 * 1. 计数暂存在 question:view:{id}（String，INCR），本任务每分钟把增量原子搬回 MySQL。
 * 2. 搬运用 GETDEL（getAndDelete）而不是先 GET 再 DEL：
 *    GET 和 DEL 是两步，中间别的实例/线程可能 INCR 了新增量，
 *    DEL 会把这部分未读取的增量一起删掉 —— 丢计数。GETDEL 单命令原子，无此窗口。
 * 3. 失败补偿：DB 写失败时把 delta INCR 回 Redis，等下一轮重试，宁可重复试也不丢数。
 *    极端情况（GETDEL 之后进程崩溃）会丢最多一分钟的增量 —— 浏览量属允许少量误差的
 *    统计数据，用这点误差换掉分布式事务，是值得的取舍。
 * 4. 不需要分布式锁：GETDEL 的原子性保证同一时刻只有一个实例能拿走某个 key 的增量，
 *    多实例并发跑也不会重复计数，与榜单重算任务（全量覆盖写、需要锁）是两种模型。
 * Author: SuperXia
 * Datetime :2026/10/3
 * Version:1.0
 */
@Component
public class ViewCountSyncTask {

    private static final Logger log = LoggerFactory.getLogger(ViewCountSyncTask.class);

    private static final String KEY_PREFIX = QuestionServiceImpl.VIEW_COUNT_KEY_PREFIX;

    private final StringRedisTemplate stringRedisTemplate;

    private final QuestionMapper questionMapper;

    public ViewCountSyncTask(StringRedisTemplate stringRedisTemplate, QuestionMapper questionMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.questionMapper = questionMapper;
    }

    /**
     * 每分钟回写一次。fixedDelay（上一次结束后计时）而非 fixedRate（按点开始计时）：
     * 上一轮没跑完时下一轮不会叠加进来，避免任务自身并发。
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void syncViewCounts() {
        Set<String> keys = scanKeys();
        if (keys.isEmpty()) {
            return;
        }
        int success = 0;
        for (String key : keys) {
            Long delta = null;
            try {
                // 原子取出并删除：取出的增量归本实例负责，其他实例看不到这个 key 了
                // StringRedisTemplate 泛型是 String，getAndDelete 返回 String，需自行转 Long
                String deltaStr = stringRedisTemplate.opsForValue().getAndDelete(key);
                delta = deltaStr == null ? null : Long.valueOf(deltaStr);
                if (delta == null || delta == 0) {
                    continue;
                }
                Long questionId = Long.valueOf(key.substring(KEY_PREFIX.length()));
                // SQL 层原子加（view_count = view_count + delta），与降级直写路径并发也不丢更新
                questionMapper.incrementViewCount(questionId, delta);
                success++;
            } catch (Exception e) {
                // 补偿：把已取出的增量加回 Redis，等下一轮重试
                if (delta != null && delta > 0) {
                    try {
                        stringRedisTemplate.opsForValue().increment(key, delta);
                    } catch (DataAccessException ignored) {
                        // Redis 也挂了：本轮增量只能记日志告警，由人工对账兜底
                        log.error("浏览量回写失败且补偿失败 key={} delta={}", key, delta);
                    }
                }
                log.error("浏览量回写失败 key={} delta={}", key, delta, e);
            }
        }
        log.info("浏览量回写完成：{} 个 key", success);
    }

    /**
     * 用 SCAN 遍历而不是 KEYS *：KEYS 是 O(N) 且阻塞 Redis 主线程，
     * key 多了会把整个 Redis 卡住；SCAN 分批游标遍历，单次只扫一小批，不影响线上读写。
     */
    private Set<String> scanKeys() {
        Set<String> result = stringRedisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keys = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(200).build();
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return keys;
        });
        return result == null ? new HashSet<>() : result;
    }
}
