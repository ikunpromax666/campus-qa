package com.campus.campusqapojo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ClassName: LikeToggleDTO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/14 17:26
 * Version:1.0
 */
@Data
public class LikeToggleDTO {

    @NotNull
    private Long targetId;


    @NotNull
    @Min(1)
    @Max(2)
    private Integer targetType;



}
