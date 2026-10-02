package com.campus.campusqapojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName: MyAnswerVO
 * Description: "我的回答" / "赞过的回答"列表项。
 *              面试考点：列表 VO 不直接返回实体 —— Answer 实体没有问题标题，
 *              而"我的回答"页必须展示问题标题才能跳转，所以用组合 VO，
 *              问题标题通过批量查询填充（防 N+1），而不是每条回答查一次问题。
 * Author: SuperXia
 * Datetime :2026/10/2 03:40
 * Version:1.0
 */
@Data
public class MyAnswerVO {
    /** 回答ID */
    private Long id;

    /** 所属问题ID（前端跳转 /question/{id} 用） */
    private Long questionId;

    /** 问题标题（batch 查询填充） */
    private String questionTitle;

    /** 回答内容（Markdown 原文，前端截断展示） */
    private String content;

    private Integer likeCount;

    /** 是否采纳：0未采纳 1已采纳 */
    private Integer isAccepted;

    /** 回答者信息（"赞过的回答"页展示回答者；"我的回答"页就是自己） */
    private Long userId;

    private String nickname;

    private String avatar;

    private LocalDateTime createTime;
}
