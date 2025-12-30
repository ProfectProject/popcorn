package com.popcorn.demo.infrastructure.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Infrastructure Layer DI 설정
 */
@Configuration
@ComponentScan(basePackages = {
    "com.popcorn.demo.infrastructure.persistence",
    "com.popcorn.demo.infrastructure.external"
})
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.popcorn.demo.infrastructure.persistence.repository")
public class InfrastructureConfig {
    // 필요시 추가 Bean 설정
}
