package com.campus.campusqaweb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * @MapperScan 扫描的是 类路径上 的 Mapper 接口，Spring Boot 的组件扫描从启动类所在包开始。
 * 启动类在`com.campus.campusqaweb` ，加上`scanBasePackages = "com.campus"`
 * */
@MapperScan("com.campus.campusqamapper.mapper")
//scanBasePackages = "com.campus" 是刻意加的：
// 因为各层包名不同（campusqaservice、campusqamapper…），
// 只写 @SpringBootApplication 默认只会扫描 web 自己的包，将来扫不到 service 的 Bean
@SpringBootApplication(scanBasePackages = "com.campus")
public class CampusQaWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusQaWebApplication.class, args);
    }

}