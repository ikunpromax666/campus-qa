package com.campus.campusqaweb.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI campusQaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("校园问答 API")
                        .description("校园问答平台后端接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SuperXia")
                                .email("xxx@xxx.com")));
    }
}