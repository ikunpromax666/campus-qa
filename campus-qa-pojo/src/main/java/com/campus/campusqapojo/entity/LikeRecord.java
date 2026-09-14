package com.campus.campusqapojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName: LikeRecord
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/13 17:58
 * Version:1.0
 */
@TableName("like_record")
@Data
public class LikeRecord {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 目标ID（问题ID或回答ID）
     */
    private Long targetId;

    /**
     * 目标类型：1问题 2回答
     */
    private Integer targetType;

    /**
     * 行为类型：1点赞 2踩
     */
    private Integer actionType;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
