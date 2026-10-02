package com.campus.campusqaservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.campusqacommon.context.UserContext;
import com.campus.campusqacommon.exception.BusinessException;
import com.campus.campusqacommon.result.ResultCode;
import com.campus.campusqacommon.utils.JwtUtils;
import com.campus.campusqamapper.mapper.*;
import com.campus.campusqapojo.dto.QuestionPublishDTO;
import com.campus.campusqapojo.dto.QuestionQueryDTO;
import com.campus.campusqapojo.dto.QuestionUpdateDTO;
import com.campus.campusqapojo.entity.*;
import com.campus.campusqapojo.vo.*;
import com.campus.campusqaservice.service.QuestionService;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ClassName: QuestionServiceImpl
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/20 17:11
 * Version:1.0
 */
@Service
public class QuestionServiceImpl implements QuestionService {

     /** 互动目标类型：1问题 */
    private static final Integer TARGET_QUESTION = 1;

    /** 点赞状态：0 无 / 1 已赞 / 2 已踩（与 like_record.action_type 语义一致） */
    private static final Integer STATUS_NONE = 0;

    private final QuestionMapper questionMapper;

    private final CategoryMapper categoryMapper;

    private final TagMapper tagMapper;

    private final QuestionTagMapper questionTagMapper;

    private final UserMapper userMapper;

    private final LikeRecordMapper likeRecordMapper;

    private final FavoriteMapper favoriteMapper;

    public QuestionServiceImpl(QuestionMapper questionMapper,
                               CategoryMapper categoryMapper,
                               TagMapper tagMapper,
                               QuestionTagMapper questionTagMapper,
                               UserMapper userMapper,
                               LikeRecordMapper likeRecordMapper,
                               FavoriteMapper favoriteMapper) {
        this.questionMapper = questionMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.questionTagMapper = questionTagMapper;
        this.userMapper = userMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.favoriteMapper = favoriteMapper;
    }

    @Override
    public Long publishQuestion(QuestionPublishDTO dto) {
        //验证用户登陆状态
        Long userId = UserContext.requireUserId();
        //校验 categoryId是否存在
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
        //校验 tags是否存在（tagIds 可选；MP 的 selectByIds 空集合会拼出 IN () 非法 SQL，必须判空）
        List<Tag> tags = Collections.emptyList();
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            tags = tagMapper.selectByIds(dto.getTagIds());
        }
        for (Tag tag : tags) {
            if (tag == null) {
                throw new BusinessException(ResultCode.TAG_NOT_FOUND);
            }
        }
        //创建问题
        Question question = new Question();
        question.setTitle(dto.getTitle());
        question.setContent(dto.getContent());
        question.setCategoryId(dto.getCategoryId());
        question.setUserId(userId);
        question.setCreateTime(LocalDateTime.now());
        questionMapper.insert(question);

        //创建问题标签关联
        Long questionId = question.getId();
        for (Long tagId : dto.getTagIds()) {
            QuestionTag qt = new QuestionTag();
            qt.setQuestionId(questionId);
            qt.setTagId(tagId);
            questionTagMapper.insert(qt);
        }

        return questionId;
    }

    @Override
    public PageResultVO<QuestionListVO> list(QuestionQueryDTO dto) {
        //构建分页查询
        Page<Question> page = new Page<>(dto.getPage(), dto.getSize());

        //构建查询条件
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        //只查询正常状态的问题
        queryWrapper.eq("status", 0);
        //分类筛选
        if (dto.getCategoryId() != null) {
            queryWrapper.eq("category_id", dto.getCategoryId());
        }
        //关键词模糊查询
        if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
            queryWrapper.like("title", dto.getKeyword());
        }
        //排序：hot 按浏览量，默认按创建时间倒序
        if("hot".equals(dto.getSort())){
            queryWrapper.orderByDesc("view_count");
        }else{
            queryWrapper.orderByDesc("create_time");
        }

        //执行查询
        IPage<Question> questionPage = questionMapper.selectPage(page, queryWrapper);

        // 拿到当前页的问题列表
        List<Question> questions = questionPage.getRecords();

        //提取需要查询的ID
        List<Long> userIds = questions.stream().map(Question::getUserId).distinct().toList();
        List<Long> categoryIds = questions.stream().map(Question::getCategoryId).distinct().toList();
        List<Long> questionIds = questions.stream().map(Question::getId).distinct().toList();

        // 批量查用户 → Map<userId, User>（空集合判空：selectByIds 空集合会拼 IN () 报错）
        List <User> users = userIds.isEmpty() ? Collections.emptyList() : userMapper.selectByIds(userIds);
        Map<Long,User> userMap = new HashMap<>();
        for (User user : users) {
            userMap.put(user.getId(), user);
        }

        // 批量查分类 → Map<categoryId, Category>（同上判空）
        List<Category> categories = categoryIds.isEmpty() ? Collections.emptyList() : categoryMapper.selectByIds(categoryIds);
        Map<Long,Category> categoryMap = new HashMap<>();
        for (Category category : categories) {
            categoryMap.put(category.getId(), category);
        }

        // 批量查问题-标签关联（in 空集合同样会拼 IN () 报错，判空）
        List <QuestionTag> questionTags = questionIds.isEmpty()
                ? Collections.emptyList()
                : questionTagMapper.selectList(
                        new QueryWrapper <QuestionTag>().in( "question_id" , questionIds)
                );

        //  收集所有 tagId（去重）
        List <Long> tagIds = new ArrayList<>();
        for (QuestionTag qt : questionTags) {
            if (!tagIds.contains(qt.getTagId())) {
                tagIds.add(qt.getTagId());
            }
        }

        // 批量查标签 → Map<tagId, Tag>（同上判空）
        List <Tag> tagList = tagIds.isEmpty() ? Collections.emptyList() : tagMapper.selectByIds(tagIds);
        Map <Long, Tag> tagMap = new HashMap<>();
        for (Tag tag : tagList) {
            tagMap.put(tag.getId(), tag);
        }

        // 4.5 按 questionId 分组标签名 → Map<questionId, List<tagName>>
        Map <Long, List<String>> tagNameMap = new HashMap <>();
        for (QuestionTag qt : questionTags) {
            Tag tag = tagMap.get(qt.getTagId());
            if (tag != null ) { // 如果 Map 里还没有这个 questionId 的列表，先创建
                List <String> nameList = tagNameMap.get(qt.getQuestionId());
                if (nameList == null ) {
                    nameList = new ArrayList <>();
                    tagNameMap.put(qt.getQuestionId(), nameList);
                }
                nameList.add(tag.getName());
            }
        }

        // 5. 组装 VO 列表
        List <QuestionListVO> voList = new ArrayList <>();
        for (Question q : questions) {
            QuestionListVO vo = new QuestionListVO ();
            vo.setId(q.getId());
            vo.setTitle(q.getTitle());

            // 设置用户信息
            User user = userMap.get(q.getUserId());
            if (user != null ) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
            // 设置分类名
            Category category = categoryMap.get(q.getCategoryId());
            if (category != null ) {
                vo.setCategoryName(category.getName());
            }
            // 设置标签列表（没标签的给空列表）
            List <String> tagNames = tagNameMap.get(q.getId());
            if (tagNames != null ) {
                vo.setTags(tagNames);
            }
            else {
                vo.setTags( new ArrayList <>());
            }

            vo.setViewCount(q.getViewCount());
            vo.setLikeCount(q.getLikeCount());
            vo.setAnswerCount(q.getAnswerCount());
            vo.setCreateTime(q.getCreateTime());

            voList.add(vo);
        }

        // 6. 组装分页结果返回
        PageResultVO <QuestionListVO> result = new PageResultVO <>();
        result.setRecords(voList);
        result.setTotal(questionPage.getTotal());
        result.setPage(dto.getPage());
        result.setSize(dto.getSize());
        return result;
    }

    /** 分页硬上限：防止 ?size=1000000 一条请求打穿服务（列表接口通用红线） */
    private static final int MAX_PAGE_SIZE = 100;

    /** 问题状态：0 正常 / 1 已关闭 / 2 已删除 */
    private static final Integer QUESTION_STATUS_DELETED = 2;

    @Override
    public PageResultVO<MyQuestionVO> myQuestions(Integer page, Integer size) {
        Long userId = UserContext.requireUserId();
        int pageNum = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, MAX_PAGE_SIZE);

        // 作者视角：查自己的提问，含已关闭（status=1），不含已删除（status=2）
        // 面试考点：逻辑删除的可见性按视角区分 —— 他人/广场视角看不到已删除，
        // 作者自己的"我的提问"列表里已关闭的仍可见（带状态标记），已删除的同样不展示
        IPage<Question> questionPage = questionMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new QueryWrapper<Question>()
                        .eq("user_id", userId)
                        .ne("status", QUESTION_STATUS_DELETED)
                        .orderByDesc("create_time")
                        .orderByDesc("id"));

        List<Question> questions = questionPage.getRecords();

        // 批查分类（空集合判空：selectByIds 拼 IN () 非法 SQL）
        List<Long> categoryIds = new ArrayList<>();
        for (Question q : questions) {
            if (q.getCategoryId() != null && !categoryIds.contains(q.getCategoryId())) {
                categoryIds.add(q.getCategoryId());
            }
        }
        List<Category> categories = categoryIds.isEmpty()
                ? Collections.emptyList() : categoryMapper.selectByIds(categoryIds);
        Map<Long, Category> categoryMap = new HashMap<>();
        for (Category c : categories) {
            categoryMap.put(c.getId(), c);
        }

        // 批查问题-标签关联
        List<Long> questionIds = new ArrayList<>();
        for (Question q : questions) {
            questionIds.add(q.getId());
        }
        List<QuestionTag> questionTags = questionIds.isEmpty()
                ? Collections.emptyList()
                : questionTagMapper.selectList(
                        new QueryWrapper<QuestionTag>().in("question_id", questionIds));

        // 批查标签
        List<Long> tagIds = new ArrayList<>();
        for (QuestionTag qt : questionTags) {
            if (!tagIds.contains(qt.getTagId())) {
                tagIds.add(qt.getTagId());
            }
        }
        List<Tag> tagList = tagIds.isEmpty()
                ? Collections.emptyList() : tagMapper.selectByIds(tagIds);
        Map<Long, Tag> tagMap = new HashMap<>();
        for (Tag t : tagList) {
            tagMap.put(t.getId(), t);
        }

        // 按 questionId 分组标签名
        Map<Long, List<String>> tagNameMap = new HashMap<>();
        for (QuestionTag qt : questionTags) {
            Tag tag = tagMap.get(qt.getTagId());
            if (tag != null) {
                List<String> names = tagNameMap.get(qt.getQuestionId());
                if (names == null) {
                    names = new ArrayList<>();
                    tagNameMap.put(qt.getQuestionId(), names);
                }
                names.add(tag.getName());
            }
        }

        // 组装 VO（作者视角：不查用户表，昵称头像都是自己的）
        List<MyQuestionVO> voList = new ArrayList<>();
        for (Question q : questions) {
            MyQuestionVO vo = new MyQuestionVO();
            vo.setId(q.getId());
            vo.setTitle(q.getTitle());
            vo.setStatus(q.getStatus());
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

        PageResultVO<MyQuestionVO> result = new PageResultVO<>();
        result.setRecords(voList);
        result.setTotal(questionPage.getTotal());
        result.setPage(pageNum);
        result.setSize(pageSize);
        return result;
    }

    @Override
    public QuestionDetailVO detail(Long id) {
        //根据id查询问题详情
        Question question = questionMapper.selectById(id);
        // 已关闭(status=1)的问题仍可查看详情（只是禁答），仅已删除(status=2)不可见
        if(question == null || question.getStatus() == 2){
            throw new BusinessException(ResultCode.QUESTION_NOT_FOUND);
        }

        //浏览量+1
        Question updateQuestion = new Question ();
        updateQuestion.setId(id);
        updateQuestion.setViewCount(question.getViewCount() + 1);
        questionMapper.updateById(updateQuestion);

        QuestionDetailVO vo = new QuestionDetailVO();

        //  查用户信息
        User user = userMapper.selectById(question.getUserId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());


        //查询分类和标签
        Category category = categoryMapper.selectById(question.getCategoryId());

        if (category != null ) {
            CategoryVO categoryVO = new CategoryVO ();
            categoryVO.setId(category.getId());
            categoryVO.setName(category.getName());
            vo.setCategory(categoryVO);
        }

        // 第一步：查 question_tag 表，拿到这个问题的所有关联记录
        List<QuestionTag> questionTags = questionTagMapper.selectList(
                new QueryWrapper<QuestionTag>().eq("question_id", question.getId())
        );

        //提取TagId列表
        List<Long> tagIds = new ArrayList<>();
         for (QuestionTag qt : questionTags) {
             tagIds.add(qt.getTagId());
        }

        // 第三步：批量查 tag 表，拿到标签详情（判空防 IN () 非法 SQL）
        List <Tag> tagList = tagIds.isEmpty() ? Collections.emptyList() : tagMapper.selectByIds(tagIds);

         // 组装标签列表
        List<TagVO> tagVOList = new ArrayList<>();
        for (Tag tag : tagList) {
            TagVO tagVO = new TagVO();
            tagVO.setId(tag.getId());
            tagVO.setName(tag.getName());
            tagVOList.add(tagVO);
        }
        vo.setTags(tagVOList);

        //当前用户相关字段（游客给默认值）
        Long currentUserId = UserContext.getUserId();
        vo.setIsOwner( currentUserId !=null && currentUserId.equals(question.getUserId()));

        // likeStatus: 0无 / 1已赞 / 2已踩；isFavorited: 是否已收藏
        // 游客（未登录）直接给默认值，不查库 —— currentUserId 为 null 时拼出来的 SQL 也没意义
        Integer likeStatus = STATUS_NONE;
        boolean isFavorited = false;
        if (currentUserId != null) {
            // 唯一索引保证"一人一目标最多一条" → 用 selectOne
            LikeRecord likeRecord = likeRecordMapper.selectOne(new QueryWrapper<LikeRecord>()
                    .eq("user_id", currentUserId)
                    .eq("target_id", question.getId())
                    .eq("target_type", TARGET_QUESTION));
            if (likeRecord != null) {
                // action_type 取值（1赞/2踩）和 VO 的 likeStatus 语义天然对齐，直接复用
                likeStatus = likeRecord.getActionType();
            }

            Favorite favorite = favoriteMapper.selectOne(new QueryWrapper<Favorite>()
                    .eq("user_id", currentUserId)
                    .eq("question_id", question.getId()));
            isFavorited = favorite != null;
        }
        vo.setLikeStatus(likeStatus);
        vo.setIsFavorited(isFavorited);

        vo.setId(question.getId());
        vo.setTitle(question.getTitle());
        vo.setContent(question.getContent());
        vo.setViewCount(question.getViewCount() + 1);
        vo.setLikeCount(question.getLikeCount());
        vo.setAnswerCount(question.getAnswerCount());
        vo.setStatus(question.getStatus());
        vo.setCreateTime(question.getCreateTime());
        vo.setIsTop(question.getIsTop());
        vo.setUserId(question.getUserId());
        return vo;
    }


    @Transactional
    @Override
    public Void update(Long id, QuestionUpdateDTO dto) {
        // 1. 查问题 + 判 是否存在
        //根据id查询问题详情
        Question question = questionMapper.selectById(id);

        if (question == null || question.getStatus() != 0) {
            throw new BusinessException(ResultCode.QUESTION_NOT_FOUND);
        }
        // 2. 判 是否是作者
        Long currentUserId = UserContext.requireUserId();
        if (!currentUserId.equals(question.getUserId())) {
            throw new BusinessException(ResultCode.QUESTION_NOT_OWNER);
        }

        // 3. 选择性更新基础字段
        if (dto.getTitle() != null) {
            question.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            question.setContent(dto.getContent());
        }
        if (dto.getCategoryId() != null) {
            question.setCategoryId(dto.getCategoryId());
        }

        // 4. 更新标签关联（先删后插）
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            //先删除旧的关联记录
            questionTagMapper.delete(
                    new QueryWrapper<QuestionTag>().eq("question_id", id)
            );
            // 再按新 tagIds 逐条插入关联记录

            for (Long tagId : dto.getTagIds()) {
                QuestionTag qt = new QuestionTag();
                qt.setQuestionId(id);
                qt.setTagId(tagId);
                questionTagMapper.insert(qt);
            }
        }
        //5. 保存问题本身
        questionMapper.updateById(question);
        return null;
    }

    @Override
    public void delete(Long id) {

        Long currentUserId = UserContext.requireUserId();

        // 1. 查问题 + 判 null/状态（删除：正常/已关闭都可删，仅已删除不可重复删）
        Question question = questionMapper.selectById(id);
        if (question == null || question.getStatus() == 2) {
            throw new BusinessException(ResultCode.QUESTION_NOT_FOUND);
        }

        //权限：作者本人 或 管理员（role 从 ThreadLocal 取，不查库）
        if (!currentUserId.equals(question.getUserId()) && !Integer.valueOf(1).equals(UserContext.get().getRole())) {
            throw new BusinessException (ResultCode.QUESTION_NOT_OWNER);
        }
         //逻辑删除：status = 2
        Question updateQuestion = new Question ();
        updateQuestion.setId(id);
        updateQuestion.setStatus(2);
        questionMapper.updateById(updateQuestion);
    }

    @Override
    public void close(Long id) {
        //验证当前用户是非为作者或管理员（与 delete 的权限模型对齐）
        Question question = questionMapper.selectById(id);

        if (question == null || question.getStatus() == 2) {
            throw new BusinessException(ResultCode.QUESTION_NOT_FOUND);
        }

        Long userId = question.getUserId();

        Long currentUserId = UserContext.requireUserId();

        // 已关闭，别重复关闭
        if (question.getStatus() == 1 ) {
            throw new BusinessException (ResultCode.QUESTION_ALREADY_CLOSED);
        }
        if (!currentUserId.equals(userId) && !Integer.valueOf(1).equals(UserContext.get().getRole())) {
            throw new BusinessException(ResultCode.QUESTION_NOT_OWNER);
        }
        // 4. 只更新 status，避免覆盖其他字段
        Question update = new Question ();
        update.setId(id);
        update.setStatus( 1 );
        questionMapper.updateById(update);
    }
}
