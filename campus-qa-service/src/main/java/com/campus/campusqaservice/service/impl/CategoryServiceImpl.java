package com.campus.campusqaservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.campusqacommon.exception.BusinessException;
import com.campus.campusqacommon.result.ResultCode;
import com.campus.campusqamapper.mapper.CategoryMapper;
import com.campus.campusqamapper.mapper.QuestionMapper;
import com.campus.campusqapojo.dto.CategoryCreateDTO;
import com.campus.campusqapojo.dto.CategoryUpdateDTO;
import com.campus.campusqapojo.entity.Category;
import com.campus.campusqapojo.entity.Question;
import com.campus.campusqapojo.vo.AdminCategoryVO;
import com.campus.campusqaservice.service.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: CategoryServiceImpl
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/30 17:43
 * Version:1.0
 */
@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;
    private final QuestionMapper questionMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper, QuestionMapper questionMapper) {
        this.categoryMapper = categoryMapper;
        this.questionMapper = questionMapper;
    }

    @Override
    public List<AdminCategoryVO> list() {
        List<Category> categoryList = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .orderByDesc(Category::getSortOrder)
                        .orderByDesc(Category::getCreateTime)
        );
        List<AdminCategoryVO> adminCategoryVOList = new ArrayList<>();
        for (Category category : categoryList) {
            AdminCategoryVO adminCategoryVO = new AdminCategoryVO();
            BeanUtils.copyProperties(category, adminCategoryVO);
            adminCategoryVOList.add(adminCategoryVO);
        }
        return adminCategoryVOList;
    }

    @Override
    public void save(CategoryCreateDTO dto) {

        long count = categoryMapper.selectCount(
            new LambdaQueryWrapper <Category>().eq(Category::getName, dto.getName())
        );
        if (count > 0 ) throw new BusinessException (ResultCode.CATEGORY_NAME_EXISTS);

        Category category = new Category();
        BeanUtils.copyProperties(dto, category);
        // 默认值,与实体注释"越大越靠前"语义对齐
        if (category.getSortOrder() == null ) {
            category.setSortOrder( 0 );
        }

        categoryMapper.insert(category);

    }

    @Transactional
    @Override
    public void update(Long id, CategoryUpdateDTO dto) {

        Category category = categoryMapper.selectById(id);
        if(category == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }

        if (dto.getName() != null && !dto.getName().equals(category.getName())) {
            long count = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                    // 关键:排除自己,否则把自己算重名
                    .eq(Category::getName, dto.getName())
                    .ne(Category::getId, id));
            if (count > 0) throw new BusinessException(ResultCode.CATEGORY_NAME_EXISTS);
        }

        Category updateCategory = new Category();
        updateCategory.setId(id);
        updateCategory.setName(dto.getName());
        if (dto.getSortOrder() != null) {
            updateCategory.setSortOrder(dto.getSortOrder());
        }
        if (dto.getDescription() != null) {
            updateCategory.setDescription(dto.getDescription());
        }
        categoryMapper.updateById(updateCategory);

    }

    @Transactional
    @Override
    public void delete(Long id) {
        Category category = categoryMapper.selectById(id);
        if(category == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getCategoryId, id)
                .eq(Question::getStatus, 0)
        );
        if(!questions.isEmpty()) {
            throw new BusinessException(ResultCode.CATEGORY_IN_USE);
        }
        categoryMapper.deleteById(id);
    }
}
