package com.popcorn.demo.chaos;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🐒 Chaos Monkey 제어 API
 *
 * 장애 시뮬레이션을 실시간으로 제어하고 모니터링하는 REST API
 *
 * 주요 기능:
 * - 실시간 장애 주입 제어
 * - 공격 유형별 활성화/비활성화
 * - 장애 통계 조회
 * - 극한 테스트 모드 지원
 */
@RestController
@RequestMapping("/api/v1/chaos")
@Slf4j
public class ChaosMonkeyController {

    // 🐒 Chaos Monkey 상태 관리
    private final AtomicBoolean chaosEnabled = new AtomicBoolean(false);
    private final AtomicBoolean extremeMode = new AtomicBoolean(false);
    private final AtomicLong totalAttacks = new AtomicLong(0);
    private final Set<String> activeAttacks = Collections.synchronizedSet(new HashSet<>());

    // 📊 메트릭 수집
    private final Map<String, Long> attackStats = new HashMap<>();

    /**
     * 🎯 현재 Chaos Monkey 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getChaosStatus() {
        return ResponseEntity.ok(Map.of(
            "enabled", chaosEnabled.get(),
            "extremeMode", extremeMode.get(),
            "totalAttacks", totalAttacks.get(),
            "activeAttacks", new ArrayList<>(activeAttacks),
            "attackStats", new HashMap<>(attackStats)
        ));
    }

    /**
     * 🔥 즉시 지연 공격 실행
     */
    @PostMapping("/attack/latency")
    public ResponseEntity<String> triggerLatencyAttack(@RequestParam(defaultValue = "5000") int maxDelayMs) {
        try {
            int delay = ThreadLocalRandom.current().nextInt(1000, maxDelayMs + 1);
            log.warn("🐒 Chaos Monkey 지연 공격! {}ms 지연 주입", delay);

            activeAttacks.add("latency");
            totalAttacks.incrementAndGet();
            attackStats.merge("latency", 1L, Long::sum);

            Thread.sleep(delay);

            activeAttacks.remove("latency");
            return ResponseEntity.ok(String.format("🐒 지연 공격 완료! %dms 지연 주입됨", delay));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            activeAttacks.remove("latency");
            return ResponseEntity.internalServerError()
                .body("🐒 지연 공격 중 인터럽트 발생: " + e.getMessage());
        }
    }

    /**
     * 💥 즉시 예외 공격 실행
     */
    @PostMapping("/attack/exception")
    public ResponseEntity<String> triggerExceptionAttack(@RequestParam(defaultValue = "Chaos Monkey Strike!") String message) {
        log.warn("🐒 Chaos Monkey 예외 공격! 메시지: {}", message);

        activeAttacks.add("exception");
        totalAttacks.incrementAndGet();
        attackStats.merge("exception", 1L, Long::sum);

        // 50% 확률로 예외 발생
        if (ThreadLocalRandom.current().nextBoolean()) {
            activeAttacks.remove("exception");
            throw new RuntimeException("🐒 " + message);
        }

        activeAttacks.remove("exception");
        return ResponseEntity.ok("🐒 예외 공격 시도했지만 운이 좋았습니다! (50% 확률)");
    }

    /**
     * 💾 메모리 압박 공격 실행
     */
    @PostMapping("/attack/memory")
    public ResponseEntity<String> triggerMemoryAttack(@RequestParam(defaultValue = "100") int sizeMB) {
        log.warn("🐒 Chaos Monkey 메모리 공격! {}MB 할당 시도", sizeMB);

        try {
            activeAttacks.add("memory");
            totalAttacks.incrementAndGet();
            attackStats.merge("memory", 1L, Long::sum);

            // 메모리 할당 (주의: OutOfMemoryError 위험)
            List<byte[]> memoryConsumer = new ArrayList<>();
            for (int i = 0; i < sizeMB; i++) {
                memoryConsumer.add(new byte[1024 * 1024]); // 1MB씩 할당
            }

            // 잠시 메모리 점유
            Thread.sleep(5000);

            // 명시적으로 null 처리하여 GC 유도
            memoryConsumer.clear();
            System.gc();

            activeAttacks.remove("memory");
            return ResponseEntity.ok(String.format("🐒 메모리 공격 완료! %dMB 할당 후 해제", sizeMB));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            activeAttacks.remove("memory");
            return ResponseEntity.internalServerError()
                .body("🐒 메모리 공격 중 인터럽트 발생: " + e.getMessage());
        } catch (OutOfMemoryError e) {
            System.gc(); // 긴급 GC
            activeAttacks.remove("memory");
            return ResponseEntity.internalServerError()
                .body("🐒 메모리 공격으로 OOM 발생! " + e.getMessage());
        }
    }

    /**
     * 🌪️ 복합 공격 실행 (지연 + 예외 + 메모리)
     */
    @PostMapping("/attack/combo")
    public ResponseEntity<String> triggerComboAttack() {
        log.warn("🐒🔥 Chaos Monkey 복합 공격 시작!");

        activeAttacks.add("combo");
        totalAttacks.incrementAndGet();
        attackStats.merge("combo", 1L, Long::sum);

        StringBuilder result = new StringBuilder("🐒 복합 공격 결과:\n");

        try {
            // 1. 지연 공격
            int delay = ThreadLocalRandom.current().nextInt(1000, 3000);
            Thread.sleep(delay);
            result.append(String.format("✅ 지연: %dms\n", delay));

            // 2. 메모리 압박
            List<byte[]> memAttack = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                memAttack.add(new byte[1024 * 1024]); // 1MB씩 50개 = 50MB
            }
            result.append("✅ 메모리: 50MB 할당\n");

            // 3. 예외 (30% 확률)
            if (ThreadLocalRandom.current().nextInt(100) < 30) {
                activeAttacks.remove("combo");
                throw new RuntimeException("복합 공격 중 예외 발생!");
            } else {
                result.append("✅ 예외: 운이 좋음 (70% 확률)\n");
            }

            // 메모리 해제
            memAttack.clear();
            System.gc();

            activeAttacks.remove("combo");
            return ResponseEntity.ok(result.toString());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            activeAttacks.remove("combo");
            return ResponseEntity.internalServerError()
                .body("🐒 복합 공격 중 인터럽트: " + e.getMessage());
        }
    }

    /**
     * 🚀 극한 모드 활성화
     */
    @PostMapping("/extreme-mode")
    public ResponseEntity<String> activateExtremeMode() {
        extremeMode.set(true);
        chaosEnabled.set(true);

        log.warn("🔥🐒 EXTREME MODE 활성화! 매우 공격적인 장애 주입 시작!");
        return ResponseEntity.ok("🔥 EXTREME MODE 활성화! 시스템 복원력 극한 테스트 시작!");
    }

    /**
     * 🛑 모든 공격 중지
     */
    @PostMapping("/stop")
    public ResponseEntity<String> stopAllAttacks() {
        chaosEnabled.set(false);
        extremeMode.set(false);
        activeAttacks.clear();

        log.info("🐒 Chaos Monkey 모든 공격 중지");
        return ResponseEntity.ok("🛑 모든 Chaos Monkey 공격이 중지되었습니다.");
    }

    /**
     * 📊 공격 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getChaosStats() {
        return ResponseEntity.ok(Map.of(
            "totalAttacks", totalAttacks.get(),
            "attacksByType", new HashMap<>(attackStats),
            "currentlyActive", new ArrayList<>(activeAttacks),
            "systemImpact", Map.of(
                "avgResponseTime", ThreadLocalRandom.current().nextInt(100, 2000),
                "errorRate", ThreadLocalRandom.current().nextDouble(0.01, 0.15),
                "memoryUsage", ThreadLocalRandom.current().nextDouble(0.5, 0.9)
            )
        ));
    }

    /**
     * 🎭 커스텀 장애 시나리오 실행
     */
    @PostMapping("/scenarios/{scenarioName}")
    public ResponseEntity<String> executeScenario(@PathVariable String scenarioName) {
        totalAttacks.incrementAndGet();
        attackStats.merge("scenario_" + scenarioName, 1L, Long::sum);

        switch (scenarioName.toLowerCase()) {
            case "blackfriday":
                return executeBlackFridayScenario();
            case "database-outage":
                return executeDatabaseOutageScenario();
            case "payment-failure":
                return executePaymentFailureScenario();
            case "network-partition":
                return executeNetworkPartitionScenario();
            default:
                return ResponseEntity.badRequest()
                    .body("🐒 알 수 없는 시나리오: " + scenarioName);
        }
    }

    private ResponseEntity<String> executeBlackFridayScenario() {
        log.warn("🐒🛍️ Black Friday 시나리오 실행: 극한 트래픽 상황 시뮬레이션");

        activeAttacks.add("blackfriday");

        try {
            // 높은 지연 + 메모리 압박
            Thread.sleep(ThreadLocalRandom.current().nextInt(3000, 8000));
            List<byte[]> memPressure = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                memPressure.add(new byte[1024 * 1024]); // 100MB
            }

            if (ThreadLocalRandom.current().nextInt(100) < 20) { // 20% 확률
                activeAttacks.remove("blackfriday");
                throw new RuntimeException("🛍️ Black Friday 트래픽 과부하!");
            }

            memPressure.clear();
            activeAttacks.remove("blackfriday");
            return ResponseEntity.ok("🛍️ Black Friday 시나리오 완료 - 시스템이 버텨냈습니다!");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            activeAttacks.remove("blackfriday");
            return ResponseEntity.internalServerError().body("Black Friday 시나리오 중단");
        }
    }

    private ResponseEntity<String> executeDatabaseOutageScenario() {
        log.warn("🐒💾 Database Outage 시나리오 실행");

        activeAttacks.add("db-outage");

        // DB 타임아웃 시뮬레이션
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(5000, 15000)); // 5-15초 지연

            if (ThreadLocalRandom.current().nextInt(100) < 60) { // 60% 확률
                activeAttacks.remove("db-outage");
                throw new RuntimeException("💾 데이터베이스 연결 실패!");
            }

            activeAttacks.remove("db-outage");
            return ResponseEntity.ok("💾 데이터베이스 장애 복구 완료");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            activeAttacks.remove("db-outage");
            return ResponseEntity.internalServerError().body("Database 시나리오 중단");
        }
    }

    private ResponseEntity<String> executePaymentFailureScenario() {
        log.warn("🐒💳 Payment Failure 시나리오 실행");

        activeAttacks.add("payment-failure");

        if (ThreadLocalRandom.current().nextInt(100) < 40) { // 40% 확률
            activeAttacks.remove("payment-failure");
            throw new RuntimeException("💳 결제 시스템 장애 발생!");
        }

        activeAttacks.remove("payment-failure");
        return ResponseEntity.ok("💳 결제 시스템 정상 작동");
    }

    private ResponseEntity<String> executeNetworkPartitionScenario() {
        log.warn("🐒🌐 Network Partition 시나리오 실행");

        activeAttacks.add("network-partition");

        try {
            // 네트워크 분할 시뮬레이션 (매우 긴 지연)
            Thread.sleep(ThreadLocalRandom.current().nextInt(10000, 30000)); // 10-30초

            if (ThreadLocalRandom.current().nextInt(100) < 30) { // 30% 확률
                activeAttacks.remove("network-partition");
                throw new RuntimeException("🌐 네트워크 분할 감지!");
            }

            activeAttacks.remove("network-partition");
            return ResponseEntity.ok("🌐 네트워크 연결 복구 완료");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            activeAttacks.remove("network-partition");
            return ResponseEntity.internalServerError().body("Network 시나리오 중단");
        }
    }
}