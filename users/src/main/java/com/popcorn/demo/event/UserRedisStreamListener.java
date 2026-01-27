package com.popcorn.demo.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * User 서비스 Redis Stream 이벤트 리스너
 * - 주문, 결제 관련 이벤트를 Stream으로 수신
 * - 사용자 통계, 알림, 포인트 적립 등 처리
 * - Consumer Group 기반 메시지 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserRedisStreamListener implements StreamListener<String, MapRecord<String, String, Object>> {

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onMessage(MapRecord<String, String, Object> record) {
        try {
            String streamName = record.getStream();
            String recordId = record.getId().getValue();
            Map<String, Object> values = record.getValue();

            log.info("🔔 [USERS] Stream 메시지 수신 - stream: {}, recordId: {}, eventType: {}",
                    streamName, recordId, values.get("eventType"));

            String eventType = (String) values.get("eventType");
            handleStreamEvent(eventType, values);

            // 메시지 처리 완료 후 ACK (자동으로 처리됨)
            log.debug("✅ [USERS] 메시지 처리 완료 - stream: {}, recordId: {}", streamName, recordId);

        } catch (Exception e) {
            log.error("🚨 [USERS] Stream 메시지 처리 실패 - record: {}, error: {}",
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
                // 주문 관련 이벤트
                case "order-created":
                    log.info("📦 [USERS] 주문 생성 이벤트 수신 - orderId: {}, userId: {}",
                            values.get("orderId"), values.get("userId"));
                    // 사용자 주문 통계 업데이트
                    handleOrderCreated(values);
                    break;
                case "order-paid":
                    log.info("💳 [USERS] 주문 결제 완료 이벤트 수신 - orderId: {}, userId: {}",
                            values.get("orderId"), values.get("userId"));
                    // 사용자 구매 이력 업데이트
                    handleOrderPaid(values);
                    break;

                // 결제 관련 이벤트
                case "payment-approved":
                    log.info("✅ [USERS] 결제 승인 이벤트 수신 - paymentId: {}", values.get("paymentId"));
                    // 포인트 적립, 사용자 알림 등
                    handlePaymentApproved(values);
                    break;
                case "payment-failed":
                    log.warn("❌ [USERS] 결제 실패 이벤트 수신 - paymentId: {}", values.get("paymentId"));
                    // 결제 실패 알림
                    handlePaymentFailed(values);
                    break;
                case "payment-completed":
                    log.info("✅ [USERS] 결제 완료 이벤트 수신 - paymentId: {}", values.get("paymentId"));
                    // 최종 포인트 적립
                    handlePaymentCompleted(values);
                    break;

                // 사용자 관련 이벤트
                case "user-created":
                    log.info("👤 [USERS] 사용자 생성 이벤트 수신 - userId: {}", values.get("userId"));
                    handleUserCreated(values);
                    break;
                case "user-updated":
                    log.info("👤 [USERS] 사용자 업데이트 이벤트 수신 - userId: {}", values.get("userId"));
                    handleUserUpdated(values);
                    break;

                default:
                    log.info("🔔 [USERS] 기타 이벤트 수신 - eventType: {}", eventType);
                    break;
            }
        } catch (Exception e) {
            log.error("🚨 [USERS] 이벤트 처리 실패 - eventType: {}, error: {}", eventType, e.getMessage(), e);
        }
    }

    /**
     * 주문 생성 이벤트 처리
     */
    private void handleOrderCreated(Map<String, Object> values) {
        try {
            String userId = (String) values.get("userId");
            String orderId = (String) values.get("orderId");

            log.info("📊 [USERS] 사용자 주문 통계 업데이트 - userId: {}, orderId: {}", userId, orderId);
            // 사용자 주문 통계, 활동 로그 업데이트 등

        } catch (Exception e) {
            log.error("🚨 [USERS] 주문 생성 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    /**
     * 주문 결제 완료 이벤트 처리
     */
    private void handleOrderPaid(Map<String, Object> values) {
        try {
            String userId = (String) values.get("userId");
            String orderId = (String) values.get("orderId");
            String totalAmount = (String) values.get("totalAmount");

            log.info("💰 [USERS] 사용자 구매 이력 업데이트 - userId: {}, amount: {}", userId, totalAmount);
            // 사용자 구매 이력, 등급 업데이트 등

        } catch (Exception e) {
            log.error("🚨 [USERS] 주문 결제 완료 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    /**
     * 결제 승인 이벤트 처리
     */
    private void handlePaymentApproved(Map<String, Object> values) {
        try {
            String paymentId = (String) values.get("paymentId");
            String amount = (String) values.get("amount");

            log.info("🎁 [USERS] 포인트 적립 처리 - paymentId: {}, amount: {}", paymentId, amount);
            // 포인트 적립, 푸시 알림 발송 등

        } catch (Exception e) {
            log.error("🚨 [USERS] 결제 승인 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    /**
     * 결제 실패 이벤트 처리
     */
    private void handlePaymentFailed(Map<String, Object> values) {
        try {
            String paymentId = (String) values.get("paymentId");
            String reason = (String) values.get("reason");

            log.info("📱 [USERS] 결제 실패 알림 발송 - paymentId: {}, reason: {}", paymentId, reason);
            // 결제 실패 푸시 알림, 이메일 발송 등

        } catch (Exception e) {
            log.error("🚨 [USERS] 결제 실패 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    /**
     * 결제 완료 이벤트 처리
     */
    private void handlePaymentCompleted(Map<String, Object> values) {
        try {
            String paymentId = (String) values.get("paymentId");

            log.info("🏆 [USERS] 최종 포인트 적립 - paymentId: {}", paymentId);
            // 최종 포인트 적립, 사용자 등급 업데이트 등

        } catch (Exception e) {
            log.error("🚨 [USERS] 결제 완료 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    /**
     * 사용자 생성 이벤트 처리
     */
    private void handleUserCreated(Map<String, Object> values) {
        try {
            String userId = (String) values.get("userId");

            log.info("🎉 [USERS] 신규 사용자 환영 처리 - userId: {}", userId);
            // 환영 이메일, 첫 가입 혜택 등

        } catch (Exception e) {
            log.error("🚨 [USERS] 사용자 생성 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    /**
     * 사용자 업데이트 이벤트 처리
     */
    private void handleUserUpdated(Map<String, Object> values) {
        try {
            String userId = (String) values.get("userId");

            log.info("🔄 [USERS] 사용자 정보 업데이트 처리 - userId: {}", userId);
            // 프로필 변경 알림 등

        } catch (Exception e) {
            log.error("🚨 [USERS] 사용자 업데이트 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }
}