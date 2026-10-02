package com.campus.campusqaservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.campusqacommon.context.UserContext;
import com.campus.campusqacommon.exception.BusinessException;
import com.campus.campusqacommon.result.ResultCode;
import com.campus.campusqamapper.mapper.AnswerMapper;
import com.campus.campusqamapper.mapper.CategoryMapper;
import com.campus.campusqamapper.mapper.FavoriteMapper;
import com.campus.campusqamapper.mapper.LikeRecordMapper;
import com.campus.campusqamapper.mapper.QuestionMapper;
import com.campus.campusqamapper.mapper.QuestionTagMapper;
import com.campus.campusqamapper.mapper.TagMapper;
import com.campus.campusqamapper.mapper.UserMapper;
import com.campus.campusqapojo.dto.FavoriteToggleDTO;
import com.campus.campusqapojo.dto.LikeToggleDTO;
import com.campus.campusqapojo.entity.Answer;
import com.campus.campusqapojo.entity.Category;
import com.campus.campusqapojo.entity.Favorite;
import com.campus.campusqapojo.entity.LikeRecord;
import com.campus.campusqapojo.entity.Question;
import com.campus.campusqapojo.entity.QuestionTag;
import com.campus.campusqapojo.entity.Tag;
import com.campus.campusqapojo.entity.User;
import com.campus.campusqapojo.vo.MyAnswerVO;
import com.campus.campusqapojo.vo.PageResultVO;
import com.campus.campusqapojo.vo.QuestionListVO;
import com.campus.campusqapojo.vo.ToggleResultVO;
import com.campus.campusqaservice.service.InteractionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName: InteractionServiceImpl
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/23 15:34
 * Version:1.0
 */
@Service
public class InteractionServiceImpl implements InteractionService {

    /** 目标类型：1问题 2回答 */
    private static final Integer TARGET_QUESTION = 1;
    private static final Integer TARGET_ANSWER = 2;

    /** 行为类型：1点赞 2踩 */
    private static final Integer ACTION_LIKE = 1;
    private static final Integer ACTION_DISLIKE = 2;

    /** 无状态（取消之后） */
    private static final Integer STATUS_NONE = 0;

    /** 收藏状态：1已收藏 0未收藏 */
    private static final Integer FAVORITED = 1;
    private static final Integer NOT_FAVORITED = 0;

    /** 两张表的逻辑删除状态不一致：question 是 2，answer 是 1 */
    private static final Integer QUESTION_STATUS_DELETED = 2;
    private static final Integer ANSWER_STATUS_DELETED = 1;

    /** 分页上限：防止 ?size=1000000 一页打爆数据库（面试考点：分页必须夹 size） */
    private static final int MAX_PAGE_SIZE = 100;

    private final QuestionMapper questionMapper;
    private final AnswerMapper answerMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final FavoriteMapper favoriteMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final QuestionTagMapper questionTagMapper;
    private final TagMapper tagMapper;

    public InteractionServiceImpl(QuestionMapper questionMapper,
                                  AnswerMapper answerMapper,
                                  LikeRecordMapper likeRecordMapper,
                                  FavoriteMapper favoriteMapper,
                                  UserMapper userMapper,
                                  CategoryMapper categoryMapper,
                                  QuestionTagMapper questionTagMapper,
                                  TagMapper tagMapper) {
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.favoriteMapper = favoriteMapper;
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.questionTagMapper = questionTagMapper;
        this.tagMapper = tagMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ToggleResultVO toggleLike(LikeToggleDTO dto) {
        return doLikeToggle(dto.getTargetId(), dto.getTargetType(), ACTION_LIKE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ToggleResultVO toggleDislike(LikeToggleDTO dto) {
        return doLikeToggle(dto.getTargetId(), dto.getTargetType(), ACTION_DISLIKE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ToggleResultVO toggleFavorite(FavoriteToggleDTO dto) {
        Long currentUserId = UserContext.requireUserId();

        // 收藏只针对问题：问题必须存在且未删除
        Question question = questionMapper.selectById(dto.getQuestionId());
        if (question == null || QUESTION_STATUS_DELETED.equals(question.getStatus())) {
            throw new BusinessException(ResultCode.QUESTION_NOT_FOUND);
        }

        // 查当前用户是否已收藏
        Favorite current = favoriteMapper.selectOne(new QueryWrapper<Favorite>()
                .eq("user_id", currentUserId)
                .eq("question_id", dto.getQuestionId()));

        int finalStatus;
        if (current == null) {
            // 未收藏 → 收藏
            Favorite favorite = new Favorite();
            favorite.setUserId(currentUserId);
            favorite.setQuestionId(dto.getQuestionId());
            try {
                favoriteMapper.insert(favorite);
            } catch (DuplicateKeyException e) {
                // 并发下另一个请求已插入同一条记录 → 结果一致，视为收藏成功
            }
            finalStatus = FAVORITED;
        } else {
            // 已收藏 → 取消收藏
            favoriteMapper.deleteById(current.getId());
            finalStatus = NOT_FAVORITED;
        }

        ToggleResultVO vo = new ToggleResultVO();
        vo.setTargetId(dto.getQuestionId());
        vo.setToggleStatus(finalStatus);
        // 收藏不影响点赞数，likeCount 保持 null
        return vo;
    }

    /**
     * 点赞/踩的公共逻辑。
     *
     * @param targetId   目标ID（问题ID或回答ID）
     * @param targetType 目标类型：1问题 2回答
     * @param actionType 本次动作：1点赞 2踩
     */
    private ToggleResultVO doLikeToggle(Long targetId, Integer targetType, Integer actionType) {
        Long currentUserId = UserContext.requireUserId();

        // ① 目标必须存在且未删除
        checkTarget(targetId, targetType);

        // ② 查当前用户对该目标的记录（唯一索引保证最多一条，所以用 selectOne）
        LikeRecord current = likeRecordMapper.selectOne(new QueryWrapper<LikeRecord>()
                .eq("user_id", currentUserId)
                .eq("target_id", targetId)
                .eq("target_type", targetType));

        // ③ 状态机：结果只归结为两个变量 —— 最终状态 + 计数增量
        Integer finalStatus;
        int delta;

        if (current == null) {
            // 情况 A：从没点过 → 新增一条记录
            LikeRecord record = new LikeRecord();
            record.setUserId(currentUserId);
            record.setTargetId(targetId);
            record.setTargetType(targetType);
            record.setActionType(actionType);
            try {
                likeRecordMapper.insert(record);
            } catch (DuplicateKeyException e) {
                // 并发下另一个请求已经插入 → 唯一索引兜底，提示重试
                throw new BusinessException(ResultCode.INTERACTION_CONFLICT);
            }
            finalStatus = actionType;
            delta = ACTION_LIKE.equals(actionType) ? 1 : 0;

        } else if (current.getActionType().equals(actionType)) {
            // 情况 B：同一个动作又点一次 → 取消，删除这条记录（主键已在手，按主键删）
            likeRecordMapper.deleteById(current.getId());
            finalStatus = STATUS_NONE;
            delta = ACTION_LIKE.equals(actionType) ? -1 : 0;

        } else {
            // 情况 C：反动作 → 切换 action_type（只 set 要改的字段，避免覆盖别的字段）
            LikeRecord update = new LikeRecord();
            update.setId(current.getId());
            update.setActionType(actionType);
            likeRecordMapper.updateById(update);
            finalStatus = actionType;
            // 踩→赞 要 +1；赞→踩 要 -1
            delta = ACTION_LIKE.equals(actionType) ? 1 : -1;
        }

        // ④ 更新目标的 like_count（delta 为 0 时不动数据库）
        if (delta != 0) {
            updateLikeCount(targetId, targetType, delta);
        }

        // ⑤ 回读最新计数并组装返回值
        ToggleResultVO vo = new ToggleResultVO();
        vo.setTargetId(targetId);
        vo.setToggleStatus(finalStatus);
        vo.setLikeCount(readLikeCount(targetId, targetType));
        return vo;
    }

    /**
     * 校验目标存在且未被删除。注意两张表的 status 语义不同：
     * question：0正常 1已关闭 2已删除（已关闭仍可点赞，只有删除才拒绝）
     * answer：  0正常 1已删除
     */
    private void checkTarget(Long targetId, Integer targetType) {
        if (TARGET_QUESTION.equals(targetType)) {
            Question question = questionMapper.selectById(targetId);
            if (question == null || QUESTION_STATUS_DELETED.equals(question.getStatus())) {
                throw new BusinessException(ResultCode.QUESTION_NOT_FOUND);
            }
        } else if (TARGET_ANSWER.equals(targetType)) {
            Answer answer = answerMapper.selectById(targetId);
            if (answer == null || ANSWER_STATUS_DELETED.equals(answer.getStatus())) {
                throw new BusinessException(ResultCode.ANSWER_NOT_FOUND);
            }
        } else {
            throw new BusinessException(ResultCode.TARGET_NOT_EXIST);
        }
    }

    /**
     * 原子更新点赞数：一条 SQL 内完成读+写，避免"先查后写"的丢失更新。
     * 递减时加 like_count >= 1 条件，防止 INT UNSIGNED 列减成负数报错。
     */
    private void updateLikeCount(Long targetId, Integer targetType, int delta) {
        if (TARGET_QUESTION.equals(targetType)) {
            UpdateWrapper<Question> wrapper = new UpdateWrapper<>();
            wrapper.eq("id", targetId);
            if (delta > 0) {
                wrapper.setSql("like_count = like_count + 1");
            } else {
                wrapper.ge("like_count", 1);
                wrapper.setSql("like_count = like_count - 1");
            }
            questionMapper.update(null, wrapper);
        } else {
            UpdateWrapper<Answer> wrapper = new UpdateWrapper<>();
            wrapper.eq("id", targetId);
            if (delta > 0) {
                wrapper.setSql("like_count = like_count + 1");
            } else {
                wrapper.ge("like_count", 1);
                wrapper.setSql("like_count = like_count - 1");
            }
            answerMapper.update(null, wrapper);
        }
    }

    /** 回读最新的点赞数（必须在事务内读，才能拿到本次修改后的值） */
    private Integer readLikeCount(Long targetId, Integer targetType) {
        if (TARGET_QUESTION.equals(targetType)) {
            Question question = questionMapper.selectById(targetId);
            return question == null ? 0 : question.getLikeCount();
        }
        Answer answer = answerMapper.selectById(targetId);
        return answer == null ? 0 : answer.getLikeCount();
    }

    // ==================== 我的收藏 / 我的点赞 ====================

    @Override
    public PageResultVO<QuestionListVO> myFavorites(Integer page, Integer size) {
        Long userId = UserContext.requireUserId();
        int pageNum = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, MAX_PAGE_SIZE);

        // 收藏记录分页：收藏时间倒序 + id 兜底（unique-key tiebreaker，保证分页稳定）
        IPage<Favorite> favPage = favoriteMapper.selectPage(new Page<>(pageNum, pageSize),
                new QueryWrapper<Favorite>()
                        .eq("user_id", userId)
                        .orderByDesc("create_time")
                        .orderByDesc("id"));

        // 收藏表只存 question_id → 按收藏顺序批量查问题（保持收藏时间的展示顺序）
        List<Long> questionIds = new ArrayList<>();
        for (Favorite fav : favPage.getRecords()) {
            if (!questionIds.contains(fav.getQuestionId())) {
                questionIds.add(fav.getQuestionId());
            }
        }
        return buildQuestionPage(favPage.getTotal(), pageNum, pageSize, questionIds);
    }

    @Override
    public PageResultVO<QuestionListVO> myLikedQuestions(Integer page, Integer size) {
        Long userId = UserContext.requireUserId();
        int pageNum = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, MAX_PAGE_SIZE);

        // 只查"赞"（action_type=1），踩不进列表；target_type=1 限定问题
        IPage<LikeRecord> likePage = likeRecordMapper.selectPage(new Page<>(pageNum, pageSize),
                new QueryWrapper<LikeRecord>()
                        .eq("user_id", userId)
                        .eq("target_type", TARGET_QUESTION)
                        .eq("action_type", ACTION_LIKE)
                        .orderByDesc("create_time")
                        .orderByDesc("id"));

        List<Long> questionIds = new ArrayList<>();
        for (LikeRecord record : likePage.getRecords()) {
            if (!questionIds.contains(record.getTargetId())) {
                questionIds.add(record.getTargetId());
            }
        }
        return buildQuestionPage(likePage.getTotal(), pageNum, pageSize, questionIds);
    }

    @Override
    public PageResultVO<MyAnswerVO> myLikedAnswers(Integer page, Integer size) {
        Long userId = UserContext.requireUserId();
        int pageNum = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, MAX_PAGE_SIZE);

        // 我赞过的回答：target_type=2 且 action_type=1
        IPage<LikeRecord> likePage = likeRecordMapper.selectPage(new Page<>(pageNum, pageSize),
                new QueryWrapper<LikeRecord>()
                        .eq("user_id", userId)
                        .eq("target_type", TARGET_ANSWER)
                        .eq("action_type", ACTION_LIKE)
                        .orderByDesc("create_time")
                        .orderByDesc("id"));

        // 批量查回答（跳过已删除：answer status=1 是删除）
        List<Long> answerIds = new ArrayList<>();
        for (LikeRecord record : likePage.getRecords()) {
            if (!answerIds.contains(record.getTargetId())) {
                answerIds.add(record.getTargetId());
            }
        }
        List<Answer> answers = answerIds.isEmpty() ? Collections.emptyList() : answerMapper.selectByIds(answerIds);
        Map<Long, Answer> answerMap = new HashMap<>();
        for (Answer a : answers) {
            if (!ANSWER_STATUS_DELETED.equals(a.getStatus())) {
                answerMap.put(a.getId(), a);
            }
        }

        // 批量查回答所属问题（拿标题做跳转），批量查回答者（拿昵称头像） —— 防 N+1
        List<Long> questionIds = new ArrayList<>();
        List<Long> userIds = new ArrayList<>();
        for (Answer a : answerMap.values()) {
            if (!questionIds.contains(a.getQuestionId())) {
                questionIds.add(a.getQuestionId());
            }
            if (!userIds.contains(a.getUserId())) {
                userIds.add(a.getUserId());
            }
        }
        List<Question> questions = questionIds.isEmpty() ? Collections.emptyList() : questionMapper.selectByIds(questionIds);
        Map<Long, Question> questionMap = new HashMap<>();
        for (Question q : questions) {
            questionMap.put(q.getId(), q);
        }
        List<User> users = userIds.isEmpty() ? Collections.emptyList() : userMapper.selectByIds(userIds);
        Map<Long, User> userMap = new HashMap<>();
        for (User u : users) {
            userMap.put(u.getId(), u);
        }

        // 按点赞时间的顺序组装（已删除的回答跳过）
        List<MyAnswerVO> voList = new ArrayList<>();
        for (LikeRecord record : likePage.getRecords()) {
            Answer a = answerMap.get(record.getTargetId());
            if (a == null) {
                continue;
            }
            voList.add(buildMyAnswerVO(a, questionMap, userMap));
        }

        PageResultVO<MyAnswerVO> result = new PageResultVO<>();
        result.setRecords(voList);
        result.setTotal(likePage.getTotal());
        result.setPage(pageNum);
        result.setSize(pageSize);
        return result;
    }

    /**
     * 互动记录(收藏/点赞) → 问题列表的公共组装：
     * 先按记录里的 ID 批量查问题（跳过已删除），再批查用户/分类/标签填充展示字段。
     * 面试考点：列表页所有关联数据一律批查（1 次列表 + 4 次批查），
     * 绝不"每条记录再查一次关联"（N+1，列表一长数据库直接被打爆）。
     */
    private PageResultVO<QuestionListVO> buildQuestionPage(long total, int pageNum, int pageSize, List<Long> orderedQuestionIds) {
        // selectByIds 不保序且空集合会拼 IN () 非法 SQL —— 判空 + Map 回填顺序
        List<Question> questions = orderedQuestionIds.isEmpty() ? Collections.emptyList() : questionMapper.selectByIds(orderedQuestionIds);
        Map<Long, Question> questionMap = new HashMap<>();
        for (Question q : questions) {
            // 已删除的问题不展示（收藏/点赞记录还留着，但列表里看不到）
            if (!QUESTION_STATUS_DELETED.equals(q.getStatus())) {
                questionMap.put(q.getId(), q);
            }
        }

        // 按互动记录顺序取出有效问题
        List<Question> orderedQuestions = new ArrayList<>();
        for (Long id : orderedQuestionIds) {
            Question q = questionMap.get(id);
            if (q != null) {
                orderedQuestions.add(q);
            }
        }

        // ---- 以下与 QuestionServiceImpl.list 的批查组装一致 ----
        List<Long> userIds = new ArrayList<>();
        List<Long> categoryIds = new ArrayList<>();
        for (Question q : orderedQuestions) {
            if (!userIds.contains(q.getUserId())) {
                userIds.add(q.getUserId());
            }
            if (!categoryIds.contains(q.getCategoryId())) {
                categoryIds.add(q.getCategoryId());
            }
        }

        List<User> users = userIds.isEmpty() ? Collections.emptyList() : userMapper.selectByIds(userIds);
        Map<Long, User> userMap = new HashMap<>();
        for (User user : users) {
            userMap.put(user.getId(), user);
        }

        List<Category> categories = categoryIds.isEmpty() ? Collections.emptyList() : categoryMapper.selectByIds(categoryIds);
        Map<Long, Category> categoryMap = new HashMap<>();
        for (Category category : categories) {
            categoryMap.put(category.getId(), category);
        }

        List<Long> questionIds = new ArrayList<>();
        for (Question q : orderedQuestions) {
            questionIds.add(q.getId());
        }
        List<QuestionTag> questionTags = questionIds.isEmpty()
                ? Collections.emptyList()
                : questionTagMapper.selectList(new QueryWrapper<QuestionTag>().in("question_id", questionIds));

        List<Long> tagIds = new ArrayList<>();
        for (QuestionTag qt : questionTags) {
            if (!tagIds.contains(qt.getTagId())) {
                tagIds.add(qt.getTagId());
            }
        }
        List<Tag> tagList = tagIds.isEmpty() ? Collections.emptyList() : tagMapper.selectByIds(tagIds);
        Map<Long, Tag> tagMap = new HashMap<>();
        for (Tag tag : tagList) {
            tagMap.put(tag.getId(), tag);
        }

        Map<Long, List<String>> tagNameMap = new HashMap<>();
        for (QuestionTag qt : questionTags) {
            Tag tag = tagMap.get(qt.getTagId());
            if (tag != null) {
                List<String> nameList = tagNameMap.get(qt.getQuestionId());
                if (nameList == null) {
                    nameList = new ArrayList<>();
                    tagNameMap.put(qt.getQuestionId(), nameList);
                }
                nameList.add(tag.getName());
            }
        }

        List<QuestionListVO> voList = new ArrayList<>();
        for (Question q : orderedQuestions) {
            QuestionListVO vo = new QuestionListVO();
            vo.setId(q.getId());
            vo.setTitle(q.getTitle());
            User user = userMap.get(q.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
            Category category = categoryMap.get(q.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
            List<String> tagNames = tagNameMap.get(q.getId());
            vo.setTags(tagNames != null ? tagNames : new ArrayList<>());
            vo.setViewCount(q.getViewCount());
            vo.setLikeCount(q.getLikeCount());
            vo.setAnswerCount(q.getAnswerCount());
            vo.setCreateTime(q.getCreateTime());
            voList.add(vo);
        }

        PageResultVO<QuestionListVO> result = new PageResultVO<>();
        result.setRecords(voList);
        result.setTotal(total);
        result.setPage(pageNum);
        result.setSize(pageSize);
        return result;
    }

    /** 回答 → MyAnswerVO 组装（问题标题/回答者信息从批量查询的 Map 里取） */
    private MyAnswerVO buildMyAnswerVO(Answer a, Map<Long, Question> questionMap, Map<Long, User> userMap) {
        MyAnswerVO vo = new MyAnswerVO();
        vo.setId(a.getId());
        vo.setQuestionId(a.getQuestionId());
        Question question = questionMap.get(a.getQuestionId());
        if (question != null) {
            vo.setQuestionTitle(question.getTitle());
        }
        vo.setContent(a.getContent());
        vo.setLikeCount(a.getLikeCount());
        vo.setIsAccepted(a.getIsAccepted());
        vo.setUserId(a.getUserId());
        User user = userMap.get(a.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }
        vo.setCreateTime(a.getCreateTime());
        return vo;
    }
}
