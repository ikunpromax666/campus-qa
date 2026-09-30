package com.campus.campusqaweb.controller.admin;

import com.campus.campusqacommon.annotation.RequireAdmin;
import com.campus.campusqacommon.result.Result;
import com.campus.campusqapojo.dto.CategoryCreateDTO;
import com.campus.campusqapojo.dto.CategoryUpdateDTO;
import com.campus.campusqapojo.vo.AdminCategoryVO;
import com.campus.campusqaservice.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ClassName: AdminCategoryController
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/30 18:42
 * Version:1.0
 */
@Tag(name = "管理后台-分类管理")
@RestController
@RequestMapping("/admin/category")
@RequireAdmin   // 类上!管理端默认拒绝,不用每个方法标
public class AdminCategoryController {

    private final CategoryService categoryService;
    // 构造器注入

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "分类列表")
    @GetMapping("/list")
    public Result<List<AdminCategoryVO>> list() {
        return Result.success(categoryService.list());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Void> save(@RequestBody @Valid CategoryCreateDTO dto) {
        categoryService.save(dto);
        return Result.success();
    }

    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody @Valid CategoryUpdateDTO dto) {
        categoryService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
