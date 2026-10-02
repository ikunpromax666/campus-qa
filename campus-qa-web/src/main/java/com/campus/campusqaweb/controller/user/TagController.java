package com.campus.campusqaweb.controller.user;

import com.campus.campusqacommon.result.Result;
import com.campus.campusqapojo.vo.AdminTagVO;
import com.campus.campusqaservice.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ClassName: TagController
 * Description: 前台 - 标签列表（供发布问题时的标签多选数据源）
 *
 * 与 CategoryController 同模式：Service 按领域不分调用方（复用 TagService.list()），
 * Controller 按调用方划分（user/admin 各自视图层）。
 * 复用 AdminTagVO 是有意为之：多一个 createTime 字段对前台无副作用，
 * 为此再建一个只有 id+name 的 TagVO 属于过度设计（YAGNI）。
 * Author: SuperXia
 * Datetime :2026/10/2 17:55
 * Version:1.0
 */
@Tag(name = "前台-标签")
@RestController
@RequestMapping("/tag")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @Operation(summary = "标签列表")
    @GetMapping("/list")
    public Result<List<AdminTagVO>> list() {
        return Result.success(tagService.list());
    }
}
