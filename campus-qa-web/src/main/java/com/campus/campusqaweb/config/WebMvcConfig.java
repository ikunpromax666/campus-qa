package com.campus.campusqaweb.config;

import com.campus.campusqaweb.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ClassName: WebMvcConfig
 * Description: Web MVC 拦截器注册
 *
 * 面试高频问题：
 * Q: 为什么用 WebMvcConfigurer 而不是继承 WebMvcConfigurationSupport？
 * A: WebMvcConfigurationSupport 是 @Import 方式全接管 MVC 自动配置，
 *    一继承就丢了 spring-boot-starter-web 自带的 HttpMessageConverter 等默认配置；
 *    WebMvcConfigurer 是接口回调，只添加自己的逻辑，不影响 Boot 默认配置——
 *    这是 Spring Boot 推荐做法。
 *
 * Q: addPathPatterns 和 excludePathPatterns 都写才完整，只写 addPathPatterns 不加 exclude，
 *    会不会把 Swagger UI 也拦了？
 * A: 会！拦截器拦截"所有请求"，Swagger 静态资源和 API 文档请求都会被 401。
 *    Swagger 放行路径必须显式写在 excludePathPatterns 里。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    public WebMvcConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")          // 拦截所有请求
                .excludePathPatterns(            // 放行无需登录的接口
                        "/user/register",
                        "/user/login",
                        // Swagger UI 路径（springdoc 2.x）
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        // 根路径（可选，防止直接访问 404）
                        "/"
                );
    }
}
