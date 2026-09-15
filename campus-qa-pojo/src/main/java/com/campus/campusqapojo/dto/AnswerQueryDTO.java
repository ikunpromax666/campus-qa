package com.campus.campusqapojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ClassName: AnswerQueryDTO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/14 17:21
 * Version:1.0
 */
@Data
public class AnswerQueryDTO {
    @NotNull
    private Long questionId;

    private Integer page = 1;

    private Integer size = 10;
}
