package com.campus.campusqaweb.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /** JWT 安全方案名，Swagger UI 里显示在 Authorize 弹框里 */
    private static final String SECURITY_SCHEME_NAME = "Authorization";

    @Bean
    public OpenAPI campusQaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("校园问答 API")
                        .description("校园问答平台后端接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SuperXia")
                                .email("xxx@xxx.com")))
                // 定义全局安全方案：Bearer Token（JWT）
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .schemaRequirement(SECURITY_SCHEME_NAME, new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));
    }
}