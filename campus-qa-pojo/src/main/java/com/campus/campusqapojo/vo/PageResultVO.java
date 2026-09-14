package com.campus.campusqapojo.vo;

import lombok.Data;

import java.util.List;

/**
 * ClassName: PageResultVO
 * Description:所有分页列表
 * Author: SuperXia
 * Datetime :2026/9/14 16:34
 * Version:1.0
 */
@Data
public class PageResultVO<T> {
    private List<T> records;

    private Long total;

    private Integer page;

    private Integer size;
}
