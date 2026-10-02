package com.campus.campusqaservice.service.impl;

/**
 * ClassName: AnswerServiceImpl
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/22 08:52
 * Version:1.0
 */
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.campusqacommon.context.UserContext;
import com.campus.campusqacommon.exception.BusinessException;
import com.campus.campusqacommon.result.ResultCode;
import com.campus.campusqamapper.mapper.AnswerMapper;
import com.campus.campusqamapper.mapper.LikeRecordMapper;
import com.campus.campusqamapper.mapper.QuestionMapper;
import com.campus.campusqamapper.mapper.UserMapper;
import com.campus.campusqapojo.dto.AnswerPublishDTO;
import com.campus.campusqapojo.dto.AnswerQueryDTO;
import com.campus.campusqapojo.dto.AnswerUpdateDTO;
import com.campus.campusqapojo.entity.Answer;
import com.campus.campusqapojo.entity.LikeRecord;
import com.campus.campusqapojo.entity.Question;
import com.campus.campusqapojo.entity.User;
import com.campus.campusqapojo.vo.AnswerVO;
import com.campus.campusqapojo.vo.MyAnswerVO;
import com.campus.campusqapojo.vo.PageResultVO;
import com.campus.campusqaservice.service.AnswerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnswerServiceImpl implements AnswerService {

    /** 互动目标类型：2回答 */
    private static final Integer TARGET_ANSWER = 2;

    /** 点赞状态：0 无 / 1 已赞 / 2 已踩（与 like_record.action_type 语义一致） */
    private static final Integer STATUS_NONE = 0;

    /** 分页上限：防止 ?size=1000000 一页打爆数据库（面试考点：分页必须夹 size） */
    private static final int MAX_PAGE_SIZE = 100;

    private final UserMapper userMapper;
    private final QuestionMapper questionMapper;
    private final AnswerMapper answerMapper;
    private final LikeRecordMapper likeRecordMapper;

    public AnswerServiceImpl(UserMapper userMapper,
                             QuestionMapper questionMapper,
                             AnswerMapper answerMapper,
                             LikeRecordMapper likeRecordMapper) {
        this.userMapper = userMapper;
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
        this.likeRecordMapper = likeRecordMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void publishAnswer(AnswerPublishDTO dto) {
        //验证用户登录状态
        Long userId = UserContext.requireUserId();

        //验证问题是否存在和问题状态是否正常
        Question question = questionMapper.selectById(dto.getQuestionId());

        // 不存在或已删除
        if (question == null || Integer.valueOf( 2 ).equals(question.getStatus())) {
            throw new BusinessException (ResultCode.QUESTION_NOT_FOUND);
        }

        // 已关闭
        if (Integer.valueOf( 1 ).equals(question.getStatus())) {
            throw new BusinessException (ResultCode.QUESTION_CLOSED);
        }

        Answer answer = new Answer();

        answer.setContent(dto.getContent());
        answer.setQuestionId(dto.getQuestionId());
        answer.setUserId(userId);
        answer.setLikeCount(0);
        answer.setIsAccepted(0);
        answer.setStatus(0);

        // 问题回答数原子 +1
        UpdateWrapper<Question> updateWrapper = new UpdateWrapper <>();
        updateWrapper.eq( "id" , dto.getQuestionId())
                .setSql( "answer_count = answer_count + 1" );
        questionMapper.update( null , updateWrapper);
        answerMapper.insert(answer);
    }

    @Override
    public PageResultVO<AnswerVO> listAnswers(AnswerQueryDTO dto) {
        // 验证问题是否存在
        Question question = questionMapper.selectById(dto.getQuestionId());
        if (question == null || Integer.valueOf( 2 ).equals(question.getStatus())) {
            throw new BusinessException(ResultCode.QUESTION_NOT_FOUND);
        }

        // 分页查询
        Page <Answer> page = new Page<>(dto.getPage(), dto.getSize());
        //获取该问题的所有回答
        QueryWrapper<Answer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("question_id", dto.getQuestionId());
        // 状态正常
        queryWrapper.eq("status", 0);
        //已采纳回答放在最前面
        queryWrapper.orderByDesc("is_accepted");
        // 再按点赞数
        queryWrapper.orderByDesc( "like_count" );
        // 最后按创建时间降序排序
        queryWrapper.orderByDesc("create_time");
        IPage<Answer> answerPage = answerMapper.selectPage(page, queryWrapper);
        List<Answer> answers = answerPage.getRecords();

        //批量查询用户信息(避免N+1查询)
        List<Long> userIds = new ArrayList<>();
        for (Answer answer : answers) {
            if(!userIds.contains(answer.getUserId())) {
                userIds.add(answer.getUserId());
            }
        }

        Map<Long, User> userMap = new HashMap<>();
        if(!userIds.isEmpty()) {
           List<User> users = userMapper.selectByIds(userIds);
           for (User user : users) {
               userMap.put(user.getId(), user);
           }
        }

        //组装vo
        Long currentUserId = UserContext.getUserId();

        // 批量回填当前用户的点赞状态：一次查完本页所有回答的点赞记录 → Map，避免循环里逐条查（N+1）
        // 游客（currentUserId 为 null）不查库，全部走默认值
        Map<Long, Integer> likeStatusMap = new HashMap<>();
        if (currentUserId != null && !answers.isEmpty()) {
            List<Long> answerIds = new ArrayList<>();
            for (Answer answer : answers) {
                answerIds.add(answer.getId());
            }
            List<LikeRecord> likeRecords = likeRecordMapper.selectList(new QueryWrapper<LikeRecord>()
                    .eq("user_id", currentUserId)
                    .eq("target_type", TARGET_ANSWER)
                    .in("target_id", answerIds));
            for (LikeRecord record : likeRecords) {
                likeStatusMap.put(record.getTargetId(), record.getActionType());
            }
        }

        List<AnswerVO> voList = new ArrayList<>();
        for (Answer answer : answers) {
            AnswerVO vo = new AnswerVO();
            vo.setId(answer.getId());
            vo.setQuestionId(answer.getQuestionId());
            vo.setContent(answer.getContent());
            vo.setUserId(answer.getUserId());
            vo.setLikeCount(answer.getLikeCount());
            vo.setIsAccepted(answer.getIsAccepted());
            vo.setCreateTime(answer.getCreateTime());

            User anthor = userMap.get(answer.getUserId());
            if (anthor != null) {
                vo.setNickname(anthor.getNickname());
                vo.setAvatar(anthor.getAvatar());
            }

            vo.setIsOwner(currentUserId != null && currentUserId.equals(answer.getUserId()));

            // 点赞状态：Map 里没有 = 没点过 → 默认 0（无 / 1已赞 / 2已踩）
            vo.setLikeStatus(likeStatusMap.getOrDefault(answer.getId(), STATUS_NONE));

            voList.add(vo);
        }
        // 5. 组装分页结果
        PageResultVO <AnswerVO> result = new PageResultVO <>();
        result.setRecords(voList);
        result.setTotal(answerPage.getTotal());
        result.setPage(dto.getPage());
        result.setSize(dto.getSize());
        return result;
    }

    @Override
    public void updateAnswer(Long id, AnswerUpdateDTO dto) {
        Long currentUserId = UserContext.requireUserId();

        // 验证回答是否存在
        Answer answer = answerMapper.selectById(id);
        if (answer == null || Integer.valueOf( 1 ).equals(answer.getStatus())) {
            throw new BusinessException(ResultCode.ANSWER_NOT_FOUND);
        }

        // 验证回答是否是当前用户
        if (!currentUserId.equals(answer.getUserId())) {
            throw new BusinessException(ResultCode.ANSWER_NOT_OWNER);
        }

        // 更新回答
       Answer updateAnswer = new Answer();
       updateAnswer.setId(id);
       updateAnswer.setContent(dto.getContent());
       answerMapper.updateById(updateAnswer);
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteAnswer(Long id) {
        // 验证回答是否存在
        Answer answer = answerMapper.selectById(id);
        if (answer == null || Integer.valueOf( 1 ).equals(answer.getStatus())) {
            throw new BusinessException(ResultCode.ANSWER_NOT_FOUND);
        }



        //验证当前用户是否是回答者或管理员
        boolean isOwner = UserContext.requireUserId().equals(answer.getUserId());
        boolean isAdmin = Integer.valueOf( 1 ).equals(UserContext.get().getRole());
        if (!isOwner && !isAdmin) {
            throw new BusinessException (ResultCode.ANSWER_NOT_YOURS);
        }

        // 删除回答(软删除)
        Answer updateAnswer = new Answer();
        updateAnswer.setId(id);
        updateAnswer.setStatus(1);

        UpdateWrapper <Question> wrapper = new UpdateWrapper <>();
        wrapper.eq( "id" , answer.getQuestionId())
                .gt( "answer_count" , 0 )
                .setSql( "answer_count = answer_count - 1" );
        if (Integer.valueOf( 1 ).equals(answer.getIsAccepted())) {
            wrapper.set( "is_adopted" , 0 );
        }
        questionMapper.update( null , wrapper);
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void acceptBestAnswer(Long answerId, Long questionId) {
        Long currentUserId = UserContext.requireUserId();

        // 1. 问题必须存在
        Question question = questionMapper.selectById(questionId);
        if (question == null || Integer.valueOf( 2 ).equals(question.getStatus())) {
            throw new BusinessException (ResultCode.QUESTION_NOT_FOUND);
        }

        // 2.必须由问题发布者采纳最佳答案
        if (!currentUserId.equals(question.getUserId())) {
            throw new BusinessException (ResultCode.QUESTION_NOT_YOURS);
        }

        //验证该问题是否已有最佳答案
        if (Integer.valueOf( 1 ).equals(question.getIsAdopted())) {
            throw new BusinessException (ResultCode.ANSWER_ALREADY_ACCEPTED);
        }

        // 4. 回答必须存在、未删除、且属于该问题
        Answer answer = answerMapper.selectById(answerId);
        if (answer == null || Integer.valueOf( 1 ).equals(answer.getStatus())
            || !questionId.equals(answer.getQuestionId())) {
            throw new BusinessException (ResultCode.ANSWER_NOT_FOUND);
        }

        //采纳该回答
        Answer updateAnswer = new Answer();
        updateAnswer.setId(answerId);
        updateAnswer.setIsAccepted(1);
        answerMapper.updateById(updateAnswer);

        // 6. 标记问题已采纳（带条件更新，防止并发重复采纳）
        UpdateWrapper <Question> wrapper = new UpdateWrapper <>();
        wrapper.eq( "id" , questionId)
                .eq( "is_adopted" , 0 )// ← 只有"未采纳"才允许更新（CAS 语义）
                .set( "is_adopted" , 1 );
        int rows = questionMapper.update( null , wrapper);
        if (rows == 0 ) {
            // 并发场景：另一个请求已经抢先采纳了
            throw new BusinessException (ResultCode.ANSWER_ALREADY_ACCEPTED);
        }

    }

    @Override
    public PageResultVO<MyAnswerVO> myAnswers(Integer page, Integer size) {
        Long userId = UserContext.requireUserId();
        // 分页夹取：size 上限 MAX_PAGE_SIZE，防止一页打爆数据库
        int pageNum = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, MAX_PAGE_SIZE);

        // 只查自己的、未删除的回答；创建时间倒序 + id 兜底保证分页稳定
        IPage<Answer> answerPage = answerMapper.selectPage(new Page<>(pageNum, pageSize),
                new QueryWrapper<Answer>()
                        .eq("user_id", userId)
                        .eq("status", 0)
                        .orderByDesc("create_time")
                        .orderByDesc("id"));

        List<Answer> answers = answerPage.getRecords();

        // 批量查所属问题拿标题（防 N+1：绝不每条回答查一次问题）
        List<Long> questionIds = new ArrayList<>();
        for (Answer a : answers) {
            if (!questionIds.contains(a.getQuestionId())) {
                questionIds.add(a.getQuestionId());
            }
        }
        List<Question> questions = questionIds.isEmpty() ? Collections.emptyList() : questionMapper.selectByIds(questionIds);
        Map<Long, Question> questionMap = new HashMap<>();
        for (Question q : questions) {
            questionMap.put(q.getId(), q);
        }

        // "我的回答"里所有回答都是自己的 → 用户信息一次查询即可
        User me = userMapper.selectById(userId);

        List<MyAnswerVO> voList = new ArrayList<>();
        for (Answer a : answers) {
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
            vo.setUserId(userId);
            if (me != null) {
                vo.setNickname(me.getNickname());
                vo.setAvatar(me.getAvatar());
            }
            vo.setCreateTime(a.getCreateTime());
            voList.add(vo);
        }

        PageResultVO<MyAnswerVO> result = new PageResultVO<>();
        result.setRecords(voList);
        result.setTotal(answerPage.getTotal());
        result.setPage(pageNum);
        result.setSize(pageSize);
        return result;
    }

}
