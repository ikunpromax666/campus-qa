package com.campus.campusqapojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName: Tag
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/13 17:34
 * Version:1.0
 */

@TableName("tag")
@Data
public class Tag {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 标签名
     */
    private String name;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
