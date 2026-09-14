package com.campus.campusqapojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName: Question
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/13 17:35
 * Version:1.0
 */
@TableName("question")
@Data
public class Question {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 提问者ID
     */
    private Long userId;

    /**
     * 问题标题
     */
    private String title;

    /**
     * 问题内容（Markdown，最大10000字符）
     */
    private String content;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 回答数
     */
    private Integer answerCount;

    /**
     * 是否置顶：0普通 1置顶
     */
    private Integer isTop;

    /**
     * 是否已采纳回答：0未采纳 1已采纳（可选标记，便于列表展示"已解决"）
     */
    private Integer isAdopted;

    /**
     * 状态：0正常 1已关闭 2已删除
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
