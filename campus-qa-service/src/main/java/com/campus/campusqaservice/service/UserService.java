package com.campus.campusqaservice.service;

import com.campus.campusqapojo.dto.PasswordUpdateDTO;
import com.campus.campusqapojo.dto.UserLoginDTO;
import com.campus.campusqapojo.dto.UserRegisterDTO;
import com.campus.campusqapojo.dto.UserUpdateDTO;
import com.campus.campusqapojo.vo.LoginVO;
import com.campus.campusqapojo.vo.UserInfoVO;

/**
 * ClassName: UserService
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/15 17:02
 * Version:1.0
 */
public interface UserService {
    /** 注册：校验手机号唯一性、BCrypt 加密密码、生成 JWT */
    LoginVO register(UserRegisterDTO dto);

    /** 登录：校验手机号+密码、生成 JWT */
    LoginVO login(UserLoginDTO dto);

    /** 获取当前登录用户信息 */
    UserInfoVO getCurrentUser();

    /** 更新个人资料（昵称/头像/简介） */
    void updateProfile(UserUpdateDTO dto);

    /** 修改密码 */
    void updatePassword(PasswordUpdateDTO dto);
}
