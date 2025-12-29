package com.ttalkak.demo.application.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Application Layer DI 설정
 */
@Configuration
@ComponentScan(basePackages = {
    "com.ttalkak.demo.application.usecase",
    "com.ttalkak.demo.application.service"
})
public class ApplicationConfig {
    // Use Case와 Application Service Bean 스캔
}