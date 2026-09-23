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
import com.campus.campusqamapper.mapper.QuestionMapper;
import com.campus.campusqamapper.mapper.UserMapper;
import com.campus.campusqapojo.dto.AnswerPublishDTO;
import com.campus.campusqapojo.dto.AnswerQueryDTO;
import com.campus.campusqapojo.dto.AnswerUpdateDTO;
import com.campus.campusqapojo.entity.Answer;
import com.campus.campusqapojo.entity.Question;
import com.campus.campusqapojo.entity.User;
import com.campus.campusqapojo.vo.AnswerVO;
import com.campus.campusqapojo.vo.PageResultVO;
import com.campus.campusqaservice.service.AnswerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnswerServiceImpl implements AnswerService {
    private final UserMapper userMapper;
    private final QuestionMapper questionMapper;
    private final AnswerMapper answerMapper;

    public AnswerServiceImpl(UserMapper userMapper, QuestionMapper questionMapper, AnswerMapper answerMapper) {
        this.userMapper = userMapper;
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
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

            //TODO 互动模块完成后回填真实值
            vo.setLikeStatus(0);

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


}
