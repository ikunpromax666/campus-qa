package com.campus.campusqaweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


//scanBasePackages = "com.campus" 是刻意加的：
// 因为各层包名不同（campusqaservice、campusqamapper…），
// 只写 @SpringBootApplication 默认只会扫描 web 自己的包，将来扫不到 service 的 Bean
@SpringBootApplication(scanBasePackages = "com.campus")
public class CampusQaWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusQaWebApplication.class, args);
    }

}