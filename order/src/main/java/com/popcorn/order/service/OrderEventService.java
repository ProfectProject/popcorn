package com.popcorn.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.popcorn.order.dto.event.OrderEventResponse;
import com.popcorn.order.dto.event.OrderEventSummary;

/**
 * 주문 이벤트 서비스 인터페이스
 *
 * Event Sourcing 패턴을 구현하는 핵심 서비스입니다.
 * 주문 관련 모든 이벤트의 저장, 조회, 재생을 담당합니다.
 */
public interface OrderEventService {

    /**
     * 특정 주문의 모든 이벤트 히스토리를 시간순으로 조회
     *
     * @param orderId 조회할 주문 ID
     * @return 시간순으로 정렬된 이벤트 목록
     */
    List<OrderEventResponse> getOrderEventHistory(UUID orderId);

    /**
     * 시스템 전체의 이벤트 목록을 조건에 따라 페이징 조회
     *
     * @param eventType 이벤트 타입 필터 (선택적)
     * @param userId 사용자 ID 필터 (선택적)
     * @param from 검색 시작 시간 (선택적)
     * @param to 검색 종료 시간 (선택적)
     * @param status 이벤트 상태 필터 (선택적)
     * @param pageable 페이징 정보
     * @return 페이징된 이벤트 요약 목록
     */
    Page<OrderEventSummary> getSystemEvents(
            String eventType,
            Long userId,
            LocalDateTime from,
            LocalDateTime to,
            String status,
            Pageable pageable
    );

    /**
     * 특정 주문의 이벤트를 재생하여 상태를 복원
     *
     * 이벤트 소싱의 핵심 기능으로, 저장된 이벤트들을 순서대로 재생해서
     * 특정 시점의 상태를 복원하거나 현재 상태를 재계산합니다.
     *
     * @param orderId 재생할 주문 ID
     * @param upTo 재생할 종료 시점 (null이면 모든 이벤트 재생)
     * @return 재생 작업 추적용 ID
     */
    String replayOrderEvents(UUID orderId, LocalDateTime upTo);

    /**
     * 이벤트 통계 및 메트릭을 조회
     *
     * 비즈니스 인사이트 도출과 시스템 모니터링을 위한
     * 다양한 통계 정보를 계산하여 반환합니다.
     *
     * @param from 통계 시작 시간 (선택적)
     * @param to 통계 종료 시간 (선택적)
     * @param aggregateBy 집계 단위 (HOUR, DAY, WEEK, MONTH)
     * @return 계산된 메트릭 정보
     */
    Object getEventMetrics(LocalDateTime from, LocalDateTime to, String aggregateBy);

    /**
     * 새로운 이벤트를 이벤트 스토어에 저장
     *
     * @param orderId 관련 주문 ID
     * @param eventType 이벤트 타입
     * @param eventData 이벤트 상세 데이터
     * @param userId 이벤트 발생 사용자 (선택적)
     * @return 저장된 이벤트 ID
     */
    String saveEvent(UUID orderId, String eventType, Object eventData, Long userId);

    /**
     * 이벤트 처리 상태를 업데이트
     *
     * @param eventId 업데이트할 이벤트 ID
     * @param status 새로운 상태
     * @param processingTimeMs 처리 소요 시간 (밀리초)
     * @param errorMessage 오류 메시지 (실패 시)
     */
    void updateEventStatus(String eventId, String status, Long processingTimeMs, String errorMessage);

    /**
     * 실패한 이벤트들을 재시도
     *
     * 시스템 장애나 일시적 오류로 실패한 이벤트들을
     * 다시 처리하도록 큐에 등록합니다.
     *
     * @param maxRetryCount 최대 재시도 횟수
     * @return 재시도 대상 이벤트 개수
     */
    int retryFailedEvents(int maxRetryCount);

    /**
     * 이벤트 스토어의 오래된 이벤트 정리
     *
     * 성능 최적화를 위해 설정된 기간보다 오래된
     * 이벤트들을 아카이브하거나 삭제합니다.
     *
     * @param olderThanDays 보관 기간 (일)
     * @return 정리된 이벤트 개수
     */
    int cleanupOldEvents(int olderThanDays);

}