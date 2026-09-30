package com.campus.campusqaservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.campusqacommon.exception.BusinessException;
import com.campus.campusqacommon.result.ResultCode;
import com.campus.campusqamapper.mapper.QuestionTagMapper;
import com.campus.campusqamapper.mapper.TagMapper;
import com.campus.campusqapojo.dto.TagCreateDTO;
import com.campus.campusqapojo.entity.QuestionTag;
import com.campus.campusqapojo.entity.Tag;
import com.campus.campusqapojo.vo.AdminTagVO;
import com.campus.campusqaservice.service.TagService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: TagServiceImpl
 * Description: 标签服务实现
 *
 * 面试高频问题：
 *  Q: 删标签的引用检查为什么查 question_tag 不用过滤 question.status？
 *  A: question_tag 是关联事实表，记录"某问题打过某标签"。标签被删除会让所有关联
 *    记录的 tag_id 变悬空指针，无论问题本身什么状态。所以只 count(tag_id = id)，
 *    不过滤 question.status。与删分类不同（分类是 question.category_id 直接外键，
 *    要排除已删除的问题避免误拦）。
 *
 *  Q: delete 加 @Transactional 但单表删除本身原子，事务的意义是什么？
 *  A: 事务边界不是为了单条 delete 的原子性，而是给"引用检查 + 删除"一个单元边界，
 *    缩小并发竞态窗口（检查时无引用，删时正好有人加引用）。完全防竞态要靠 DB 外键
 *    或 select for update 悲观锁，当前简化处理，管理端低频操作可接受。
 *
 *  Q: save 里为什么手动 set 而不用 BeanUtils？
 *  A: Tag 只有一个 name 需要从 DTO 取（createTime 由 MetaObjectHandler 自动填充），
 *    一行 setName 比 BeanUtils 反射更清晰，且无反射开销。多字段时才考虑 BeanUtils。
 *
 * Author: SuperXia
 * Datetime :2026/9/30 18:02
 * Version:1.0
 */
@Service
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final QuestionTagMapper questionTagMapper;

    public TagServiceImpl(TagMapper tagMapper, QuestionTagMapper questionTagMapper) {
        this.tagMapper = tagMapper;
        this.questionTagMapper = questionTagMapper;
    }

    @Override
    public List<AdminTagVO> list() {
        List<Tag> tagList = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .orderByDesc(Tag::getCreateTime)
        );
        List<AdminTagVO> voList = new ArrayList<>(tagList.size());
        for (Tag tag : tagList) {
            AdminTagVO vo = new AdminTagVO();
            BeanUtils.copyProperties(tag, vo);
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public void save(TagCreateDTO dto) {
        // 重名检查
        long count = tagMapper.selectCount(
                new LambdaQueryWrapper<Tag>().eq(Tag::getName, dto.getName())
        );
        if (count > 0) {
            throw new BusinessException(ResultCode.TAG_NAME_EXISTS);
        }

        // 标签只有 name 需要赋值，createTime 由 MetaObjectHandler 自动填充
        Tag tag = new Tag();
        tag.setName(dto.getName());
        tagMapper.insert(tag);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        // 1. 校验存在
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(ResultCode.TAG_NOT_FOUND);
        }
        // 2. 引用检查：question_tag 关联表无 status 字段，不用过滤 question 状态
        long refCount = questionTagMapper.selectCount(
                new LambdaQueryWrapper<QuestionTag>().eq(QuestionTag::getTagId, id)
        );
        if (refCount > 0) {
            throw new BusinessException(ResultCode.TAG_IN_USE);
        }
        // 3. 物理删除
        tagMapper.deleteById(id);
    }
}
