package com.campus.campusqaweb.controller.user;

import com.campus.campusqacommon.annotation.RequireLogin;
import com.campus.campusqacommon.result.Result;
import com.campus.campusqapojo.dto.FavoriteToggleDTO;
import com.campus.campusqapojo.dto.LikeToggleDTO;
import com.campus.campusqapojo.vo.MyAnswerVO;
import com.campus.campusqapojo.vo.PageResultVO;
import com.campus.campusqapojo.vo.QuestionListVO;
import com.campus.campusqapojo.vo.ToggleResultVO;
import com.campus.campusqaservice.service.InteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ClassName: InteractionController
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/23 15:28
 * Version:1.0
 */
@Tag(name = "互动接口")
@RestController
@RequestMapping("/interaction")
public class InteractionController {
    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @RequireLogin
    @Operation(summary = "点赞/取消点赞")
    @PostMapping("/like")
    public Result<ToggleResultVO> toggleLike(@Valid @RequestBody LikeToggleDTO dto) {
        return Result.success(interactionService.toggleLike(dto));
    }

    @RequireLogin
    @Operation(summary = "踩/取消踩")
    @PostMapping("/dislike")
    public Result<ToggleResultVO> toggleDislike(@Valid @RequestBody LikeToggleDTO dto) {
        return Result.success(interactionService.toggleDislike(dto));
    }

    @RequireLogin
    @Operation(summary = "收藏/取消收藏")
    @PostMapping("/favorite")
    public Result<ToggleResultVO> toggleFavorite(@Valid @RequestBody FavoriteToggleDTO dto) {
        return Result.success(interactionService.toggleFavorite(dto));
    }

    // ==================== 我的收藏 / 我的点赞（GET 查询，参数走 @RequestParam，GET 不带 body） ====================

    @RequireLogin
    @Operation(summary = "我收藏的问题")
    @GetMapping("/my/favorites")
    public Result<PageResultVO<QuestionListVO>> myFavorites(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(interactionService.myFavorites(page, size));
    }

    @RequireLogin
    @Operation(summary = "我赞过的问题")
    @GetMapping("/my/likes/questions")
    public Result<PageResultVO<QuestionListVO>> myLikedQuestions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(interactionService.myLikedQuestions(page, size));
    }

    @RequireLogin
    @Operation(summary = "我赞过的回答")
    @GetMapping("/my/likes/answers")
    public Result<PageResultVO<MyAnswerVO>> myLikedAnswers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(interactionService.myLikedAnswers(page, size));
    }
}
