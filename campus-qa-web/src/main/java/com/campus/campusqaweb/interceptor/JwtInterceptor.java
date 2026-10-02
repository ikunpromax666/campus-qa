package com.campus.campusqaweb.interceptor;

import com.campus.campusqacommon.annotation.RequireAdmin;
import com.campus.campusqacommon.annotation.RequireLogin;
import com.campus.campusqacommon.context.LoginUser;
import com.campus.campusqacommon.context.UserContext;
import com.campus.campusqacommon.exception.BusinessException;
import com.campus.campusqacommon.result.ResultCode;
import com.campus.campusqacommon.utils.JwtUtils;
import com.campus.campusqaservice.service.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 * 拦截顺序：@RequireAdmin 隐含 @RequireLogin —— 先验 token、再验账号状态、最后验 role == 1
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    /** 请求头名：客户端把 token 放在 Authorization 里 */
    private static final String AUTH_HEADER = "Authorization";
    /** token 前缀："Bearer xxx" */
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;

    /**
     * 拦截器依赖 UserService 来校验账号状态。
     * web 依赖 service 是正确方向（controller 也是这么依赖的），
     * 【不要】为了省事直接注入 UserMapper：那是跨层依赖，Mapper 只属于 service 层。
     */
    private final UserService userService;

    public JwtInterceptor(JwtUtils jwtUtils, UserService userService) {
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只拦 Controller 方法，放行静态资源（handler 是 ResourceHttpRequestHandler）
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }

        Method method = hm.getMethod();
        Class<?> declaringClass = hm.getBeanType();

        // 1. 先判断目标是否需要鉴权
        boolean hasRequireLogin = method.isAnnotationPresent(RequireLogin.class)
                || declaringClass.isAnnotationPresent(RequireLogin.class);
        boolean hasRequireAdmin = method.isAnnotationPresent(RequireAdmin.class)
                || declaringClass.isAnnotationPresent(RequireAdmin.class);

        // 2. 【可选认证】尽力解析 token 写入 UserContext —— 即使接口不需要登录。
        //    场景：问题详情页游客可看，但登录用户要看 isOwner / likeStatus / isFavorited，
        //    这些字段依赖 UserContext；如果"无注解直接放行"，登录用户访问公开接口
        //    Service 层拿到的 currentUserId 永远是 null（踩过的 bug）。
        String header = request.getHeader(AUTH_HEADER);
        boolean tokenPresent = header != null
                && header.startsWith(TOKEN_PREFIX)
                && header.length() > TOKEN_PREFIX.length();
        if (tokenPresent) {
            try {
                UserContext.set(jwtUtils.parseToken(header.substring(TOKEN_PREFIX.length())));
            } catch (JwtException e) {
                // token 无效（过期/篡改/格式错误）：
                // 受保护接口 → 必须 401，不能把坏 token 当游客放过去
                if (hasRequireLogin || hasRequireAdmin) {
                    throw new BusinessException(ResultCode.UNAUTHORIZED);
                }
                // 公开接口 → 静默降级为游客，不影响浏览体验
            }
        }

        // 3. 公开接口：无需登录，放行（UserContext 可能已填充，供 isOwner 等字段使用）
        if (!hasRequireLogin && !hasRequireAdmin) {
            return true;
        }

        // 4. 受保护接口：必须有有效登录态（token 缺失/格式错误/上一步解析失败都到不了这里）
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 5. 校验账号状态：JWT 是无状态的，载荷里只有 userId / role，没有 status ——
        //    管理员把账号禁用后，用户手上那个还没过期的 token 照样能通过解析。
        //    所以每请求补一次状态查询，禁用才能"立即生效"（内部走 Redis 缓存，正常情况只有一次 GET）
        if (!userService.isUserEnabled(loginUser.getUserId())) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 6. 如果是 @RequireAdmin，再验一次管理员权限（role == 1）
        //    必须写成 Integer.valueOf(1).equals(...) 而不是 loginUser.getRole() != 1：
        //    != 会触发自动拆箱，而包装类型为 null 时拆箱直接 NPE
        //    （旧版本签发的 token 里没有 role 字段，就会走到这条路径）
        if (hasRequireAdmin && !Integer.valueOf(1).equals(loginUser.getRole())) {
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
