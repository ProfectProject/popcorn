package com.popcorn.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.popcorn.demo.config.TestRedisConfig;
import com.popcorn.demo.config.TestSecurityConfig;

@SpringBootTest
@Import({TestSecurityConfig.class, TestRedisConfig.class})
@ActiveProfiles("test")
@DisplayName("🎬 PopCorn 데모 애플리케이션 테스트")
class DemoApplicationTests {

    @Test
    @DisplayName("애플리케이션 컨텍스트 로딩 테스트")
    void contextLoads() {
        // Spring Boot 애플리케이션이 정상적으로 로딩되는지 확인하는 기본 테스트
        // Spring Boot Context가 정상 시작되면 테스트 성공
    }
}
