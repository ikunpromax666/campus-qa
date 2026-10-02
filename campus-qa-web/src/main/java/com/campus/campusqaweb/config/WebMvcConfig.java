package com.campus.campusqaweb.config;

import com.campus.campusqaweb.interceptor.JwtInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
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
                        "/category/list",        // 分类列表：未登录也要能看筛选/下拉
                        "/tag/list",             // 标签列表：发布问题下拉（发布本身需登录，列表可放行）
                        // Swagger UI 路径（springdoc 2.x）
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        // 根路径（可选，防止直接访问 404）
                        "/"
                );
    }

    /**
     * CORS 跨域配置（Filter 方式）
     *
     * 面试高频问题：
     * Q: 为什么用 CorsFilter 而不是 WebMvcConfigurer.addCorsMappings？
     * A: 执行时机不同。Filter 在 DispatcherServlet【之前】执行，
     *    拦截器在 DispatcherServlet【之后】执行。
     *    浏览器跨域前会先发 OPTIONS 预检请求（preflight），
     *    如果用 addCorsMappings，预检请求会先经过 JwtInterceptor——
     *    预检请求不带 Authorization 头，直接被拦截器返回 401，
     *    浏览器认为"预检失败"，真正的请求根本发不出去。
     *    用 CorsFilter 注册成最高优先级 Servlet Filter，
     *    预检请求在进入拦截器链之前就被正确响应，问题从根上解决。
     *
     * Q: 为什么 allowedOriginPatterns 而不是 allowedOrigins("*")？
     * A: 当 allowCredentials(true) 时，Spring 禁止 allowedOrigins("*")
     *    （规范要求：允许携带凭证时，Origin 必须明确，防止 CSRF）。
     *    allowedOriginPatterns 是合法替代，支持通配符且符合规范。
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(java.util.List.of("http://localhost:5173")); // 前端 dev 地址
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setAllowCredentials(true); // 允许携带 cookie/凭证
        config.setMaxAge(3600L);          // 预检结果缓存 1 小时，减少 OPTIONS 请求

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE); // 最高优先级，确保在 JwtInterceptor 之前
        return bean;
    }
}
