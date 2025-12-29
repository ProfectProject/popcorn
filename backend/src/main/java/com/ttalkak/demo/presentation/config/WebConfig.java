package com.ttalkak.demo.presentation.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Presentation Layer DI 설정
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
    "com.ttalkak.demo.presentation.web.controller"
})
public class WebConfig implements WebMvcConfigurer {
    // Web Layer 관련 Bean 스캔 및 설정
}