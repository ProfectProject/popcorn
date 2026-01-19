package com.popcorn.demo.chaos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.ResponseEntity;
import java.util.Map;

/**
 * 🐒 Chaos Monkey 단위 테스트
 *
 * 장애 시뮬레이션 기능이 정상적으로 작동하는지 검증
 */
class ChaosMonkeyIntegrationTest {

    private ChaosMonkeyController chaosMonkeyController;

    @BeforeEach
    void setUp() {
        chaosMonkeyController = new ChaosMonkeyController();
    }

    @Test
    void chaosMonkey_StatusEndpoint_ReturnsConfiguration() {
        // when
        ResponseEntity<Map<String, Object>> response = chaosMonkeyController.getChaosStatus();

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("enabled");
        assertThat(response.getBody()).containsKey("totalAttacks");
        assertThat(response.getBody()).containsKey("activeAttacks");
        assertThat(response.getBody()).containsKey("attackStats");

        System.out.println("🐒 Chaos Monkey 상태: " + response.getBody());
    }

    @Test
    void chaosMonkey_LatencyAttack_ExecutesSuccessfully() {
        // given
        int maxDelayMs = 5000; // Controller의 최소값 1000ms보다 큰 값으로 설정

        // when
        ResponseEntity<String> response = chaosMonkeyController.triggerLatencyAttack(maxDelayMs);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("지연 공격 완료");
        assertThat(response.getBody()).contains("ms 지연 주입됨");

        System.out.println("🐒 지연 공격 결과: " + response.getBody());
    }

    @Test
    void chaosMonkey_LatencyAttack_WithMinimumDelay() {
        // given - 최소 지연 시간 테스트
        Instant start = Instant.now();

        // when
        ResponseEntity<String> response = chaosMonkeyController.triggerLatencyAttack(2000);

        // then
        Instant end = Instant.now();
        Duration actualDelay = Duration.between(start, end);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
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
            ResponseEntity<String> response = chaosMonkeyController.triggerExceptionAttack("테스트예외");

            // 예외가 발생하지 않은 경우
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).contains("운이 좋았습니다");

            System.out.println("🐒 예외 공격 결과 (운이 좋음): " + response.getBody());

        } catch (RuntimeException e) {
            // 예외가 발생한 경우 (정상적인 Chaos Monkey 동작)
            assertThat(e.getMessage()).contains("테스트예외");
            System.out.println("🐒 예외 공격 성공! 예외 발생: " + e.getMessage());
        }
    }

    @Test
    void chaosMonkey_MemoryAttack_AllocatesAndReleasesMemory() {
        // given
        int sizeMB = 50;

        // when
        ResponseEntity<String> response = chaosMonkeyController.triggerMemoryAttack(sizeMB);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("메모리 공격 완료");
        assertThat(response.getBody()).contains(sizeMB + "MB");

        System.out.println("🐒 메모리 공격 결과: " + response.getBody());
    }

    @Test
    void chaosMonkey_ComboAttack_ExecutesMultipleAttacks() {
        // given
        Instant start = Instant.now();

        // when & then
        try {
            ResponseEntity<String> response = chaosMonkeyController.triggerComboAttack();

            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);

            // 복합 공격이므로 시간이 오래 걸림
            assertThat(duration.toMillis()).isGreaterThan(900);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).contains("복합 공격 결과");
            assertThat(response.getBody()).contains("지연:");
            assertThat(response.getBody()).contains("메모리:");

            System.out.println("🐒 복합 공격 결과: " + response.getBody());
            System.out.println("복합 공격 소요 시간: " + duration.toMillis() + "ms");

        } catch (RuntimeException e) {
            // 복합 공격에서 예외가 발생한 경우 (30% 확률)
            assertThat(e.getMessage()).contains("복합 공격 중 예외 발생");
            System.out.println("🐒 복합 공격에서 예외 발생 (정상): " + e.getMessage());
        }
    }

    @Test
    void chaosMonkey_StopAllAttacks_DisablesAllAttacks() {
        // when
        ResponseEntity<String> response = chaosMonkeyController.stopAllAttacks();

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("모든 Chaos Monkey 공격이 중지");

        System.out.println("🐒 공격 중지 결과: " + response.getBody());

        // 상태 확인
        ResponseEntity<Map<String, Object>> statusResponse = chaosMonkeyController.getChaosStatus();
        assertThat(statusResponse.getStatusCode().value()).isEqualTo(200);
        assertThat((Boolean) statusResponse.getBody().get("enabled")).isFalse();
    }

    @Test
    void chaosMonkey_ExtremeMode_ActivatesAggressiveSettings() {
        // when
        ResponseEntity<String> response = chaosMonkeyController.activateExtremeMode();

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("EXTREME MODE 활성화");

        System.out.println("🔥 EXTREME MODE 활성화: " + response.getBody());

        // 극한 모드 상태 확인
        ResponseEntity<Map<String, Object>> statusResponse = chaosMonkeyController.getChaosStatus();
        assertThat((Boolean) statusResponse.getBody().get("enabled")).isTrue();
        assertThat((Boolean) statusResponse.getBody().get("extremeMode")).isTrue();
    }

    @Test
    void chaosMonkey_GetStats_ReturnsStatistics() {
        // when
        ResponseEntity<Map<String, Object>> response = chaosMonkeyController.getChaosStats();

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsKey("totalAttacks");
        assertThat(response.getBody()).containsKey("systemImpact");

        System.out.println("📊 Chaos Monkey 통계: " + response.getBody());
    }

    @Test
    void chaosMonkey_BlackFridayScenario_ExecutesSuccessfully() {
        // given
        Instant start = Instant.now();

        // when & then
        try {
            ResponseEntity<String> response = chaosMonkeyController.executeScenario("blackfriday");

            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);

            // Black Friday 시나리오는 시간이 오래 걸림
            assertThat(duration.toMillis()).isGreaterThan(2500);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).contains("Black Friday");

            System.out.println("🛍️ Black Friday 시나리오 결과: " + response.getBody());
            System.out.println("시나리오 소요 시간: " + duration.toMillis() + "ms");

        } catch (RuntimeException e) {
            // Black Friday 시나리오에서 예외 발생 (20% 확률)
            assertThat(e.getMessage()).contains("Black Friday 트래픽 과부하");
            System.out.println("🛍️ Black Friday 트래픽 과부하 발생 (정상): " + e.getMessage());
        }
    }

    @Test
    void chaosMonkey_UnknownScenario_ReturnsBadRequest() {
        // when
        ResponseEntity<String> response = chaosMonkeyController.executeScenario("unknown-scenario");

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("알 수 없는 시나리오");

        System.out.println("🐒 알 수 없는 시나리오 처리: " + response.getBody());
    }

    @Test
    void chaosMonkey_PaymentFailureScenario_ExecutesCorrectly() {
        // when & then
        try {
            ResponseEntity<String> response = chaosMonkeyController.executeScenario("payment-failure");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).contains("결제 시스템 정상 작동");

            System.out.println("💳 결제 시나리오 성공: " + response.getBody());

        } catch (RuntimeException e) {
            // 결제 실패 시나리오에서 예외 발생 (40% 확률)
            assertThat(e.getMessage()).contains("결제 시스템 장애 발생");
            System.out.println("💳 결제 시스템 장애 발생 (정상): " + e.getMessage());
        }
    }

    @Test
    void chaosMonkey_DatabaseOutageScenario_HandlesTimeout() {
        // given
        Instant start = Instant.now();

        // when & then
        try {
            ResponseEntity<String> response = chaosMonkeyController.executeScenario("database-outage");

            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);

            // DB 장애는 긴 시간이 걸림
            assertThat(duration.toMillis()).isGreaterThan(4000);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).contains("데이터베이스 장애 복구 완료");

            System.out.println("💾 DB 복구 완료: " + response.getBody());

        } catch (RuntimeException e) {
            // DB 연결 실패 (60% 확률)
            assertThat(e.getMessage()).contains("데이터베이스 연결 실패");
            System.out.println("💾 DB 연결 실패 (정상): " + e.getMessage());
        }
    }

}