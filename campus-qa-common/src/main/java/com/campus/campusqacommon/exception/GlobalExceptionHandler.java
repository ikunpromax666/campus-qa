package com.campus.campusqacommon.exception;

import com.campus.campusqacommon.result.Result;
import com.campus.campusqacommon.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：把 Controller 层抛出的异常统一转成 Result 返回
 *
 * 面试高频问题：
 * Q: 你们项目里异常怎么处理的？
 * A: 三层策略——
 *    ① 业务异常（手机号已注册）→ Service 层 throw BusinessException → 这里捕获返回 Result.fail
 *    ② 参数校验异常（@Valid 校验不通过）→ 单独捕获返回字段级别的错误信息
 *    ③ 兜底 Exception（空指针/数据库连接失败）→ 返回 Result.fail(500)，
 *       同时 log.error 打印完整堆栈，不把堆栈暴露给前端
 *
 * Q: @RestControllerAdvice 和 @ControllerAdvice 什么区别？
 * A: @RestControllerAdvice = @ControllerAdvice + @ResponseBody，
 *    方法返回值自动序列化为 JSON
 *
 * Q: 多个 @ExceptionHandler 怎么匹配？
 * A: Spring 按"能匹配的最精确类型"调用，子类先于父类。
 *    所以 BusinessException（最精确）会先于 Exception（兜底）被调用
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** ① 业务异常 → 返回枚举里的 code + message */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        // 业务异常是正常的业务流程分支，用 warn 级别，不打堆栈
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** ② 参数校验异常（@Valid + @RequestBody 触发） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        // 取第一个校验失败的字段和消息返回给前端
        FieldError firstError = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);
        String msg = firstError != null
                ? firstError.getField() + ": " + firstError.getDefaultMessage()
                : "参数校验失败";
        log.warn("参数校验异常: {}", msg);
        return Result.fail(ResultCode.VALIDATE_ERROR.getCode(), msg);
    }

    /** ③ 参数绑定异常（@Valid + 表单/Query 触发） */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        FieldError firstError = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);
        String msg = firstError != null
                ? firstError.getField() + ": " + firstError.getDefaultMessage()
                : "参数校验失败";
        log.warn("参数绑定异常: {}", msg);
        return Result.fail(ResultCode.VALIDATE_ERROR.getCode(), msg);
    }

    /** ④ 兜底：所有未预期异常 → 返回 500，堆栈只打日志 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        // 未知异常必须打 error 并带堆栈，方便排查
        log.error("系统异常", e);
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }
}
