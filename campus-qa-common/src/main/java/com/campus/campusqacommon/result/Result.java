package com.campus.campusqacommon.result;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *ClassName: Result
 * Description: Result 统一返回结果包装类
 *
 * 统一返回结果包装类
 *  *
 *  * 面试高频问题：
 *  * Q: 后端 API 返回格式为什么要统一？
 *  * A: 统一格式让前端解析逻辑只写一套、异常处理好做、便于做统一日志记录。
 *  *    返回 { code, message, data } 三段式：code 业务状态、message 提示文案、data 真正数据。
 *  *
 *  * Q: 为什么用静态工厂方法而不是 @Builder ？
 *  * A: Result 调用频率极高且字段只有 3 个。
 *  *    - 工厂方法：Result.success(user)  / Result.fail("xxx") → 简洁 1 行；
 *  *    - @Builder ：Result.builder().code(200).message("成功").data(user).build() → 啰嗦 4 倍。
 *  * @Builder 适合字段多、可选参数多的 DTO/配置类，Result 不适用。
 * Author: SuperXia
 * Datetime :2026/9/11 14:17
 *  Version:1.0
 * */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {


    private Integer code;
    private String message;
    private T data;

    //有数据成功返回
    public static <T> Result<T> success( T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    //无数据成功返回
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    //失败返回
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    //业务失败：用枚举（推荐，错误码集中管理）
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    //业务失败：只传 message（code 自动走通用业务错误码 10000）
    public static <T> Result<T> fail(String message) {
        return new Result<>(ResultCode.BUSINESS_ERROR.getCode(), message, null);
    }

    /*//判断是否成功
    Service 层不返回 Result，所以 isSuccess() 不在业务主流程用；它的价值在 横切关注点 （测试、AOP 日志、远程调用判断）里
    public boolean isSuccess () {
        return this.code != null && this.code == ResultCode.SUCCESS.getCode();
    }*/

}
