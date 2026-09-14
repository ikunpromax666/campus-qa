package com.campus.campusqapojo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: UserUpdateDTO
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/14 08:55
 * Version:1.0
 */
@Data
public class UserUpdateDTO  {

    @Size(min = 1, max = 20, message="昵称长度1-20位")
    private String nickname;

    @Size(max = 200)
    private String avatar;

    @Size(max = 200)
    private String bio;


}
