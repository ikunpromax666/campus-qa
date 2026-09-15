package com.campus.campusqapojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ClassName: FavoriteToggleDTO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/14 17:28
 * Version:1.0
 */
@Data
public class FavoriteToggleDTO {
    @NotNull
    private Long questionId;
}
