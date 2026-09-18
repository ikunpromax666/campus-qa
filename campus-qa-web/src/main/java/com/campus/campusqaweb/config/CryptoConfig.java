package com.campus.campusqaweb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * ClassName: CrytoConfig
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/17 16:13
 * Version:1.0
 */
@Configuration
public class CryptoConfig {
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}