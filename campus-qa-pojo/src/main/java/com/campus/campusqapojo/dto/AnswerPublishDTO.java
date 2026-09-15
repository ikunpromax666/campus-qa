package com.campus.campusqapojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ClassName: AnswerPublishDTO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/14 17:19
 * Version:1.0
 */
@Data
public class AnswerPublishDTO {
    @NotNull
    private Long questionId;

    @NotBlank
    @Size(max = 5000)
    private String content;
}
