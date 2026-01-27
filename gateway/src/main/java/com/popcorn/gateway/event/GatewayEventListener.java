package com.popcorn.gateway.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Gateway 서비스 범용 이벤트 리스너
 * - Redis 이벤트 구독
 * - Spring Application 이벤트 구독
 * - 모든 이벤트를 로그로 기록
 * - Gateway 성능 메트릭 수집
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GatewayEventListener implements MessageListener {

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

            log.info("🔔 [GATEWAY] Redis 이벤트 수신 - channel: {}, body: {}", channel, body);

            // 채널별 이벤트 처리
            handleRedisEvent(channel, body);

        } catch (Exception e) {
            log.error("🚨 [GATEWAY] Redis 이벤트 처리 실패 - message: {}, error: {}",
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
                    log.info("📦 [GATEWAY] 주문 생성 이벤트 수신 - {}", body);
                    // Gateway 라우팅 메트릭 업데이트
                    break;
                case "events:payment-approved":
                    log.info("✅ [GATEWAY] 결제 승인 이벤트 수신 - {}", body);
                    // Gateway 성공률 메트릭 업데이트
                    break;
                case "events:payment-failed":
                    log.warn("❌ [GATEWAY] 결제 실패 이벤트 수신 - {}", body);
                    // Gateway 실패율 메트릭 업데이트
                    break;
                case "events:service-health-check":
                    log.info("🏥 [GATEWAY] 서비스 헬스체크 이벤트 수신 - {}", body);
                    // Circuit Breaker 상태 업데이트
                    break;
                case "events:rate-limit-exceeded":
                    log.warn("⚡ [GATEWAY] Rate Limit 초과 이벤트 수신 - {}", body);
                    // Rate Limiting 메트릭 업데이트
                    break;
                default:
                    log.info("🔔 [GATEWAY] 기타 이벤트 수신 - channel: {}, body: {}", channel, body);
                    break;
            }
        } catch (Exception e) {
            log.error("🚨 [GATEWAY] 이벤트 처리 실패 - channel: {}, error: {}", channel, e.getMessage(), e);
        }
    }

    /**
     * Spring Application 이벤트 수신 (범용)
     */
    @EventListener
    public void handleApplicationEvent(Object event) {
        try {
            String eventType = event.getClass().getSimpleName();

            // Gateway 관련 이벤트는 더 자세히 로깅
            if (eventType.contains("Gateway") || eventType.contains("Circuit") ||
                eventType.contains("Route") || eventType.contains("Filter") ||
                eventType.contains("Auth") || eventType.contains("Jwt")) {
                log.info("🌟 [GATEWAY] Gateway 관련 이벤트 수신 - type: {}, event: {}", eventType, event.toString());
            } else {
                log.debug("🔔 [GATEWAY] Application 이벤트 수신 - type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("🚨 [GATEWAY] Application 이벤트 처리 실패 - event: {}, error: {}",
                    event.getClass().getSimpleName(), e.getMessage(), e);
        }
    }
}