package com.popcorn.demo.domain.order.event;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.Getter;

/**
 * 주문 이벤트 메트릭스 수집 서비스
 *
 * 이벤트 처리 성능 및 상태 모니터링:
 * - 이벤트별 처리 횟수 추적
 * - 오류 발생률 모니터링
 * - 처리 시간 측정
 * - 비즈니스 메트릭 수집
 */
@Service
public class OrderEventMetrics {

    private static final Logger log = LoggerFactory.getLogger(OrderEventMetrics.class);

    // 이벤트 타입별 처리 카운터
    private final Map<String, AtomicLong> eventProcessedCounts = new ConcurrentHashMap<>();

    // 이벤트 타입별 오류 카운터
    private final Map<String, AtomicLong> eventErrorCounts = new ConcurrentHashMap<>();

    // 우선순위별 처리 카운터
    private final Map<String, AtomicLong> priorityProcessedCounts = new ConcurrentHashMap<>();

    // 오류 타입별 카운터
    private final Map<String, AtomicLong> errorTypeCounts = new ConcurrentHashMap<>();

    // 시간별 통계 (마지막 리셋 시간)
    private volatile LocalDateTime lastResetTime = LocalDateTime.now();

    // 전체 통계
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalErrorsOccurred = new AtomicLong(0);

    /**
     * 이벤트 처리 완료 기록
     */
    public void recordEventProcessed(String eventType, String priority) {
        eventProcessedCounts.computeIfAbsent(eventType, k -> new AtomicLong(0))
                          .incrementAndGet();

        priorityProcessedCounts.computeIfAbsent(priority, k -> new AtomicLong(0))
                             .incrementAndGet();

        totalEventsProcessed.incrementAndGet();

        log.debug("📊 이벤트 처리 기록 - 타입: {}, 우선순위: {}", eventType, priority);
    }

    /**
     * 이벤트 처리 오류 기록
     */
    public void recordEventError(String eventType, String errorType) {
        eventErrorCounts.computeIfAbsent(eventType, k -> new AtomicLong(0))
                       .incrementAndGet();

        errorTypeCounts.computeIfAbsent(errorType, k -> new AtomicLong(0))
                      .incrementAndGet();

        totalErrorsOccurred.incrementAndGet();

        log.warn("❌ 이벤트 오류 기록 - 타입: {}, 오류: {}", eventType, errorType);
    }

    /**
     * 전체 메트릭 조회
     */
    public EventMetrics getMetrics() {
        return EventMetrics.builder()
                .totalEventsProcessed(totalEventsProcessed.get())
                .totalErrorsOccurred(totalErrorsOccurred.get())
                .eventProcessedCounts(copyAtomicMap(eventProcessedCounts))
                .eventErrorCounts(copyAtomicMap(eventErrorCounts))
                .priorityProcessedCounts(copyAtomicMap(priorityProcessedCounts))
                .errorTypeCounts(copyAtomicMap(errorTypeCounts))
                .lastResetTime(lastResetTime)
                .errorRate(calculateErrorRate())
                .mostProcessedEventType(getMostProcessedEventType())
                .mostCommonErrorType(getMostCommonErrorType())
                .build();
    }

    /**
     * 특정 이벤트 타입의 메트릭 조회
     */
    public EventTypeMetrics getEventTypeMetrics(String eventType) {
        long processedCount = getCountForEventType(eventProcessedCounts, eventType);
        long errorCount = getCountForEventType(eventErrorCounts, eventType);

        return EventTypeMetrics.builder()
                .eventType(eventType)
                .processedCount(processedCount)
                .errorCount(errorCount)
                .errorRate(calculateEventTypeErrorRate(processedCount, errorCount))
                .isHealthy(isEventTypeHealthy(processedCount, errorCount))
                .build();
    }

    /**
     * 메트릭 리셋
     */
    public void resetMetrics() {
        log.info("🔄 이벤트 메트릭 리셋 시작");

        eventProcessedCounts.clear();
        eventErrorCounts.clear();
        priorityProcessedCounts.clear();
        errorTypeCounts.clear();

        totalEventsProcessed.set(0);
        totalErrorsOccurred.set(0);

        lastResetTime = LocalDateTime.now();

        log.info("✅ 이벤트 메트릭 리셋 완료 - 시간: {}", lastResetTime);
    }

    /**
     * 건강성 체크
     */
    public boolean isHealthy() {
        double errorRate = calculateErrorRate();
        long totalEvents = totalEventsProcessed.get();

        // 이벤트가 아직 없으면 초기 상태로 건강함 처리
        if (totalEvents == 0) {
            return true;
        }

        // 전체 오류율이 5% 미만이면 건강함
        return errorRate < 5.0;
    }

    /**
     * 시간별 통계 요약
     */
    public String getMetricsSummary() {
        EventMetrics metrics = getMetrics();

        return String.format("""
                📊 이벤트 메트릭 요약 (리셋 이후: %s)
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                📈 처리 통계
                   - 총 처리: %,d 건
                   - 총 오류: %,d 건
                   - 오류율: %.2f%%
                   - 가장 많이 처리된 이벤트: %s
                   - 가장 흔한 오류: %s

                🔥 이벤트별 처리 현황
                %s

                ⚠️ 오류 현황
                %s

                🎯 우선순위별 현황
                %s
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                """,
                lastResetTime,
                metrics.getTotalEventsProcessed(),
                metrics.getTotalErrorsOccurred(),
                metrics.getErrorRate(),
                metrics.getMostProcessedEventType(),
                metrics.getMostCommonErrorType(),
                formatCountMap("   ", metrics.getEventProcessedCounts()),
                formatCountMap("   ", metrics.getEventErrorCounts()),
                formatCountMap("   ", metrics.getPriorityProcessedCounts())
        );
    }

    // ================ 내부 헬퍼 메서드들 ================

    private Map<String, Long> copyAtomicMap(Map<String, AtomicLong> atomicMap) {
        Map<String, Long> result = new ConcurrentHashMap<>();
        atomicMap.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }

    private long getCountForEventType(Map<String, AtomicLong> map, String eventType) {
        AtomicLong counter = map.get(eventType);
        return counter != null ? counter.get() : 0;
    }

    private double calculateErrorRate() {
        long totalEvents = totalEventsProcessed.get();
        long totalErrors = totalErrorsOccurred.get();

        if (totalEvents == 0) return 0.0;
        return (double) totalErrors / totalEvents * 100.0;
    }

    private double calculateEventTypeErrorRate(long processedCount, long errorCount) {
        if (processedCount == 0) return 0.0;
        return (double) errorCount / processedCount * 100.0;
    }

    private boolean isEventTypeHealthy(long processedCount, long errorCount) {
        if (processedCount == 0) return true; // 처리된 것이 없으면 건강함으로 간주
        double errorRate = calculateEventTypeErrorRate(processedCount, errorCount);
        return errorRate < 10.0; // 10% 미만이면 건강함
    }

    private String getMostProcessedEventType() {
        return eventProcessedCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue(Comparator.comparingLong(AtomicLong::get)))
                .map(Map.Entry::getKey)
                .orElse("없음");
    }

    private String getMostCommonErrorType() {
        return errorTypeCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue(Comparator.comparingLong(AtomicLong::get)))
                .map(Map.Entry::getKey)
                .orElse("없음");
    }

    private String formatCountMap(String prefix, Map<String, Long> map) {
        if (map.isEmpty()) return prefix + "없음";

        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> String.format("%s%s: %,d", prefix, entry.getKey(), entry.getValue()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse(prefix + "없음");
    }

    // ================ 내부 클래스들 ================

    /**
     * 전체 이벤트 메트릭
     */
    @Getter
    public static class EventMetrics {
        private final long totalEventsProcessed;
        private final long totalErrorsOccurred;
        private final Map<String, Long> eventProcessedCounts;
        private final Map<String, Long> eventErrorCounts;
        private final Map<String, Long> priorityProcessedCounts;
        private final Map<String, Long> errorTypeCounts;
        private final LocalDateTime lastResetTime;
        private final double errorRate;
        private final String mostProcessedEventType;
        private final String mostCommonErrorType;

        private EventMetrics(Builder builder) {
            this.totalEventsProcessed = builder.totalEventsProcessed;
            this.totalErrorsOccurred = builder.totalErrorsOccurred;
            this.eventProcessedCounts = Map.copyOf(builder.eventProcessedCounts);
            this.eventErrorCounts = Map.copyOf(builder.eventErrorCounts);
            this.priorityProcessedCounts = Map.copyOf(builder.priorityProcessedCounts);
            this.errorTypeCounts = Map.copyOf(builder.errorTypeCounts);
            this.lastResetTime = builder.lastResetTime;
            this.errorRate = builder.errorRate;
            this.mostProcessedEventType = builder.mostProcessedEventType;
            this.mostCommonErrorType = builder.mostCommonErrorType;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private long totalEventsProcessed;
            private long totalErrorsOccurred;
            private Map<String, Long> eventProcessedCounts = Map.of();
            private Map<String, Long> eventErrorCounts = Map.of();
            private Map<String, Long> priorityProcessedCounts = Map.of();
            private Map<String, Long> errorTypeCounts = Map.of();
            private LocalDateTime lastResetTime;
            private double errorRate;
            private String mostProcessedEventType;
            private String mostCommonErrorType;

            public Builder totalEventsProcessed(long count) { this.totalEventsProcessed = count; return this; }
            public Builder totalErrorsOccurred(long count) { this.totalErrorsOccurred = count; return this; }
            public Builder eventProcessedCounts(Map<String, Long> counts) { this.eventProcessedCounts = counts; return this; }
            public Builder eventErrorCounts(Map<String, Long> counts) { this.eventErrorCounts = counts; return this; }
            public Builder priorityProcessedCounts(Map<String, Long> counts) { this.priorityProcessedCounts = counts; return this; }
            public Builder errorTypeCounts(Map<String, Long> counts) { this.errorTypeCounts = counts; return this; }
            public Builder lastResetTime(LocalDateTime time) { this.lastResetTime = time; return this; }
            public Builder errorRate(double rate) { this.errorRate = rate; return this; }
            public Builder mostProcessedEventType(String type) { this.mostProcessedEventType = type; return this; }
            public Builder mostCommonErrorType(String type) { this.mostCommonErrorType = type; return this; }

            public EventMetrics build() { return new EventMetrics(this); }
        }
    }

    /**
     * 이벤트 타입별 메트릭
     */
    @Getter
    public static class EventTypeMetrics {
        private final String eventType;
        private final long processedCount;
        private final long errorCount;
        private final double errorRate;
        private final boolean isHealthy;

        private EventTypeMetrics(Builder builder) {
            this.eventType = builder.eventType;
            this.processedCount = builder.processedCount;
            this.errorCount = builder.errorCount;
            this.errorRate = builder.errorRate;
            this.isHealthy = builder.isHealthy;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private String eventType;
            private long processedCount;
            private long errorCount;
            private double errorRate;
            private boolean isHealthy;

            public Builder eventType(String type) { this.eventType = type; return this; }
            public Builder processedCount(long count) { this.processedCount = count; return this; }
            public Builder errorCount(long count) { this.errorCount = count; return this; }
            public Builder errorRate(double rate) { this.errorRate = rate; return this; }
            public Builder isHealthy(boolean healthy) { this.isHealthy = healthy; return this; }

            public EventTypeMetrics build() { return new EventTypeMetrics(this); }
        }
    }
}
