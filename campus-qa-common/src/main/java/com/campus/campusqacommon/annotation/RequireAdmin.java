package com.campus.campusqacommon.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ClassName: RequireAdmin
 * Description: 标记需要管理员权限才能访问的接口
 *
 * 用法：可标注在 Controller 类上（整个类生效）或方法上。
 * 拦截器校验顺序：先验登录（@RequireLogin 的前置条件），再验 role == 1。
 * @RequireAdmin 隐含 @RequireLogin 的语义——管理员必然已登录。
 *
 * 面试高频问题：
 * Q: 为什么单独做 @RequireAdmin 而不是给 @RequireLogin 加个属性 value()？
 * A: 单独注解语义更清晰、拦截器分支更简单：
 *    if (hasRequireAdmin) 验管理员 else if (hasRequireLogin) 验登录；
 *    若合并成一个注解加属性，拦截器里还要处理"两个注解同时标了怎么办"的组合歧义。
 *
 * Q: 为什么 @Retention 必须是 RUNTIME？
 * A: 同 @RequireLogin——拦截器运行期靠反射读注解，
 *    CLASS 级别的注解运行期不可见，校验会静默失效。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAdmin {
}
