package com.campus.campusqaservice.service;

import com.campus.campusqapojo.dto.FavoriteToggleDTO;
import com.campus.campusqapojo.dto.LikeToggleDTO;
import com.campus.campusqapojo.vo.MyAnswerVO;
import com.campus.campusqapojo.vo.PageResultVO;
import com.campus.campusqapojo.vo.QuestionListVO;
import com.campus.campusqapojo.vo.ToggleResultVO;

/**
 * ClassName: InteractionService
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/23 15:33
 * Version:1.0
 */
public interface InteractionService {
    // 点赞 toggle
    ToggleResultVO toggleLike (LikeToggleDTO dto) ;
    // 踩toggle
    ToggleResultVO toggleDislike (LikeToggleDTO dto) ;

    ToggleResultVO toggleFavorite (FavoriteToggleDTO dto) ;

    /** 我收藏的问题（收藏时间倒序） */
    PageResultVO<QuestionListVO> myFavorites(Integer page, Integer size);

    /** 我赞过的问题（点赞时间倒序） */
    PageResultVO<QuestionListVO> myLikedQuestions(Integer page, Integer size);

    /** 我赞过的回答（点赞时间倒序） */
    PageResultVO<MyAnswerVO> myLikedAnswers(Integer page, Integer size);
}
