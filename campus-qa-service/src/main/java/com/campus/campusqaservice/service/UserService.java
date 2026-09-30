package com.campus.campusqaservice.service;

import com.campus.campusqapojo.dto.PasswordUpdateDTO;
import com.campus.campusqapojo.dto.UserLoginDTO;
import com.campus.campusqapojo.dto.UserRegisterDTO;
import com.campus.campusqapojo.dto.UserStatusUpdateDTO;
import com.campus.campusqapojo.dto.UserUpdateDTO;
import com.campus.campusqapojo.vo.AdminUserVO;
import com.campus.campusqapojo.vo.LoginVO;
import com.campus.campusqapojo.vo.PageResultVO;
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

    /** 管理员：用户分页列表（支持昵称/手机号模糊搜索、按状态筛选，手机号脱敏返回） */
    PageResultVO<AdminUserVO> listUsers(Integer page, Integer size, String keyword, Integer status);

    /** 管理员：启用/禁用用户（id 走路径参数，仅 status 走请求体） */
    void updateStatus(Long id, UserStatusUpdateDTO dto);

    /** 判断用户当前是否处于正常状态（供登录拦截器校验"禁用立即生效"） */
    boolean isUserEnabled(Long userId);
}
