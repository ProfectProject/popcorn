package com.popcorn.demo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.popcorn.demo.domain.auth.jwt.JwtUtil;

/**
 * 테스트용 최소 구성
 */
@TestConfiguration
public class TestConfig {

    /**
     * 테스트용 JwtUtil 빈
     */
    @Bean
    @Primary
    public JwtUtil testJwtUtil() {
        return new JwtUtil("dGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItdGVzdC1lbnZpcm9ubWVudC1vbmx5");
    }
}