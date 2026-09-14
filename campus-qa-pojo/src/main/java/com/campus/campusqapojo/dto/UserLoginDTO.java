package com.campus.campusqapojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: UserLoginDTO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/14 08:54
 * Version:1.0
 */
@Data
public class UserLoginDTO  {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;

}
