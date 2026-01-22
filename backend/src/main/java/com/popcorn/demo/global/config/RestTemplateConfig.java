package com.popcorn.demo.global.config;

import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 설정
 *
 * 마이크로서비스 간 HTTP 통신을 위한 RestTemplate 빈 설정
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .requestFactory(HttpComponentsClientHttpRequestFactory.class)
            .connectTimeout(Duration.ofSeconds(5))    // 연결 타임아웃 5초
            .readTimeout(Duration.ofSeconds(10))      // 읽기 타임아웃 10초
            .build();
    }

}