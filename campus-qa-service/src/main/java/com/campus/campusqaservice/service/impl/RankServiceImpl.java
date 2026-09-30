package com.campus.campusqaservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.campusqamapper.mapper.AnswerMapper;
import com.campus.campusqamapper.mapper.QuestionMapper;
import com.campus.campusqamapper.mapper.UserMapper;
import com.campus.campusqapojo.dto.AnswererStatDTO;
import com.campus.campusqapojo.entity.Question;
import com.campus.campusqapojo.entity.User;
import com.campus.campusqapojo.vo.AnswererRankVO;
import com.campus.campusqapojo.vo.HotQuestionVO;
import com.campus.campusqaservice.service.RankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ClassName: RankServiceImpl
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/24 13:36
 * Version:1.0
 */

@Service
public class RankServiceImpl implements RankService {

    private final QuestionMapper questionMapper;

    private final UserMapper userMapper;

    private final AnswerMapper answerMapper;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 专门用来存"对象"的模板（JSON 序列化），榜单的 ZSet 不用它
     */
    private final RedisTemplate<String, Object> redisTemplate;

    private static final Logger log = LoggerFactory.getLogger(RankServiceImpl.class);

    public RankServiceImpl(QuestionMapper questionMapper,
                           UserMapper userMapper,
                           AnswerMapper answerMapper,
                           StringRedisTemplate stringRedisTemplate,
                           RedisTemplate<String, Object> redisTemplate) {
        this.questionMapper = questionMapper;
        this.userMapper = userMapper;
        this.answerMapper = answerMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisTemplate = redisTemplate;
    }

    private static final String HOT_KEY_PREFIX = "hot:question:";

    private static final DateTimeFormatter KEY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 候选池上限
    private static final int CANDIDATE_LIMIT = 1000;

    // 榜单保留条数
    private static final int RANK_SIZE = 1000;

    // 默认返回条数 / 单次最多返回条数
    private static final int DEFAULT_LIMIT = 10;

    private static final int MAX_LIMIT = 50;

    private static final long HOT_KEY_TTL_HOURS = 48;

    // 优秀回答者榜：固定 key，不按天清零 —— "长期贡献"每天归零没有意义
    private static final String ANSWERER_RANK_KEY = "rank:answerer";

    // 榜单明细（ZSet 只能存 member+score，回答数/采纳数这些放这里，JSON 格式）
    private static final String ANSWERER_STAT_KEY = "rank:answerer:stat";

    // TTL 比热榜长：靠定时任务持续续期，只要任务在跑就不会过期
    private static final long ANSWERER_KEY_TTL_HOURS = 72;

    @Override
    public void refreshHotRank() {
        // 1.今天的榜单 key：hot:question:20260924
        String key = hotKey(LocalDate.now());

        // 2.取候选池（不能全表！）
        QueryWrapper<Question> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0);
        wrapper.ge("create_time", LocalDateTime.now().minusDays(7));
        wrapper.orderByDesc("view_count");
        wrapper.last("LIMIT " + CANDIDATE_LIMIT);
        List<Question> candidates = questionMapper.selectList(wrapper);

        // 3.算分 → 组装 (member, score) 二元组
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        LocalDateTime now = LocalDateTime.now();
        for (Question q : candidates) {
            double score = calcHotScore(q, now);
            tuples.add(ZSetOperations.TypedTuple.of(String.valueOf(q.getId()), score));
        }

        if (tuples.isEmpty()) {
            // 没数据就别去碰 Redis；但必须留日志 —— 否则"候选池为空"这条路径在日志里完全隐形，
            // 榜单不更新时既没有 SQL、也没有 Redis 命令，排查只能靠猜（这个坑实际踩过）
            log.warn("热榜候选池为空，跳过本轮重算：请检查 question 表近 7 天是否有 status=0 的数据");
            return;
        }

        // 4.一条命令批量写入（不是循环 1000 次）
        stringRedisTemplate.opsForZSet().add(key, tuples);

        // 5.截断 + 设过期
        stringRedisTemplate.opsForZSet().removeRange(key, 0, -(RANK_SIZE + 1));
        stringRedisTemplate.expire(key, Duration.ofHours(HOT_KEY_TTL_HOURS));
    }

    @Override
    public List<HotQuestionVO> getHotRank(int limit) {
        // 1.限额兜底：limit 来自 URL，属于外部输入，必须夹到合法区间
        int size = normalizeLimit(limit);

        // 2.读 TopN（Redis 故障时降级为空榜单，不往上抛）
        Set<ZSetOperations.TypedTuple<String>> tuples;
        try {
            tuples = readTopFromRedis(size);
        } catch (DataAccessException e) {
            // 热榜是辅助功能：Redis 挂了只让榜单空白，不能把首页拖成 500
            log.warn("热榜读取失败，已降级返回空榜单，size={}，原因={}", size, e.getMessage());
            return Collections.emptyList();
        }

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        // 3.收集 questionId（member 就是问题 id 的字符串形式）
        List<Long> questionIds = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            questionIds.add(Long.valueOf(tuple.getValue()));
        }

        // 4.批量查问题 → Map（避免 N+1）
        List<Question> questions = questionMapper.selectByIds(questionIds);
        Map<Long, Question> questionMap = new HashMap<>();
        for (Question question : questions) {
            questionMap.put(question.getId(), question);
        }

        // 5.批量查作者 → Map（先去重再查）
        Set<Long> userIds = new LinkedHashSet<>();
        for (Question question : questions) {
            userIds.add(question.getUserId());
        }
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectByIds(userIds);
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
        }

        // 6.严格按 ZSet 的顺序组装（顺序必须跟着榜单走，不能跟着 Map 走）
        List<HotQuestionVO> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long questionId = Long.valueOf(tuple.getValue());
            Question question = questionMap.get(questionId);
            if (question == null) {
                // Redis 里有、库里没有：问题已被物理删除，跳过
                continue;
            }

            HotQuestionVO vo = new HotQuestionVO();
            vo.setRank(result.size() + 1);
            vo.setQuestionId(questionId);
            vo.setTitle(question.getTitle());
            vo.setLikeCount(question.getLikeCount());
            vo.setAnswerCount(question.getAnswerCount());
            vo.setViewCount(question.getViewCount());
            vo.setScore(tuple.getScore());

            User author = userMap.get(question.getUserId());
            if (author != null) {
                vo.setNickname(author.getNickname());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public void refreshAnswererRank() {
        // 1.聚合统计 + 算分 + 取 TopN 全部交给一条 SQL 完成
        //   这里没有"最近 7 天"这种候选池条件：贡献榜统计的是全部历史回答，
        //   GROUP BY 会扫全表，数据量大了要靠索引或统计表优化（见日记记录）
        List<AnswererStatDTO> stats = answerMapper.selectAnswererStats(CANDIDATE_LIMIT);
        if (stats.isEmpty()) {
            // 同理：这条 return 也要留痕，否则"榜单一直不更新"排查时毫无线索
            log.warn("优秀回答者榜候选为空，跳过本轮重算：请检查 answer 表是否有 status=0 的数据");
            return;
        }

        // 2.先把明细写进 String key（JSON 序列化），再写榜单
        //   两个 key 无法用单条命令原子写入，这个顺序下最坏情况是"明细写了、榜单没写"，
        //   明细成了孤儿数据（无害，下次重算就覆盖），不会出现"榜单有新数据、明细是旧数据"的错配
        redisTemplate.opsForValue().set(ANSWERER_STAT_KEY, stats,
                Duration.ofHours(ANSWERER_KEY_TTL_HOURS));

        // 3.组装 (userId, score) 二元组：榜单主体从"内容"变成了"人"
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        for (AnswererStatDTO stat : stats) {
            tuples.add(ZSetOperations.TypedTuple.of(String.valueOf(stat.getUserId()), stat.getScore()));
        }

        // 4.一条命令批量写入
        stringRedisTemplate.opsForZSet().add(ANSWERER_RANK_KEY, tuples);

        // 5.截断 + 续期（两个 key 的 TTL 都设上，避免一个先过期导致错配或内存泄漏）
        stringRedisTemplate.opsForZSet().removeRange(ANSWERER_RANK_KEY, 0, -(RANK_SIZE + 1));
        stringRedisTemplate.expire(ANSWERER_RANK_KEY, Duration.ofHours(ANSWERER_KEY_TTL_HOURS));
    }

    @Override
    public List<AnswererRankVO> getAnswererRank(int limit) {
        int size = normalizeLimit(limit);

        // 1.读 TopN（Redis 故障时降级为空榜单）
        Set<ZSetOperations.TypedTuple<String>> tuples;
        try {
            // 固定 key，没有"跨日兜底"这一步 —— key 设计变了，读取逻辑也跟着变简单
            tuples = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(ANSWERER_RANK_KEY, 0, size - 1);
        } catch (DataAccessException e) {
            log.warn("优秀回答者榜读取失败，已降级返回空榜单，size={}，原因={}", size, e.getMessage());
            return Collections.emptyList();
        }

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        // 2.收集 userId（member 就是回答者 id）
        Set<Long> userIds = new LinkedHashSet<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            userIds.add(Long.valueOf(tuple.getValue()));
        }

        // 3.批量查用户 → Map（避免 N+1）
        List<User> users = userMapper.selectByIds(userIds);
        Map<Long, User> userMap = new HashMap<>();
        for (User user : users) {
            userMap.put(user.getId(), user);
        }

        // 4.读明细缓存 → Map（单独降级：明细丢了只少几个字段，榜单本身照常返回）
        Map<Long, AnswererStatDTO> statMap = readAnswererStatMap();

        // 5.严格按 ZSet 的顺序组装
        List<AnswererRankVO> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long userId = Long.valueOf(tuple.getValue());
            User user = userMap.get(userId);
            // 用户已被物理删除，或账号已被禁用 → 不上榜
            if (user == null || Integer.valueOf(1).equals(user.getStatus())) {
                continue;
            }

            AnswererRankVO vo = new AnswererRankVO();
            vo.setRank(result.size() + 1);
            vo.setUserId(userId);
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setScore(tuple.getScore());

            // 明细缓存缺失/失效时这几个字段保持 null，前端自行兜底展示
            AnswererStatDTO stat = statMap.get(userId);
            if (stat != null) {
                vo.setAnswerCount(stat.getAnswerCount());
                vo.setAcceptedCount(stat.getAcceptedCount());
                vo.setLikeSum(stat.getLikeSum());
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 读榜单明细缓存，转成 userId → 统计数据的 Map
     * <p>
     * 这里用 RedisTemplate&lt;String, Object&gt;（JSON 序列化）而不是 StringRedisTemplate：
     * 存的是对象列表，需要序列化器帮我们处理"对象 ↔ JSON"的转换，
     * 序列化结果里带 @class 类型信息，所以能把 JSON 还原回 List&lt;AnswererStatDTO&gt;
     */
    @SuppressWarnings("unchecked")
    private Map<Long, AnswererStatDTO> readAnswererStatMap() {
        Map<Long, AnswererStatDTO> statMap = new HashMap<>();
        List<AnswererStatDTO> stats;
        try {
            stats = (List<AnswererStatDTO>) redisTemplate.opsForValue().get(ANSWERER_STAT_KEY);
        } catch (DataAccessException e) {
            // 明细属于"锦上添花"，读失败不该影响榜单主体
            log.warn("优秀回答者榜明细读取失败，本次仅返回排名与分数，原因={}", e.getMessage());
            return statMap;
        }
        if (stats == null) {
            return statMap;
        }
        for (AnswererStatDTO stat : stats) {
            statMap.put(stat.getUserId(), stat);
        }
        return statMap;
    }

    /**
     * 从 Redis 读 TopN（含跨日兜底）
     * 单独抽出来，是为了让降级的 try-catch 只包住 Redis 这一段 ——
     * 后面查 MySQL 出问题该抛还得抛，不能被一起吞掉
     */
    private Set<ZSetOperations.TypedTuple<String>> readTopFromRedis(int size) {
        // 今天的 key；跨日或 Redis 刚重启导致今日榜还没生成时，退到昨天的榜
        String key = hotKey(LocalDate.now());
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(key))) {
            key = hotKey(LocalDate.now().minusDays(1));
        }
        // ZREVRANGE ... WITHSCORES（从高分到低分）
        return stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, size - 1);
    }

    /**
     * 榜单 key：hot:question:yyyyMMdd
     * 写入和读取共用同一个方法，避免两边格式写不一致
     */
    private String hotKey(LocalDate date) {
        return HOT_KEY_PREFIX + date.format(KEY_DATE_FORMAT);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private double calcHotScore(Question q, LocalDateTime now) {
        // create_time 晚于 now（时区异常/时钟回拨）会让 Pow 得到 NaN，Redis 会拒绝写入，这里夹到 0
        double hours = Math.max(0, Duration.between(q.getCreateTime(), now).toMinutes() / 60.0);
        return (q.getLikeCount() * 3 + q.getAnswerCount() * 5 + q.getViewCount() * 0.1)
                / Math.pow(hours + 2, 1.5);
    }
}
