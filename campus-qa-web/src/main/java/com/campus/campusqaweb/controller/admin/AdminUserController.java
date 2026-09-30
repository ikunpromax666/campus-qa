package com.campus.campusqaweb.controller.admin;

import com.campus.campusqacommon.annotation.RequireAdmin;
import com.campus.campusqacommon.result.Result;
import com.campus.campusqapojo.dto.UserStatusUpdateDTO;
import com.campus.campusqapojo.vo.AdminUserVO;
import com.campus.campusqapojo.vo.PageResultVO;
import com.campus.campusqaservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ClassName: AdminUserController
 * Description: 管理后台 - 用户管理
 *
 * 面试高频问题：
 * Q: @RequireAdmin 为什么打在类上而不是每个方法上？
 * A: 管理端接口整体都要求管理员权限，打在类上是"默认拒绝、无需逐个标注"；
 *    标在方法上只要漏标一个，就是一个暴露出去的越权接口。安全配置应当默认关闭。
 *
 * Q: 为什么用 @RestController 而不是 @Controller？
 * A: @RestController = @Controller + @ResponseBody。
 *    只写 @Controller 时，返回值会被当成【视图名】交给视图解析器去找 JSP/HTML，
 *    返回一个 PageResultVO 对象会直接 404/500 —— 接口静默不可用。
 * Author: SuperXia
 * Datetime :2026/9/29 16:31
 * Version:1.0
 */
@Tag(name = "管理后台-用户管理")
@RestController
@RequestMapping("/admin/user")
@RequireAdmin
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户列表（分页）")
    @GetMapping("/list")
    public Result<PageResultVO<AdminUserVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(userService.listUsers(page, size, keyword, status));
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestBody @Valid UserStatusUpdateDTO dto) {
        userService.updateStatus(id, dto);
        return Result.success();
    }
}
