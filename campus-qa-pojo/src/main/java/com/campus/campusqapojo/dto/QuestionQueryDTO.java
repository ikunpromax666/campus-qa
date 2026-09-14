package com.campus.campusqapojo.dto;

import lombok.Data;

/**
 * ClassName: QuestionQueryDTO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/14 16:16
 * Version:1.0
 */
@Data
public class QuestionQueryDTO {
    private Integer page = 1;

    private Integer size = 10;

    private Long categoryId;

    private String keyword;

    private String sort;
}
