package com.campus.campusqapojo.vo;

import lombok.Data;

/**
 * ClassName: AnswererRankVO
 * Description: 优秀回答者榜展示对象
 * Author: SuperXia
 * Datetime :2026/9/24
 * Version:1.0
 */
@Data
public class AnswererRankVO {

    /**
     * 名次：1、2、3…
     */
    private Integer rank;

    /**
     * 回答者ID
     */
    private Long userId;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 贡献分（前端可展示"贡献值"角标）
     */
    private Double score;

    /**
     * 回答总数（来自明细缓存，缓存缺失时为 null）
     */
    private Integer answerCount;

    /**
     * 被采纳的回答数（来自明细缓存，缓存缺失时为 null）
     */
    private Integer acceptedCount;

    /**
     * 累计获赞数（来自明细缓存，缓存缺失时为 null）
     */
    private Integer likeSum;
}
