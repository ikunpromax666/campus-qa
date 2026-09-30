package com.campus.campusqapojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ClassName: CategoryUpdateDTO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/30 17:30
 * Version:1.0
 */
@Data
public class CategoryUpdateDTO {
    @NotBlank
    private String name;

    private String description;

    private Integer sortOrder;
}
