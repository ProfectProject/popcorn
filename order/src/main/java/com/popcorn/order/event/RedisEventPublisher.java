package com.popcorn.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub을 사용한 이벤트 퍼블리셔
 *
 * 마이크로서비스 간 이벤트 통신을 위해 Redis를 중간 매개체로 사용
 * 나중에 Kafka로 전환할 때는 이 클래스만 교체하면 됨
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // 이벤트 토픽 상수
    private static final String STOCK_DEDUCTION_REQUESTED_TOPIC = "events:stock-deduction-requested";
    private static final String STOCK_DEDUCTION_SUCCESS_TOPIC = "events:stock-deduction-success";
    private static final String STOCK_DEDUCTION_FAILED_TOPIC = "events:stock-deduction-failed";

    /**
     * 재고 차감 요청 이벤트 발행 (Store 서비스에서 수신)
     */
    public void publishStockDeductionRequestedEvent(StockDeductionRequestedEvent event) {
        try {
            log.info("재고 차감 요청 이벤트 발행 시작 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

            String eventJson = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(STOCK_DEDUCTION_REQUESTED_TOPIC, eventJson);

            log.info("재고 차감 요청 이벤트 발행 완료 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

        } catch (Exception e) {
            log.error("재고 차감 요청 이벤트 발행 실패 - orderId: {}, eventId: {}, error: {}",
                    event.getOrderId(), event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("재고 차감 요청 이벤트 발행 실패", e);
        }
    }

    /**
     * 재고 차감 성공 이벤트 수신 확인 로그
     */
    public void logStockDeductionSuccessEvent(StockDeductionSuccessEvent event) {
        log.info("재고 차감 성공 이벤트 수신 확인 - orderId: {}, eventId: {}",
                event.getOrderId(), event.getEventId());
    }

    /**
     * 재고 차감 실패 이벤트 수신 확인 로그
     */
    public void logStockDeductionFailedEvent(StockDeductionFailedEvent event) {
        log.info("재고 차감 실패 이벤트 수신 확인 - orderId: {}, eventId: {}, reason: {}",
                event.getOrderId(), event.getEventId(), event.getReason());
    }
}