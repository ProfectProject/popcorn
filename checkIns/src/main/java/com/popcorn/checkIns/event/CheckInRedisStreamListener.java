package com.popcorn.checkIns.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * CheckIns 서비스 Redis Stream 이벤트 리스너
 * - 주문, 결제, QR 관련 이벤트를 Stream으로 수신
 * - Consumer Group 기반 메시지 처리
 * - QR 코드 생성 및 체크인 로직 트리거
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CheckInRedisStreamListener implements StreamListener<String, MapRecord<String, String, Object>> {

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onMessage(MapRecord<String, String, Object> record) {
        try {
            String streamName = record.getStream();
            String recordId = record.getId().getValue();
            Map<String, Object> values = record.getValue();

            log.info("🔔 [CHECKINS] Stream 메시지 수신 - stream: {}, recordId: {}, eventType: {}",
                    streamName, recordId, values.get("eventType"));

            String eventType = (String) values.get("eventType");
            handleStreamEvent(eventType, values);

            // 메시지 처리 완료 후 ACK (자동으로 처리됨)
            log.debug("✅ [CHECKINS] 메시지 처리 완료 - stream: {}, recordId: {}", streamName, recordId);

        } catch (Exception e) {
            log.error("🚨 [CHECKINS] Stream 메시지 처리 실패 - record: {}, error: {}",
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
                    log.info("📦 [CHECKINS] 주문 생성 이벤트 수신 - orderId: {}", values.get("orderId"));
                    break;
                case "order-paid":
                    log.info("💳 [CHECKINS] 주문 결제 완료 이벤트 수신 - orderId: {}", values.get("orderId"));
                    // QR 코드 생성 준비 로직 트리거 가능
                    break;

                // 결제 관련 이벤트
                case "payment-created":
                    log.info("🧾 [CHECKINS] 결제 생성 이벤트 수신 - paymentId: {}", values.get("paymentId"));
                    break;
                case "payment-approved":
                    log.info("✅ [CHECKINS] 결제 승인 이벤트 수신 - paymentId: {}", values.get("paymentId"));
                    // QR 코드 생성 트리거
                    handlePaymentApproved(values);
                    break;
                case "payment-failed":
                    log.warn("❌ [CHECKINS] 결제 실패 이벤트 수신 - paymentId: {}", values.get("paymentId"));
                    break;
                case "payment-cancelled":
                    log.info("↩️ [CHECKINS] 결제 취소 이벤트 수신 - paymentId: {}", values.get("paymentId"));
                    break;
                case "payment-completed":
                    log.info("✅ [CHECKINS] 결제 완료 이벤트 수신 - paymentId: {}", values.get("paymentId"));
                    break;

                // QR 관련 이벤트
                case "qr-generation-requested":
                    log.info("🔳 [CHECKINS] QR 코드 생성 요청 이벤트 수신 - orderId: {}", values.get("orderId"));
                    // QR 코드 생성 처리 시작
                    handleQrGenerationRequested(values);
                    break;
                case "qr-generated":
                    log.info("🎯 [CHECKINS] QR 코드 생성 완료 이벤트 수신 - qrCode: {}", values.get("qrCode"));
                    break;
                case "qr-invalidation-requested":
                    log.info("❌ [CHECKINS] QR 코드 무효화 요청 이벤트 수신 - qrCode: {}", values.get("qrCode"));
                    break;
                case "qr-checkin-requested":
                    log.info("📱 [CHECKINS] QR 체크인 요청 이벤트 수신 - qrCode: {}", values.get("qrCode"));
                    handleQrCheckinRequested(values);
                    break;
                case "qr-checkin-completed":
                    log.info("🎉 [CHECKINS] QR 체크인 완료 이벤트 수신 - checkinId: {}", values.get("checkinId"));
                    break;

                default:
                    log.info("🔔 [CHECKINS] 기타 이벤트 수신 - eventType: {}", eventType);
                    break;
            }
        } catch (Exception e) {
            log.error("🚨 [CHECKINS] 이벤트 처리 실패 - eventType: {}, error: {}", eventType, e.getMessage(), e);
        }
    }

    /**
     * 결제 승인 이벤트 처리 - QR 코드 생성 트리거
     */
    private void handlePaymentApproved(Map<String, Object> values) {
        try {
            String orderId = (String) values.get("orderId");
            if (orderId != null) {
                log.info("🔳 [CHECKINS] 결제 승인으로 인한 QR 코드 생성 준비 - orderId: {}", orderId);
                // QR 코드 생성 로직 트리거
                // 실제 QR 코드 생성은 QrCodeService에서 처리
            }
        } catch (Exception e) {
            log.error("🚨 [CHECKINS] 결제 승인 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    /**
     * QR 코드 생성 요청 이벤트 처리
     */
    private void handleQrGenerationRequested(Map<String, Object> values) {
        try {
            String orderId = (String) values.get("orderId");
            log.info("🔳 [CHECKINS] QR 코드 생성 요청 처리 시작 - orderId: {}", orderId);

            // QR 코드 생성 로직 처리
            // QrCodeService의 issue 메서드 호출 등

        } catch (Exception e) {
            log.error("🚨 [CHECKINS] QR 코드 생성 요청 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    /**
     * QR 체크인 요청 이벤트 처리
     */
    private void handleQrCheckinRequested(Map<String, Object> values) {
        try {
            String qrCode = (String) values.get("qrCode");
            String orderId = (String) values.get("orderId");

            log.info("📱 [CHECKINS] QR 체크인 요청 처리 시작 - qrCode: {}, orderId: {}", qrCode, orderId);

            // QR 체크인 로직 처리
            // QrCheckinService의 checkin 메서드 호출 등

        } catch (Exception e) {
            log.error("🚨 [CHECKINS] QR 체크인 요청 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }
}