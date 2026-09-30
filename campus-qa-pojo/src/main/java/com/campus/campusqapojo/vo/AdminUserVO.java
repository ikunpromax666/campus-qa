package com.campus.campusqapojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName: AdminUserVO
 * Description: 管理后台的用户列表项
 *
 * 为什么不复用 User 实体：
 *   实体里有 password（BCrypt 哈希）和明文手机号，直接当响应体返回就是把密码哈希交出去了。
 *   DTO/VO 分层不是"多写个类显得规范"，它是安全边界。
 *   phone 字段在 Service 赋值时就已经脱敏，VO 全程不持有明文 —— 从源头切断泄露路径。
 * Author: SuperXia
 * Datetime :2026/9/29 16:10
 * Version:1.0
 */
@Data
public class AdminUserVO {

    /**
     * 用户ID
     * 必须用 Long：数据库 user.id 是 BIGINT，实体和 LoginUser 也都是 Long，
     * 用 int 既类型不一致，将来 id 超过 21 亿还会直接溢出成负数
     */
    private Long id;

    /** 手机号：已脱敏，形如 133****8052 */
    private String phone;

    private String nickname;

    private String avatar;

    /** 角色：0 普通用户 1 管理员 */
    private Integer role;

    /** 状态：0 正常 1 禁用 */
    private Integer status;

    private LocalDateTime createTime;
}
