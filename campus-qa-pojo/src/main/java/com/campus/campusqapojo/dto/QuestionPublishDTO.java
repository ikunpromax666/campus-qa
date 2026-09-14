package com.campus.campusqapojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * ClassName: QuestionPubishDTO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/14 16:08
 * Version:1.0
 */
@Data
public class QuestionPublishDTO {

    @NotBlank(message = "标题不能为空")
    @Size(min = 5, max = 150, message="标题长度5-150位")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size( max = 10000)
    private String content;

    @NotNull
    private Long categoryId;

    @Size(max = 10)
    private List<Long> tagIds;
}
