package com.campus.campusqapojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ClassName: QuestionListVO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/14 16:20
 * Version:1.0
 */
@Data
public class QuestionListVO {
    private Long id;

    private String title;

    private String nickname;

    private String avatar;

    private String categoryName;

    private List<String> tags;

    private Integer viewCount;

    private Integer likeCount;

    private Integer answerCount;

    private LocalDateTime createTime;
}
