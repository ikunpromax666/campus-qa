package com.campus.campusqaservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.campusqacommon.exception.BusinessException;
import com.campus.campusqacommon.result.ResultCode;
import com.campus.campusqacommon.utils.JwtUtils;
import com.campus.campusqamapper.mapper.UserMapper;
import com.campus.campusqapojo.dto.PasswordUpdateDTO;
import com.campus.campusqapojo.dto.UserLoginDTO;
import com.campus.campusqapojo.dto.UserRegisterDTO;
import com.campus.campusqapojo.dto.UserUpdateDTO;
import com.campus.campusqapojo.entity.User;
import com.campus.campusqapojo.vo.LoginVO;
import com.campus.campusqapojo.vo.UserInfoVO;
import com.campus.campusqaservice.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * ClassName: UserServiceImpl
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/15 17:02
 * Version:1.0
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    public UserServiceImpl (UserMapper userMapper, JwtUtils jwtUtils) {
        this .userMapper = userMapper;
        this .jwtUtils = jwtUtils;
    }

    @Override
    /** 注册：校验手机号唯一性、BCrypt 加密密码、生成 JWT */
    public LoginVO register(UserRegisterDTO dto) {
        //确认密码是否一致
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_MISMATCH.getCode(),ResultCode.PASSWORD_MISMATCH.getMessage());
        }

        // 校验手机号是否存在
        long count = userMapper.selectCount(
                new QueryWrapper<User>().eq("phone", dto.getPhone())
        );
        if (count > 0) {
            throw new BusinessException(ResultCode.PHONE_EXISTS.getCode(),ResultCode.PHONE_EXISTS.getMessage());
        }


        // 1. BCrypt 加密
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedPassword = encoder.encode(dto.getPassword());

        //构建实体
        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPassword(encodedPassword);
        user.setNickname(dto.getNickname());
        user.setRole(0);
        userMapper.insert(user);

        // 2. 生成 JWT
        String token = jwtUtils.createToken(user.getId(), user.getRole());

        //返回vo
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setNickname(user.getNickname());
        return vo;
    }
    @Override
    /** 登录：校验手机号+密码、生成 JWT */
    public LoginVO login(UserLoginDTO dto) {
        return null;
    }
    /** 获取当前登录用户信息 */
    @Override
    public UserInfoVO getCurrentUser() {
        return null;
    }
    /** 更新个人资料（昵称/头像/简介） */
    @Override
    public void updateProfile(UserUpdateDTO dto) {

    }
    /** 修改密码 */
    @Override
    public void updatePassword(PasswordUpdateDTO dto) {

    }

}
