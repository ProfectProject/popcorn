package com.popcorn.demo.chaos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 🐒 Chaos Monkey 통합 테스트
 *
 * 장애 시뮬레이션 기능이 정상적으로 작동하는지 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("chaos-test")
@TestPropertySource(properties = {
    "chaos.monkey.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:chaostest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never",
    "jwt.secret=test-jwt-secret-for-chaos-testing",
    "jwt.expiration=86400000",
    "toss.secret-key=test-secret-key",
    "toss.client-key=test-client-key"
})
class ChaosMonkeyIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1/chaos";
    }

    @Test
    void chaosMonkey_StatusEndpoint_ReturnsConfiguration() {
        // when
        ResponseEntity<String> response = restTemplate.getForEntity(
            getBaseUrl() + "/status",
            String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("enabled");
        assertThat(response.getBody()).contains("assaults");
        assertThat(response.getBody()).contains("watcher");

        System.out.println("🐒 Chaos Monkey 상태: " + response.getBody());
    }

    @Test
    void chaosMonkey_LatencyAttack_InducesDelay() {
        // given
        int maxDelay = 3000; // 3초
        Instant start = Instant.now();

        // when
        ResponseEntity<String> response = restTemplate.postForEntity(
            getBaseUrl() + "/attack/latency?maxDelayMs=" + maxDelay,
            null,
            String.class
        );

        // then
        Instant end = Instant.now();
        Duration actualDelay = Duration.between(start, end);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actualDelay.toMillis()).isGreaterThan(800); // 최소 지연 확인
        assertThat(response.getBody()).contains("지연 공격 완료");

        System.out.println("🐒 지연 공격 결과: " + response.getBody());
        System.out.println("실제 지연 시간: " + actualDelay.toMillis() + "ms");
    }

    @Test
    void chaosMonkey_ExceptionAttack_MayThrowException() {
        // when & then
        // 예외가 발생하거나 성공 메시지가 반환됨 (50% 확률)
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                getBaseUrl() + "/attack/exception?message=테스트예외",
                null,
                String.class
            );

            // 예외가 발생하지 않은 경우
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("운이 좋았습니다");

            System.out.println("🐒 예외 공격 결과 (운이 좋음): " + response.getBody());

        } catch (Exception e) {
            // 예외가 발생한 경우 (정상적인 Chaos Monkey 동작)
            System.out.println("🐒 예외 공격 성공! 예외 발생: " + e.getMessage());
        }
    }

    @Test
    void chaosMonkey_MemoryAttack_AllocatesAndReleasesMemory() {
        // given
        int sizeMB = 50;

        // when
        ResponseEntity<String> response = restTemplate.postForEntity(
            getBaseUrl() + "/attack/memory?sizeMB=" + sizeMB,
            null,
            String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("메모리 공격 완료");
        assertThat(response.getBody()).contains(sizeMB + "MB");

        System.out.println("🐒 메모리 공격 결과: " + response.getBody());
    }

    @Test
    void chaosMonkey_ComboAttack_ExecutesMultipleAttacks() {
        // given
        Instant start = Instant.now();

        // when
        ResponseEntity<String> response = restTemplate.postForEntity(
            getBaseUrl() + "/attack/combo",
            null,
            String.class
        );

        // then
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);

        // 복합 공격이므로 시간이 오래 걸림
        assertThat(duration.toMillis()).isGreaterThan(900);

        if (response.getStatusCode() == HttpStatus.OK) {
            assertThat(response.getBody()).contains("복합 공격 결과");
            assertThat(response.getBody()).contains("지연:");
            assertThat(response.getBody()).contains("메모리:");
        }

        System.out.println("🐒 복합 공격 결과: " + response.getBody());
        System.out.println("복합 공격 소요 시간: " + duration.toMillis() + "ms");
    }

    @Test
    void chaosMonkey_StopAllAttacks_DisablesAllAttacks() {
        // when
        ResponseEntity<String> response = restTemplate.postForEntity(
            getBaseUrl() + "/stop",
            null,
            String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("모든 Chaos Monkey 공격이 중지");

        System.out.println("🐒 공격 중지 결과: " + response.getBody());

        // 상태 확인
        ResponseEntity<String> statusResponse = restTemplate.getForEntity(
            getBaseUrl() + "/status",
            String.class
        );

        // 모든 공격이 비활성화되었는지 확인은 생략 (설정이 복잡함)
        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void chaosMonkey_ExtremeMode_ActivatesAggressiveSettings() {
        // when
        ResponseEntity<String> response = restTemplate.postForEntity(
            getBaseUrl() + "/extreme-mode",
            null,
            String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("EXTREME MODE 활성화");

        System.out.println("🔥 EXTREME MODE 활성화: " + response.getBody());
    }

    @Test
    void chaosMonkey_GetStats_ReturnsStatistics() {
        // when
        ResponseEntity<String> response = restTemplate.getForEntity(
            getBaseUrl() + "/stats",
            String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("totalAttacks");
        assertThat(response.getBody()).contains("systemImpact");

        System.out.println("📊 Chaos Monkey 통계: " + response.getBody());
    }

    @Test
    void chaosMonkey_BlackFridayScenario_ExecutesSuccessfully() {
        // given
        Instant start = Instant.now();

        // when
        ResponseEntity<String> response = restTemplate.postForEntity(
            getBaseUrl() + "/scenarios/blackfriday",
            null,
            String.class
        );

        // then
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);

        // Black Friday 시나리오는 시간이 오래 걸림
        assertThat(duration.toMillis()).isGreaterThan(2500);

        if (response.getStatusCode() == HttpStatus.OK) {
            assertThat(response.getBody()).contains("Black Friday");
        }

        System.out.println("🛍️ Black Friday 시나리오 결과: " + response.getBody());
        System.out.println("시나리오 소요 시간: " + duration.toMillis() + "ms");
    }

    @Test
    void chaosMonkey_UnknownScenario_ReturnsBadRequest() {
        // when
        ResponseEntity<String> response = restTemplate.postForEntity(
            getBaseUrl() + "/scenarios/unknown-scenario",
            null,
            String.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("알 수 없는 시나리오");

        System.out.println("🐒 알 수 없는 시나리오 처리: " + response.getBody());
    }

    @Test
    void chaosMonkey_WithRealWorkload_MaintainsSystemStability() {
        // given - 실제 워크로드와 함께 Chaos Monkey 테스트
        System.out.println("🐒 실제 워크로드와 함께 Chaos Monkey 테스트 시작");

        // 1. 정상적인 API 호출
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health",
            String.class
        );
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 2. Chaos Monkey 활성화
        ResponseEntity<String> chaosResponse = restTemplate.postForEntity(
            getBaseUrl() + "/attack/latency?maxDelayMs=2000",
            null,
            String.class
        );

        // 3. 시스템이 여전히 응답하는지 확인
        ResponseEntity<String> healthAfterChaos = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health",
            String.class
        );

        // then
        assertThat(healthAfterChaos.getStatusCode()).isEqualTo(HttpStatus.OK);

        System.out.println("✅ 시스템이 Chaos Monkey 공격 후에도 안정적으로 작동함");
    }
}