package com.campus.campusqacommon.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ClassName: LoginUser
 * Description: 当前登录用户的最小信息载体，由拦截器解析 token 后放入 ThreadLocal
 *
 * 面试高频问题：
 * Q: 为什么只放 userId 和 role 两个字段？
 * A: ThreadLocal 里的对象生命周期跟着线程走（线程池里线程不死），
 *    字段多了既占内存、又容易形成对 User 实体的反向依赖；
 *    Controller 需要详细信息时拿 userId 去查库/查缓存即可。
 *
 * Q: 为什么 userId 用 Long？
 * A: 数据库 user.id 是 BIGINT，Java 侧用 Long 一一对应，
 *    避免 String/Long 混用带来的转换成本和隐藏 bug。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginUser {

    /** 用户ID（对应 user.id，BIGINT） */
    private Long userId;

    /** 角色：0 普通用户 1 管理员（对应 user.role，TINYINT） */
    private Integer role;

}
