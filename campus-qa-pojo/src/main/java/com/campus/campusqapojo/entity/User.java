package com.campus.campusqapojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName: User
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/13 17:19
 * Version:1.0
 */

@TableName("user")
@Data
public class User  {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 手机号（登录账号）
     */
    private String phone;

    /**
     * 密码（BCrypt加密）
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 角色：0普通用户 1管理员
     */
    private Integer role;

    /**
     * 账号状态：0正常 1禁用
     */
    private Integer status;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
