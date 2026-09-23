package com.campus.campusqaweb.controller;

import com.campus.campusqacommon.annotation.RequireLogin;
import com.campus.campusqacommon.result.Result;
import com.campus.campusqapojo.dto.AnswerPublishDTO;
import com.campus.campusqapojo.dto.AnswerQueryDTO;
import com.campus.campusqapojo.dto.AnswerUpdateDTO;
import com.campus.campusqapojo.vo.AnswerVO;
import com.campus.campusqapojo.vo.PageResultVO;
import com.campus.campusqaservice.service.AnswerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ClassName: AnswerController
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/22 08:47
 * Version:1.0
 */
@RestController
@Tag(name = "回答接口")
@RequestMapping("/answer")
public class AnswerController {
    private final AnswerService answerService;


    public AnswerController(AnswerService answerService) {
        this.answerService = answerService;

    }

    @Operation(summary = "发布回答")
    @PostMapping("/publish")
    @RequireLogin
    public Result<Void> publishAnswer(@Valid @RequestBody AnswerPublishDTO dto) {
        answerService.publishAnswer(dto);
        return Result.success();
    }


    @PostMapping ("/list")
    @Operation(summary = "查询回答列表")
    public Result<PageResultVO<AnswerVO>> listAnswers(@Valid @RequestBody AnswerQueryDTO dto) {
        PageResultVO<AnswerVO> pageResultVO = answerService.listAnswers(dto);
        return Result.success(pageResultVO);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新回答")
    @RequireLogin
    public Result<Void> updateAnswer(@PathVariable Long id, @Valid @RequestBody AnswerUpdateDTO dto) {
        answerService.updateAnswer(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除回答")
    @RequireLogin
    public Result<Void> deleteAnswer(@PathVariable Long id) {
        answerService.deleteAnswer(id);
        return Result.success();
    }

    @RequireLogin
    @PostMapping("accept")
    @Operation(summary = "采纳最佳回答")
    public Result<Void> acceptBestAnswer(Long answerId, Long questionId) {
        answerService.acceptBestAnswer(answerId, questionId);
        return Result.success();
    }
}
