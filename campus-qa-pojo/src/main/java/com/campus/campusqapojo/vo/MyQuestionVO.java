package com.campus.campusqapojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ClassName: MyQuestionVO
 * Description: "我的提问"列表项 —— 作者视角，比 QuestionListVO 多 status，
 *              少 nickname/avatar（都是自己的，不需要批查用户，省一次查询）。
 *              面试考点：同一个"问题"在不同视角下暴露的字段不同：
 *              广场列表（他人视角）/ 我的提问（作者视角，带状态）/ 管理后台（管理员视角），
 *              各建各的 VO，而不是一个万能 VO 全字段塞 —— 字段越少，契约越稳，泄露面越小。
 * Author: SuperXia
 * Datetime :2026/10/2
 * Version:1.0
 */
@Data
public class MyQuestionVO {
    private Long id;

    private String title;

    /** 状态：0正常 1已关闭（已删除的不进列表） */
    private Integer status;

    private String categoryName;

    private List<String> tags;

    private Integer viewCount;

    private Integer likeCount;

    private Integer answerCount;

    private LocalDateTime createTime;
}
