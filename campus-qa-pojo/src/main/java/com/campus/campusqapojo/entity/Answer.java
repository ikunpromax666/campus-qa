package com.campus.campusqapojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName: Answer
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/13 17:53
 * Version:1.0
 */

@TableName("answer")
@Data
public class Answer {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 问题ID
     */
    private Long questionId;

    /**
     * 回答者ID
     */
    private Long userId;

    /**
     * 回答内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer likeCount;
    /**
     * 是否采纳：0未采纳 1已采纳
     */

    private Integer isAccepted;
    /**
     * 状态：0正常 1已删除
     */
    private Integer status;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
