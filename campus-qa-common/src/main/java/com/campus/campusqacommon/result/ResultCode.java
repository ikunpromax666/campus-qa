package com.campus.campusqacommon.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * ClassName: ResultCode
 * Description:统一错误码枚举
 *
 * 面试高频问题：
 *  * Q: 你们项目里错误码怎么管理的？为什么用枚举不用常量类？
 *  * A: 用枚举集中管理所有错误码，每个枚举常量同时携带 code + message。
 *  *    比常量类（public static final int 10001）好的地方：
 *  *    ① 类型安全——方法参数可以写 ResultCode 而不是 int，传错数字编译就报错；
 *  *    ② 自带 message，业务里抛异常只写 ResultCode.PHONE_EXISTS，不用重复写 message；
 *  *    ③ 文档即代码——IDEA 里 Ctrl+B 跳进去就能看到所有错误码的含义。
 *  *
 *  * Q: 错误码怎么分段的？
 *  * A: 按模块分段，便于快速定位来源：
 *  *    - 200      成功
 *  *    - 401/403  认证鉴权相关（通用 HTTP 语义）
 *  *    - 500      系统异常
 *  *    - 10000+   用户模块（注册/登录/禁用）
 *  *    - 20000+   问题模块
 *  *    - 30000+   回答模块
 *  *    - 40000+   互动模块（点赞/收藏）
 * Author: SuperXia
 * Datetime :2026/9/11 15:18
 * Version:1.0
 */

@Getter
@AllArgsConstructor
public enum ResultCode {
    //通用状态码

    SUCCESS(200, "操作成功"),
    UNAUTHORIZED( 401 , "未登录或Token已过期" ),
    FORBIDDEN( 403 , "无权限访问" ),
    SYSTEM_ERROR( 500 , "系统错误，请稍后重试" ),
    BUSINESS_ERROR( 10000 , "业务异常" ),
    VALIDATE_ERROR( 400 , "参数校验失败" ),

    //用户模块状态码
    PHONE_EXISTS( 10001 , "手机号已注册" ),
    PHONE_NOT_REGISTERED( 10002 , "手机号未注册" ),
    PASSWORD_ERROR( 10003 , "密码错误" ),
    USER_DISABLED( 10004 , "账号已被禁用" ),
    NICKNAME_EXISTS( 10005 , "昵称已被使用" ),
    PASSWORD_MISMATCH( 10006 , "两次输入密码不一致" ),

    //问题模块状态码
    QUESTION_NOT_FOUND( 20001 , "问题不存在或已删除" ),
    QUESTION_CLOSED( 20002 , "问题已关闭，无法回答" ),
    QUESTION_NOT_YOURS( 20003 , "无权操作该问题" ),

    //回答模块状态码
    ANSWER_NOT_FOUND( 30001 , "回答不存在或已删除" ),
    ANSWER_NOT_YOURS( 30002 , "无权操作该回答" ),
    ANSWER_ALREADY_ACCEPTED( 30003 , "该问题已有最佳答案" ),

    //互动状态码
    ALREADY_LIKED( 40001 , "您已经点赞过了" ),
    ALREADY_FAVORITED( 40002 , "您已经收藏过了" );

    private final Integer code;
    private final String message;


}
