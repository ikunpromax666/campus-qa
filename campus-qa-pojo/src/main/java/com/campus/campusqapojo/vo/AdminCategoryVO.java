package com.campus.campusqapojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName: AdminCategoryVO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/30 17:17
 * Version:1.0
 */
@Data
public class AdminCategoryVO {

    private Long id;

    private String name;

    private String description;

    private Integer sortOrder;

    private LocalDateTime createTime;

}
