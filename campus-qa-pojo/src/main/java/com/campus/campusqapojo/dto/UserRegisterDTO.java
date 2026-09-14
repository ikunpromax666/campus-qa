package com.campus.campusqapojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: UserRegisterDTO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/14 08:43
 * Version:1.0
 */
@Data
public class UserRegisterDTO  {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    @Size(min=6, max=20, message="密码长度6-20位")
    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    //TODO 验证密码是否一致
    private String confirmPassword;

    @NotBlank(message = "昵称不能为空")
    @Size(min=1, max=20, message="昵称长度1-20位")
    private String nickname;

}
