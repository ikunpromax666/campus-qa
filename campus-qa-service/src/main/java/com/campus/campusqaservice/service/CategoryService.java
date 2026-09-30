package com.campus.campusqaservice.service;

import com.campus.campusqapojo.dto.CategoryCreateDTO;
import com.campus.campusqapojo.dto.CategoryUpdateDTO;
import com.campus.campusqapojo.vo.AdminCategoryVO;

import java.util.List;

/**
 * ClassName: CategoryService
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/30 17:41
 * Version:1.0
 */
public interface CategoryService {
    List<AdminCategoryVO> list () ;
    void save (CategoryCreateDTO dto) ;
    void update (Long id, CategoryUpdateDTO dto) ;
    void delete (Long id) ;
}
