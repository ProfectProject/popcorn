package com.popcorn.demo.domain.order.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.versioning.ApiVersion;
import com.popcorn.demo.domain.order.event.BaseOrderEvent;
import com.popcorn.demo.domain.order.event.OrderEventStore;
import com.popcorn.demo.domain.order.event.OrderEventMetrics;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;

/**
 * 주문 이벤트 관리 및 모니터링 컨트롤러
 *
 * 기능:
 * - 이벤트 스토어 조회 및 관리
 * - 이벤트 메트릭 모니터링
 * - 이벤트 재생 및 디버깅 지원
 * - 이벤트 시스템 헬스체크
 */
@Hidden
@RestController
@ApiVersion("v1")
@RequestMapping("/api/v1/orders/events")
@RequiredArgsConstructor
public class OrderEventController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(OrderEventController.class);

    private final OrderEventStore eventStore;
    private final OrderEventMetrics eventMetrics;

    // ================ 이벤트 스토어 조회 ================

    /**
     * 특정 주문의 이벤트 스트림 조회
     */
    @GetMapping("/stream/{orderId}")
    public ResponseEntity<BaseResponse<List<OrderEventStore.EventRecord>>> getEventStream(
            @PathVariable UUID orderId) {

        log.info("📜 이벤트 스트림 조회 요청 - 주문ID: {}", orderId);

        try {
            List<OrderEventStore.EventRecord> events = eventStore.getEventStream(orderId);

            log.debug("✅ 이벤트 스트림 조회 완료 - 주문ID: {}, 이벤트 수: {}", orderId, events.size());
            return ok(events);

        } catch (Exception e) {
            log.error("❌ 이벤트 스트림 조회 실패 - 주문ID: {}", orderId, e);
            throw new RuntimeException("이벤트 스트림 조회 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 특정 주문의 이벤트 스트림 조회 (시간 범위)
     */
    @GetMapping("/stream/{orderId}/range")
    public ResponseEntity<BaseResponse<List<OrderEventStore.EventRecord>>> getEventStreamByTimeRange(
            @PathVariable UUID orderId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime) {

        log.info("📜 시간 범위 이벤트 스트림 조회 - 주문ID: {}, 기간: {} ~ {}", orderId, fromTime, toTime);

        try {
            List<OrderEventStore.EventRecord> events =
                eventStore.getEventStream(orderId, fromTime, toTime);

            log.debug("✅ 시간 범위 이벤트 스트림 조회 완료 - 주문ID: {}, 이벤트 수: {}",
                    orderId, events.size());
            return ok(events);

        } catch (Exception e) {
            log.error("❌ 시간 범위 이벤트 스트림 조회 실패 - 주문ID: {}", orderId, e);
            throw new RuntimeException("시간 범위 이벤트 스트림 조회 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 특정 이벤트 타입 조회
     */
    @GetMapping("/type/{eventType}")
    public ResponseEntity<BaseResponse<List<OrderEventStore.EventRecord>>> getEventsByType(
            @PathVariable String eventType) {

        log.info("📊 이벤트 타입별 조회 - 타입: {}", eventType);

        try {
            List<OrderEventStore.EventRecord> events = eventStore.getEventsByType(eventType);

            log.debug("✅ 이벤트 타입별 조회 완료 - 타입: {}, 이벤트 수: {}", eventType, events.size());
            return ok(events);

        } catch (Exception e) {
            log.error("❌ 이벤트 타입별 조회 실패 - 타입: {}", eventType, e);
            throw new RuntimeException("이벤트 타입별 조회 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 최근 이벤트 조회
     */
    @GetMapping("/recent")
    public ResponseEntity<BaseResponse<List<OrderEventStore.EventRecord>>> getRecentEvents(
            @RequestParam(defaultValue = "100") int limit) {

        log.info("⏰ 최근 이벤트 조회 - 한계: {}", limit);

        try {
            List<OrderEventStore.EventRecord> events = eventStore.getRecentEvents(limit);

            log.debug("✅ 최근 이벤트 조회 완료 - 이벤트 수: {}", events.size());
            return ok(events);

        } catch (Exception e) {
            log.error("❌ 최근 이벤트 조회 실패", e);
            throw new RuntimeException("최근 이벤트 조회 중 오류가 발생했습니다", e);
        }
    }

    // ================ 이벤트 재생 ================

    /**
     * 이벤트 재생
     */
    @PostMapping("/replay/{orderId}")
    public ResponseEntity<BaseResponse<List<BaseOrderEvent>>> replayEvents(@PathVariable UUID orderId) {
        log.info("🔄 이벤트 재생 요청 - 주문ID: {}", orderId);

        try {
            List<BaseOrderEvent> replayedEvents = eventStore.replayEvents(orderId);

            String message = String.format("이벤트 재생 완료 - 주문ID: %s, 재생된 이벤트 수: %d",
                    orderId, replayedEvents.size());

            log.info("✅ {}", message);
            return ok(replayedEvents);

        } catch (Exception e) {
            log.error("❌ 이벤트 재생 실패 - 주문ID: {}", orderId, e);
            throw new RuntimeException("이벤트 재생 중 오류가 발생했습니다", e);
        }
    }

    // ================ 이벤트 메트릭 조회 ================

    /**
     * 전체 이벤트 메트릭 조회
     */
    @GetMapping("/metrics")
    public ResponseEntity<BaseResponse<OrderEventMetrics.EventMetrics>> getEventMetrics() {
        log.info("📊 이벤트 메트릭 조회 요청");

        try {
            OrderEventMetrics.EventMetrics metrics = eventMetrics.getMetrics();

            log.debug("✅ 이벤트 메트릭 조회 완료 - 총 처리: {}, 총 오류: {}",
                    metrics.getTotalEventsProcessed(), metrics.getTotalErrorsOccurred());
            return ok(metrics);

        } catch (Exception e) {
            log.error("❌ 이벤트 메트릭 조회 실패", e);
            throw new RuntimeException("이벤트 메트릭 조회 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 특정 이벤트 타입의 메트릭 조회
     */
    @GetMapping("/metrics/{eventType}")
    public ResponseEntity<BaseResponse<OrderEventMetrics.EventTypeMetrics>> getEventTypeMetrics(
            @PathVariable String eventType) {

        log.info("📊 이벤트 타입 메트릭 조회 - 타입: {}", eventType);

        try {
            OrderEventMetrics.EventTypeMetrics metrics = eventMetrics.getEventTypeMetrics(eventType);

            log.debug("✅ 이벤트 타입 메트릭 조회 완료 - 타입: {}, 처리: {}, 오류: {}",
                    eventType, metrics.getProcessedCount(), metrics.getErrorCount());
            return ok(metrics);

        } catch (Exception e) {
            log.error("❌ 이벤트 타입 메트릭 조회 실패 - 타입: {}", eventType, e);
            throw new RuntimeException("이벤트 타입 메트릭 조회 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 메트릭 텍스트 요약
     */
    @GetMapping("/metrics/summary")
    public ResponseEntity<String> getMetricsSummary() {
        log.info("📋 이벤트 메트릭 요약 조회");

        try {
            String summary = eventMetrics.getMetricsSummary();
            log.debug("✅ 이벤트 메트릭 요약 조회 완료");
            return okText(summary);

        } catch (Exception e) {
            log.error("❌ 이벤트 메트릭 요약 조회 실패", e);
            throw new RuntimeException("이벤트 메트릭 요약 조회 중 오류가 발생했습니다", e);
        }
    }

    // ================ 이벤트 스토어 관리 ================

    /**
     * 이벤트 스토어 통계
     */
    @GetMapping("/store/stats")
    public ResponseEntity<BaseResponse<OrderEventStore.EventStoreStats>> getEventStoreStats() {
        log.info("📊 이벤트 스토어 통계 조회");

        try {
            OrderEventStore.EventStoreStats stats = eventStore.getStatistics();

            log.debug("✅ 이벤트 스토어 통계 조회 완료 - 총 이벤트: {}, 총 스트림: {}",
                    stats.getTotalEvents(), stats.getTotalOrderStreams());
            return ok(stats);

        } catch (Exception e) {
            log.error("❌ 이벤트 스토어 통계 조회 실패", e);
            throw new RuntimeException("이벤트 스토어 통계 조회 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 이벤트 스토어 정리 (오래된 이벤트 삭제)
     */
    @DeleteMapping("/store/cleanup")
    public ResponseEntity<BaseResponse<String>> cleanupEventStore(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeTime) {

        log.warn("🧹 이벤트 스토어 정리 요청 - 기준시간: {}", beforeTime);

        try {
            eventStore.cleanup(beforeTime);

            String message = String.format("이벤트 스토어 정리 완료 - 기준시간: %s", beforeTime);
            log.info("✅ {}", message);

            return ok(message);

        } catch (Exception e) {
            log.error("❌ 이벤트 스토어 정리 실패 - 기준시간: {}", beforeTime, e);
            throw new RuntimeException("이벤트 스토어 정리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 메트릭 리셋
     */
    @DeleteMapping("/metrics/reset")
    public ResponseEntity<BaseResponse<String>> resetMetrics() {
        log.warn("🔄 이벤트 메트릭 리셋 요청");

        try {
            eventMetrics.resetMetrics();

            String message = "이벤트 메트릭이 성공적으로 리셋되었습니다";
            log.info("✅ {}", message);

            return ok(message);

        } catch (Exception e) {
            log.error("❌ 이벤트 메트릭 리셋 실패", e);
            throw new RuntimeException("이벤트 메트릭 리셋 중 오류가 발생했습니다", e);
        }
    }

    // ================ 헬스체크 ================

    /**
     * 이벤트 시스템 헬스체크
     */
    @GetMapping("/health")
    public ResponseEntity<BaseResponse<String>> healthCheck() {
        log.debug("❤️ 이벤트 시스템 헬스체크");

        try {
            boolean isEventMetricsHealthy = eventMetrics.isHealthy();
            OrderEventStore.EventStoreStats storeStats = eventStore.getStatistics();

            boolean isEventStoreHealthy = storeStats.getTotalEvents() >= 0; // 기본 헬스체크

            if (isEventMetricsHealthy && isEventStoreHealthy) {
                String message = String.format(
                    "이벤트 시스템 정상 - 스토어 이벤트: %d개, 메트릭 오류율: %.2f%%",
                    storeStats.getTotalEvents(),
                    eventMetrics.getMetrics().getErrorRate()
                );
                return ok(message);
            } else {
                throw new RuntimeException("이벤트 시스템 상태 이상 감지");
            }

        } catch (Exception e) {
            log.error("❌ 이벤트 시스템 헬스체크 실패", e);
            throw new RuntimeException("이벤트 시스템이 정상적으로 동작하지 않습니다", e);
        }
    }
}
