package com.campus.campusqapojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName: UserInfoVO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/14 15:40
 * Version:1.0
 */
@Data
public class UserInfoVO {
    private Long id;

    private String phone;

    private String nickname;

    private String avatar;

    private String bio;

    private Integer role;

    private LocalDateTime createTime;
}
