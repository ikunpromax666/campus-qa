package com.campus.campusqapojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ClassName: CategoryCreateDTO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/30 17:28
 * Version:1.0
 */
@Data
public class CategoryCreateDTO {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private String description;

    private Integer sortOrder;
}
