package com.popcorn.demo.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * User 서비스 범용 이벤트 리스너
 * - Redis 이벤트 구독
 * - Spring Application 이벤트 구독
 * - 모든 이벤트를 로그로 기록
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener implements MessageListener {

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

            log.info("🔔 [USERS] Redis 이벤트 수신 - channel: {}, body: {}", channel, body);

            // 채널별 이벤트 처리
            handleRedisEvent(channel, body);

        } catch (Exception e) {
            log.error("🚨 [USERS] Redis 이벤트 처리 실패 - message: {}, error: {}",
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
                    log.info("📦 [USERS] 주문 생성 이벤트 수신 - {}", body);
                    break;
                case "events:order-paid":
                    log.info("💳 [USERS] 주문 결제 완료 이벤트 수신 - {}", body);
                    break;
                case "events:payment-approved":
                    log.info("✅ [USERS] 결제 승인 이벤트 수신 - {}", body);
                    break;
                case "events:payment-failed":
                    log.warn("❌ [USERS] 결제 실패 이벤트 수신 - {}", body);
                    break;
                case "events:qr-generated":
                    log.info("🔳 [USERS] QR 코드 생성 이벤트 수신 - {}", body);
                    break;
                case "events:stock-deduction-success":
                    log.info("📦 [USERS] 재고 차감 성공 이벤트 수신 - {}", body);
                    break;
                case "events:stock-deduction-failed":
                    log.warn("⚠️ [USERS] 재고 차감 실패 이벤트 수신 - {}", body);
                    break;
                default:
                    log.info("🔔 [USERS] 기타 이벤트 수신 - channel: {}, body: {}", channel, body);
                    break;
            }
        } catch (Exception e) {
            log.error("🚨 [USERS] 이벤트 처리 실패 - channel: {}, error: {}", channel, e.getMessage(), e);
        }
    }

    /**
     * Spring Application 이벤트 수신 (범용)
     */
    @EventListener
    public void handleApplicationEvent(Object event) {
        try {
            String eventType = event.getClass().getSimpleName();
            log.info("🌟 [USERS] Application 이벤트 수신 - type: {}, event: {}", eventType, event.toString());

            // 특정 이벤트에 대한 추가 처리가 필요한 경우 여기서 처리

        } catch (Exception e) {
            log.error("🚨 [USERS] Application 이벤트 처리 실패 - event: {}, error: {}",
                    event.getClass().getSimpleName(), e.getMessage(), e);
        }
    }
}