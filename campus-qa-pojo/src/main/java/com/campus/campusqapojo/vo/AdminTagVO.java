package com.campus.campusqapojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName: AdminTagVO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/30 17:19
 * Version:1.0
 */
@Data
public class AdminTagVO {
    private Long id;

    private String name;

    private LocalDateTime createTime;
}
