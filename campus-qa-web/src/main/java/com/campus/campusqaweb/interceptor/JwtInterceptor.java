package com.campus.campusqaweb.interceptor;

import com.campus.campusqacommon.annotation.RequireAdmin;
import com.campus.campusqacommon.annotation.RequireLogin;
import com.campus.campusqacommon.context.LoginUser;
import com.campus.campusqacommon.context.UserContext;
import com.campus.campusqacommon.exception.BusinessException;
import com.campus.campusqacommon.result.ResultCode;
import com.campus.campusqacommon.utils.JwtUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

/**
 * ClassName: JwtInterceptor
 * Description: JWT 登录拦截器
 *
 * 工作链路：请求进来 → preHandle 解析 token 写入 UserContext → Controller 执行业务
 *         → afterCompletion 清理 UserContext（防止线程复用串号 + ThreadLocal 内存泄漏）
 *
 * 注解匹配规则：method 先查 → 没查到再查 declaring class（类上标注解 = 整个类生效）
 * 拦截顺序：@RequireAdmin 隐含 @RequireLogin —— 先验登录、再验 role == 1
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    /** 请求头名：客户端把 token 放在 Authorization 里 */
    private static final String AUTH_HEADER = "Authorization";
    /** token 前缀："Bearer xxx" */
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;

    public JwtInterceptor(JwtUtils jwtUtils) {

        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只拦 Controller 方法，放行静态资源（handler 是 ResourceHttpRequestHandler）
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }

        Method method = hm.getMethod();
        Class<?> declaringClass = hm.getBeanType();

        // 1. 目标方法/类上既没有 @RequireLogin 也没有 @RequireAdmin → 无需鉴权，直接放行
        boolean hasRequireLogin = method.isAnnotationPresent(RequireLogin.class)
                || declaringClass.isAnnotationPresent(RequireLogin.class);
        boolean hasRequireAdmin = method.isAnnotationPresent(RequireAdmin.class)
                || declaringClass.isAnnotationPresent(RequireAdmin.class);
        if (!hasRequireLogin && !hasRequireAdmin) {
            return true;
        }

        // 2. 从 Authorization 请求头取 token，格式 "Bearer xxx"
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String token = header.substring(TOKEN_PREFIX.length());
        if (token.isEmpty()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 3. 解析 token（过期/篡改/格式错误都会抛 JwtException）
        LoginUser loginUser;
        try {
            loginUser = jwtUtils.parseToken(token);
        } catch (JwtException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 4. 写入 ThreadLocal，供 Service 层通过 UserContext.getUserId() 读取
        UserContext.set(loginUser);

        // 5. 如果是 @RequireAdmin，再验一次管理员权限（role == 1）
        if (hasRequireAdmin && loginUser.getRole() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 无论请求成功还是异常，都必须清理 ThreadLocal
        // Tomcat 用线程池复用线程，不清理 → 下一个请求读到上一个用户的登录态（安全事故）
        UserContext.clear();
    }
}
