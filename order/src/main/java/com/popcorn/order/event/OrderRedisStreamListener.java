package com.popcorn.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import com.popcorn.order.service.OrderPriceLookupService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Order 서비스 Redis Stream 이벤트 리스너
 * - Store 서비스로부터 재고 처리 결과 및 가격 조회 응답 수신
 * - Consumer Group 기반 메시지 처리
 * - 메시지 ACK 자동 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderRedisStreamListener implements StreamListener<String, MapRecord<String, String, Object>> {

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OrderPriceLookupService orderPriceLookupService;

    @Override
    public void onMessage(MapRecord<String, String, Object> record) {
        try {
            String streamName = record.getStream();
            String recordId = record.getId().getValue();
            Map<String, Object> values = record.getValue();

            log.info("🔔 [ORDER] Stream 메시지 수신 - stream: {}, recordId: {}, eventType: {}",
                    streamName, recordId, values.get("eventType"));

            String eventType = (String) values.get("eventType");
            handleStreamEvent(eventType, values);

            // 메시지 처리 완료 후 ACK (자동으로 처리됨)
            log.debug("✅ [ORDER] 메시지 처리 완료 - stream: {}, recordId: {}", streamName, recordId);

        } catch (Exception e) {
            log.error("🚨 [ORDER] Stream 메시지 처리 실패 - record: {}, error: {}",
                    record, e.getMessage(), e);
            // TODO: 실패한 메시지를 DLQ(Dead Letter Queue)로 이동하거나 재시도 로직 구현
        }
    }

    /**
     * Stream 이벤트 타입별 처리
     */
    private void handleStreamEvent(String eventType, Map<String, Object> values) {
        try {
            switch (eventType) {
                case "stock-deduction-success":
                    log.info("✅ [ORDER] 재고 차감 성공 이벤트 수신");
                    handleStockDeductionSuccess(values);
                    break;
                case "stock-deduction-failed":
                    log.info("❌ [ORDER] 재고 차감 실패 이벤트 수신");
                    handleStockDeductionFailed(values);
                    break;
                case "price-lookup-response":
                    log.info("💰 [ORDER] 가격 조회 응답 이벤트 수신");
                    handlePriceLookupResponse(values);
                    break;
                default:
                    log.info("🔔 [ORDER] 알 수 없는 이벤트 타입 - type: {}", eventType);
                    break;
            }
        } catch (Exception e) {
            log.error("🚨 [ORDER] 이벤트 처리 실패 - eventType: {}, error: {}", eventType, e.getMessage(), e);
        }
    }

    /**
     * 재고 차감 성공 이벤트 처리
     */
    private void handleStockDeductionSuccess(Map<String, Object> values) {
        try {
            log.info("재고 차감 성공 이벤트 처리 시작 - eventId: {}", values.get("eventId"));

            StockDeductionSuccessEvent event = StockDeductionSuccessEvent.create(
                    UUID.fromString((String) values.get("orderId")),
                    (String) values.get("orderNo"),
                    UUID.fromString((String) values.get("popupId")),
                    (String) values.get("stockDetails")
            );

            // Spring 내부 이벤트로 발행 (기존 handleStockDeductionSuccess가 받음)
            applicationEventPublisher.publishEvent(event);

            log.info("재고 차감 성공 이벤트 처리 완료 - orderId: {}, eventId: {}",
                    values.get("orderId"), values.get("eventId"));

        } catch (Exception e) {
            log.error("재고 차감 성공 이벤트 처리 중 오류 - values: {}, error: {}",
                    values, e.getMessage(), e);
        }
    }

    /**
     * 재고 차감 실패 이벤트 처리
     */
    private void handleStockDeductionFailed(Map<String, Object> values) {
        try {
            log.info("재고 차감 실패 이벤트 처리 시작 - eventId: {}", values.get("eventId"));

            StockDeductionFailedEvent event = StockDeductionFailedEvent.forSystemError(
                    UUID.fromString((String) values.get("orderId")),
                    (String) values.get("orderNo"),
                    (String) values.get("reason")
            );

            // Spring 내부 이벤트로 발행 (기존 handleInternalStockDeductionFailed가 받음)
            applicationEventPublisher.publishEvent(event);

            log.info("재고 차감 실패 이벤트 처리 완료 - orderId: {}, eventId: {}",
                    values.get("orderId"), values.get("eventId"));

        } catch (Exception e) {
            log.error("재고 차감 실패 이벤트 처리 중 오류 - values: {}, error: {}",
                    values, e.getMessage(), e);
        }
    }

    /**
     * 가격 조회 응답 이벤트 처리
     */
    private void handlePriceLookupResponse(Map<String, Object> values) {
        try {
            log.info("가격 조회 응답 이벤트 처리 시작 - correlationId: {}", values.get("correlationId"));

            PriceLookupResponseEvent response = PriceLookupResponseEvent.builder()
                    .eventId((String) values.get("eventId"))
                    .correlationId((String) values.get("correlationId"))
                    .requestType((String) values.get("requestType"))
                    .sessionId(values.get("sessionId") != null && !values.get("sessionId").toString().isEmpty() ?
                               UUID.fromString((String) values.get("sessionId")) : null)
                    .goodsVariantId(values.get("goodsVariantId") != null && !values.get("goodsVariantId").toString().isEmpty() ?
                                    UUID.fromString((String) values.get("goodsVariantId")) : null)
                    .price(values.get("price") != null ? Integer.parseInt(values.get("price").toString()) : null)
                    .stockQuantity(values.get("stockQuantity") != null ?
                                   Integer.parseInt(values.get("stockQuantity").toString()) : null)
                    .success(Boolean.parseBoolean(values.get("success").toString()))
                    .message((String) values.get("message"))
                    .respondedAt(values.get("respondedAt") != null ?
                                 LocalDateTime.parse((String) values.get("respondedAt")) : null)
                    .eventTime(values.get("eventTime") != null ?
                               LocalDateTime.parse((String) values.get("eventTime")) : null)
                    .build();

            orderPriceLookupService.handlePriceLookupResponse(response);

            log.info("가격 조회 응답 이벤트 처리 완료 - correlationId: {}, success: {}",
                    response.getCorrelationId(), response.isSuccess());
        } catch (Exception e) {
            log.error("가격 조회 응답 이벤트 처리 중 오류 - values: {}, error: {}",
                    values, e.getMessage(), e);
        }
    }
}