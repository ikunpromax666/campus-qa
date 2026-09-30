package com.campus.campusqapojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ClassName: TagCreateDTO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/30 17:31
 * Version:1.0
 */
@Data
public class TagCreateDTO {
    @NotBlank(message = "标签名称不能为空")
    private String name;
}
