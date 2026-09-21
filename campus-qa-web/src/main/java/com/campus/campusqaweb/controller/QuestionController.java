package com.campus.campusqaweb.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.campusqacommon.annotation.RequireLogin;
import com.campus.campusqacommon.result.Result;
import com.campus.campusqapojo.dto.QuestionPublishDTO;
import com.campus.campusqapojo.dto.QuestionQueryDTO;
import com.campus.campusqapojo.dto.QuestionUpdateDTO;
import com.campus.campusqapojo.vo.PageResultVO;
import com.campus.campusqapojo.vo.QuestionDetailVO;
import com.campus.campusqapojo.vo.QuestionListVO;
import com.campus.campusqaservice.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * ClassName: QuestionController
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/20 17:07
 * Version:1.0
 */
@Tag(name = "问题接口")
@RestController
@RequestMapping("/question")
public class QuestionController {

    private final QuestionService questionService;
    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @RequireLogin
    @Operation(summary = "发布问题")
    @PostMapping("/publish")
    public Result<Long> publish(@Valid  @RequestBody QuestionPublishDTO dto) {
        Long questionId = questionService.publishQuestion(dto);
        return Result.success(questionId);
       }

    @Operation(summary = "问题分页查询")
    @GetMapping("/page")
    public Result<PageResultVO<QuestionListVO>> page(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false)String keyword,
        @RequestParam(required = false)String sort) {
        QuestionQueryDTO dto = new QuestionQueryDTO();
        dto.setPage(page);
        dto.setSize(size);
        dto.setCategoryId(categoryId);
        dto.setKeyword(keyword);
        dto.setSort(sort);
        return Result.success(questionService.list(dto));
    }

    @Operation(summary = "问题详情")
    @GetMapping("/{id}")
    public Result<QuestionDetailVO> detail(@PathVariable Long id) {
        return Result.success(questionService.detail(id));
    }

    @Operation(summary = "更新问题")
    @RequireLogin
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @Valid @RequestBody QuestionUpdateDTO dto) {
        questionService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除问题")
    @RequireLogin
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.success();
    }

    @Operation(summary = "关闭问题")
    @RequireLogin
    @PutMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        questionService.close(id);
        return Result.success();
    }
}
