package com.campus.campusqapojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ClassName: QuestionDetailVO
 * Description:问题详情展示
 * Author: SuperXia
 * Datetime :2026/9/14 16:24
 * Version:1.0
 */
@Data
public class QuestionDetailVO {
    private Long id;

    private String title;

    private String content;

    private Long userId;

    private String nickname;

    private String avatar;

    private CategoryVO category;

    private List<TagVO> tags;

    private Integer viewCount;

    private Integer likeCount;

    private Integer answerCount;

    private Integer isTop;

    private LocalDateTime createTime;

    private Integer likeStatus;

    private Boolean isFavorited;

    private Boolean isOwner;
}
