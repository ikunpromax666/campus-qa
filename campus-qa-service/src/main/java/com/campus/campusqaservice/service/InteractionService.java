package com.campus.campusqaservice.service;

import com.campus.campusqapojo.dto.FavoriteToggleDTO;
import com.campus.campusqapojo.dto.LikeToggleDTO;
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

}
