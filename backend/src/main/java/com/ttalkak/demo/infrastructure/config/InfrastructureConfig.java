package com.ttalkak.demo.infrastructure.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Infrastructure Layer DI 설정
 */
@Configuration
@ComponentScan(basePackages = {
    "com.ttalkak.demo.infrastructure.persistence",
    "com.ttalkak.demo.infrastructure.external"
})
@EnableJpaRepositories(basePackages = "com.ttalkak.demo.infrastructure.persistence.repository")
public class InfrastructureConfig {
    // 필요시 추가 Bean 설정
}