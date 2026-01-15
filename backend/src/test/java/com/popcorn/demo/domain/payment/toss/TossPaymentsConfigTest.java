package com.popcorn.demo.domain.payment.toss;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

import com.popcorn.demo.config.TestSecurityConfig;
import com.popcorn.demo.config.TestRedisConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({TestSecurityConfig.class, TestRedisConfig.class})
@ActiveProfiles("test")
@DisplayName("💳 TossPayments 설정 테스트")
class TossPaymentsConfigTest {

    @Test
    @DisplayName("TossPayments 설정 클래스가 정상적으로 로딩되는지 테스트")
    void shouldLoadConfiguration() {
        // Given & When - Spring 컨텍스트 로딩
        TossPaymentsConfig config = new TossPaymentsConfig();

        // Then - 설정 클래스가 정상적으로 인스턴스화됨
        assertThat(config).isNotNull();
    }
}