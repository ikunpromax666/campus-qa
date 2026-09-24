package com.campus.campusqaservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.campus.campusqacommon.context.UserContext;
import com.campus.campusqacommon.exception.BusinessException;
import com.campus.campusqacommon.result.ResultCode;
import com.campus.campusqamapper.mapper.AnswerMapper;
import com.campus.campusqamapper.mapper.FavoriteMapper;
import com.campus.campusqamapper.mapper.LikeRecordMapper;
import com.campus.campusqamapper.mapper.QuestionMapper;
import com.campus.campusqapojo.dto.FavoriteToggleDTO;
import com.campus.campusqapojo.dto.LikeToggleDTO;
import com.campus.campusqapojo.entity.Answer;
import com.campus.campusqapojo.entity.Favorite;
import com.campus.campusqapojo.entity.LikeRecord;
import com.campus.campusqapojo.entity.Question;
import com.campus.campusqapojo.vo.ToggleResultVO;
import com.campus.campusqaservice.service.InteractionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final QuestionMapper questionMapper;
    private final AnswerMapper answerMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final FavoriteMapper favoriteMapper;

    public InteractionServiceImpl(QuestionMapper questionMapper,
                                  AnswerMapper answerMapper,
                                  LikeRecordMapper likeRecordMapper,
                                  FavoriteMapper favoriteMapper) {
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.favoriteMapper = favoriteMapper;
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
}
