package com.campus.campusqacommon.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ClassName: RequireLogin
 * Description: 标记需要登录才能访问的接口
 *
 * 用法：可标注在 Controller 类上（整个类生效）或方法上。
 * web 模块的登录拦截器在 preHandle 里通过 HandlerMethod 反射读取该注解决定是否校验。
 *
 * 面试高频问题：
 * Q: 为什么 @Retention 必须是 RUNTIME？
 * A: 注解生命周期三档：SOURCE（编译即丢）→ CLASS（进 class 文件但运行期不可见）→
 *    RUNTIME（运行期可通过反射读到）。拦截器是运行期反射读注解，
 *    若用默认的 CLASS，注解运行期等于"不存在"，权限校验会静默失效——很难排查。
 *
 * Q: 为什么用"标记注解 + 拦截器"，而不是每个接口手写 if 判断登录？
 * A: 声明式编程：权限规则与业务代码解耦，拦截器一处统一生效，
 *    新接口只需加一个注解，避免漏判和重复代码（AOP 思想）。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireLogin {
}
