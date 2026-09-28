package com.campus.campusqapojo.dto;

import lombok.Data;

/**
 * ClassName: AnswererStatDTO
 * Description: 优秀回答者榜的聚合统计结果，对应 answer 表 GROUP BY user_id 后的一行
 * Author: SuperXia
 * Datetime :2026/9/24
 * Version:1.0
 */
@Data
public class AnswererStatDTO {

    /**
     * 回答者ID
     */
    private Long userId;

    /**
     * 回答总数
     */
    private Integer answerCount;

    /**
     * 被采纳的回答数
     */
    private Integer acceptedCount;

    /**
     * 累计获赞数
     */
    private Integer likeSum;

    /**
     * 贡献分（排行榜的 score，在 SQL 里算好，见 AnswerMapper#selectAnswererStats）
     */
    private Double score;
}
