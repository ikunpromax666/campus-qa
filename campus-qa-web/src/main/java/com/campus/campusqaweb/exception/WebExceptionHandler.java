package com.campus.campusqaweb.exception;

import com.campus.campusqacommon.result.Result;
import com.campus.campusqacommon.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * ClassName: WebExceptionHandler
 * Description: Web 层专属异常处理（只放需要 spring-webmvc 类型的异常）
 * Author: SuperXia
 * Datetime :2026/9/21
 * Version:1.0
 *
 * 面试考点：
 * Q: 为什么这个类不放 common 模块的 GlobalExceptionHandler 里？
 * A: NoResourceFoundException 在 spring-webmvc 包下，而 common 模块只依赖 spring-web
 *    （刻意不引入 webmvc，保持底层库性质）。Web 层专属异常就留在 web 模块处理。
 *
 * Q: 两个 @RestControllerAdvice 同时存在，谁生效？
 * A: Spring 会按 @Order 顺序遍历所有 advice，第一个「有匹配方法」的 advice 直接返回。
 *    所以这里必须加 HIGHEST_PRECEDENCE，否则 common 里 @ExceptionHandler(Exception.class)
 *    的兜底会先接住 NoResourceFoundException，又打成 ERROR 堆栈。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class WebExceptionHandler {

    /**
     * 静态资源不存在 —— 典型的噪音请求，例如 Chrome DevTools 每次打开控制台都会探测
     * /.well-known/appspecific/com.chrome.devtools.json。
     * 属于正常现象，用 debug 级别记录，返回 404 即可，不打 ERROR 堆栈。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.debug("静态资源不存在: {}", e.getResourcePath());
        return Result.fail(ResultCode.NOT_FOUND);
    }
}
