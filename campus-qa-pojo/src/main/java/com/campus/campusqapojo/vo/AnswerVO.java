package com.campus.campusqapojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName: AnswerVO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/14 17:23
 * Version:1.0
 */
@Data
public class AnswerVO {
    private Long id;

    private Long questionId;

    private Long userId;

    private String nickname;

    private String avatar;

    private String content;

    private Integer likeCount;

    private Integer isAccepted;

    private Integer likeStatus;

    private Boolean isOwner;

    private LocalDateTime createTime;

}
