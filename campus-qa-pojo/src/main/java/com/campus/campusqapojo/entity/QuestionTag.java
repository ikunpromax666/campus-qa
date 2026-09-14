package com.campus.campusqapojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * ClassName: QuestionTag
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/13 17:51
 * Version:1.0
 */
@TableName("question_tag")
@Data
public class QuestionTag {

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
     * 标签ID
     */
    private Long tagId;
}
