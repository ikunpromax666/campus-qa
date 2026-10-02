package com.campus.campusqaweb.controller.user;

import com.campus.campusqacommon.result.Result;
import com.campus.campusqapojo.vo.AdminCategoryVO;
import com.campus.campusqaservice.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ClassName: CategoryController
 * Description: 前台 - 分类列表（供首页筛选、发布问题下拉使用）
 *
 * 面试高频问题：
 * Q: 前台 CategoryController 和管理端 AdminCategoryController 都调 CategoryService.list()，
 *    为什么 Service 只有一份？
 * A: Service 按领域划分（CategoryService 就是分类领域的全部业务），
 *    Controller 按调用方划分（user/admin 各自的视图层）。
 *    同一领域服务可以被多个调用方复用，避免复制业务逻辑——
 *    如果把分类查询逻辑分别写在两个 Controller 里，改排序规则就要改两处。
 * Author: SuperXia
 * Datetime :2026/10/2 16:10
 * Version:1.0
 */
@Tag(name = "前台-分类")
@RestController
@RequestMapping("/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "分类列表")
    @GetMapping("/list")
    public Result<List<AdminCategoryVO>> list() {
        return Result.success(categoryService.list());
    }
}
