package com.campus.campusqapojo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * ClassName: QuestionUpdateDTO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/14 16:15
 * Version:1.0
 */
@Data
public class QuestionUpdateDTO {

    @Size(max = 50)
    private String title;

    @Size(max = 10000)
    private String content;


    private Long categoryId;

    @Size(max = 10)
    private List<Long> tagIds;
}
