package com.popcorn.demo;

import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.popcorn.demo.config.TestRedisConfig;
import com.popcorn.demo.config.TestSecurityConfig;

/**
 * 팝업 도메인 전용 통합 테스트
 *
 * 팝업 관련 컴포넌트들의 컨텍스트 로딩을 검증합니다.
 */

@SpringBootTest
@Import({TestSecurityConfig.class, TestRedisConfig.class})
@ActiveProfiles("test")
public class PopupApplicationTests {

    @Test
    void contextLoads() {
        // 팝업 도메인이 정상적으로 로딩되는지 확인하는 기본 테스트
        // Spring Boot Context가 정상 시작되면 테스트 성공
    }

}
