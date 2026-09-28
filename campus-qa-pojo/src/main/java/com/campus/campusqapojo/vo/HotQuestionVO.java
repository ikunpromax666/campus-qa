package com.campus.campusqapojo.vo;

import lombok.Data;

/**
 * ClassName: HotQuestionVO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/24 13:34
 * Version:1.0
 */
@Data
public class HotQuestionVO {

    // 名次：1、2、3…
    private Integer rank;

     private Long questionId;

     private String title;

     private Integer likeCount;

     private Integer answerCount;

     private Integer viewCount;

     //提问者
     private String nickname;

     //热度分（前端可展示"热度"角标）
     private Double score;

}
