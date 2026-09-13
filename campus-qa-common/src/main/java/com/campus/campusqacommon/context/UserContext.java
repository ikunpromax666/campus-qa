package com.campus.campusqacommon.context;

import com.campus.campusqacommon.exception.BusinessException;
import com.campus.campusqacommon.result.ResultCode;

/**
 * ClassName: UserContext
 * Description: 基于 ThreadLocal 的登录用户上下文
 *
 * 使用链路：拦截器 preHandle 解析 token → UserContext.set(loginUser)
 *          → 业务代码 UserContext.getUserId() 拿当前用户
 *          → 拦截器 afterCompletion 里 UserContext.clear() 清理
 *
 * 面试高频问题：
 * Q: ThreadLocal 原理？
 * A: 每个 Thread 对象内部有一张 ThreadLocalMap，key 是 ThreadLocal 实例（弱引用），
 *    value 是存的值。各线程只操作自己的 map，天然线程隔离，不需要加锁。
 *
 * Q: 为什么必须在拦截器 afterCompletion 里 clear()？
 * A: Tomcat 用线程池复用线程：
 *    ① 不清理 → 下一个请求复用该线程时可能读到上一个用户的登录态（数据串号，安全事故）；
 *    ② value 是强引用，key 弱引用被 GC 后 value 仍挂在 map 里 → 内存泄漏。
 *
 * Q: 为什么不用 InheritableThreadLocal 传给子线程？
 * A: 它只在"创建子线程那一刻"复制一次，线程池的核心线程早就创建好了，
 *    异步任务里拿到的是 null；要跨线程池传递需用阿里 TransmittableThreadLocal。
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    /** 拦截器解析 token 后调用，写入当前请求的登录用户 */
    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /** 获取当前登录用户（可能为 null，由调用方自行判空） */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 获取当前用户ID，未登录返回 null（如"我的问题列表"允许游客场景） */
    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    /** 获取当前用户ID，未登录直接抛 401 业务异常（Service 层强制登录场景用） */
    public static Long requireUserId() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user.getUserId();
    }

    /** 请求结束时清理，防止线程复用导致的数据串号和内存泄漏，必须在拦截器 afterCompletion 里调用 */
    public static void clear() {
        HOLDER.remove();
    }
}
