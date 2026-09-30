package com.campus.campusqaweb.controller.admin;

import com.campus.campusqacommon.annotation.RequireAdmin;
import com.campus.campusqacommon.result.Result;
import com.campus.campusqapojo.dto.TagCreateDTO;
import com.campus.campusqapojo.vo.AdminTagVO;
import com.campus.campusqaservice.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ClassName: AdminTagController
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/30 18:43
 * Version:1.0
 */
@Tag(name = "管理后台-标签管理")
@RestController
@RequestMapping("/admin/tag")
@RequireAdmin
public class AdminTagController {

    private final TagService tagService;
    // 构造器注入
    public AdminTagController(TagService tagService) {
        this.tagService = tagService;
    }

    @Operation(summary = "标签列表")
    @GetMapping("/list")
    public Result<List<AdminTagVO>> list() {
        return Result.success(tagService.list());
    }

    @Operation(summary = "新增标签")
    @PostMapping
    public Result<Void> save(@RequestBody @Valid TagCreateDTO dto) {
        tagService.save(dto);
        return Result.success();
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.success();
    }
}