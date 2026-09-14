package com.campus.campusqapojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: PasswordUpdateDTO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/14 08:56
 * Version:1.0
 */

@Data
//DTO 走 HTTP + JSON（Jackson）传输，Jackson 不要求 Serializable。它只在 JVM 原生序列化（RMI、Session 持久化）时需要
public class PasswordUpdateDTO  {


    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message="新密码长度6-20位")
    private String newPassword;

    // TODO 确认新密码是否与旧密码相同
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
