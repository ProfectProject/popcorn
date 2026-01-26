package com.popcorn.checkIns.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * CheckIns 서비스 범용 이벤트 리스너
 * - Redis 이벤트 구독
 * - Spring Application 이벤트 구독
 * - 모든 이벤트를 로그로 기록
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CheckInRedisEventListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Redis Pub/Sub 이벤트 수신
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());

            log.info("🔔 [CHECKINS] Redis 이벤트 수신 - channel: {}, body: {}", channel, body);

            // 채널별 이벤트 처리
            handleRedisEvent(channel, body);

        } catch (Exception e) {
            log.error("🚨 [CHECKINS] Redis 이벤트 처리 실패 - message: {}, error: {}",
                    new String(message.getBody()), e.getMessage(), e);
        }
    }

    /**
     * Redis 이벤트 처리
     */
    private void handleRedisEvent(String channel, String body) {
        try {
            switch (channel) {
                case "events:order-created":
                    log.info("📦 [CHECKINS] 주문 생성 이벤트 수신 - {}", body);
                    break;
                case "events:order-paid":
                    log.info("💳 [CHECKINS] 주문 결제 완료 이벤트 수신 - {}", body);
                    break;
                case "events:payment-approved":
                    log.info("✅ [CHECKINS] 결제 승인 이벤트 수신 - {}", body);
                    // QR 코드 생성 준비
                    break;
                case "events:qr-generation-requested":
                    log.info("🔳 [CHECKINS] QR 코드 생성 요청 이벤트 수신 - {}", body);
                    // QR 코드 생성 처리 시작
                    break;
                case "events:qr-generated":
                    log.info("🎯 [CHECKINS] QR 코드 생성 완료 이벤트 수신 - {}", body);
                    break;
                case "events:qr-invalidation-requested":
                    log.info("❌ [CHECKINS] QR 코드 무효화 요청 이벤트 수신 - {}", body);
                    break;
                case "events:qr-checkin-requested":
                    log.info("📱 [CHECKINS] QR 체크인 요청 이벤트 수신 - {}", body);
                    break;
                case "events:qr-checkin-completed":
                    log.info("🎉 [CHECKINS] QR 체크인 완료 이벤트 수신 - {}", body);
                    break;
                default:
                    log.info("🔔 [CHECKINS] 기타 이벤트 수신 - channel: {}, body: {}", channel, body);
                    break;
            }
        } catch (Exception e) {
            log.error("🚨 [CHECKINS] 이벤트 처리 실패 - channel: {}, error: {}", channel, e.getMessage(), e);
        }
    }

    /**
     * Spring Application 이벤트 수신 (범용)
     */
    @EventListener
    public void handleApplicationEvent(Object event) {
        try {
            String eventType = event.getClass().getSimpleName();

            // QR/CheckIn 관련 이벤트는 더 자세히 로깅
            if (eventType.contains("Qr") || eventType.contains("QR") ||
                eventType.contains("Checkin") || eventType.contains("CheckIn")) {
                log.info("🌟 [CHECKINS] QR/CheckIn 관련 이벤트 수신 - type: {}, event: {}", eventType, event.toString());
            } else {
                log.debug("🔔 [CHECKINS] Application 이벤트 수신 - type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("🚨 [CHECKINS] Application 이벤트 처리 실패 - event: {}, error: {}",
                    event.getClass().getSimpleName(), e.getMessage(), e);
        }
    }
}