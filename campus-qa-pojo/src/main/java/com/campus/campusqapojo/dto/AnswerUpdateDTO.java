package com.campus.campusqapojo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ClassName: AnswerUpdateDTO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/14 17:20
 * Version:1.0
 */
@Data
public class AnswerUpdateDTO {
    @Size(max = 5000)
    private String content;
}
