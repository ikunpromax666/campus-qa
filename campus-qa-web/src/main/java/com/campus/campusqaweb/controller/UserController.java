package com.campus.campusqaweb.controller;

import com.campus.campusqacommon.annotation.RequireLogin;
import com.campus.campusqacommon.result.Result;
import com.campus.campusqapojo.dto.UserLoginDTO;
import com.campus.campusqapojo.dto.UserRegisterDTO;
import com.campus.campusqapojo.dto.UserUpdateDTO;
import com.campus.campusqapojo.dto.PasswordUpdateDTO;
import com.campus.campusqapojo.vo.LoginVO;
import com.campus.campusqapojo.vo.UserInfoVO;
import com.campus.campusqaservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * ClassName: UserController
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/15 17:17
 * Version:1.0
 */
@Tag(name = "用户模块", description = "注册、登录、个人信息")
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    // 构造器注入（推荐方式，Spring 4.3+ 自动注入）
    public UserController(UserService userService) {
        this.userService = userService;
    }
    

    @Operation(summary = "用户注册",description = "手机号注册，注册成功自动登录返回 token")
    @PostMapping("/register") public Result <LoginVO> register ( @RequestBody @Valid UserRegisterDTO dto) {
        LoginVO vo = userService.register(dto);
        return Result.success(vo);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid UserLoginDTO dto) {
        LoginVO vo = userService.login(dto);
        return Result.success(vo);
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    @RequireLogin
    public Result<UserInfoVO> getInfo() {
        UserInfoVO vo = userService.getCurrentUser();
        return Result.success(vo);
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/info")
    @RequireLogin
    public Result<Void> updateInfo(@RequestBody @Valid UserUpdateDTO dto) {
        userService.updateProfile(dto);
        return Result.success();
    }

    @Operation(summary = "修改密码",description = "用户登录后修改密码")
    @PutMapping("/password")
    @RequireLogin
    public Result<Void> updatePassword(@RequestBody @Valid PasswordUpdateDTO dto) {
        userService.updatePassword(dto);
        return Result.success();
    }
}
