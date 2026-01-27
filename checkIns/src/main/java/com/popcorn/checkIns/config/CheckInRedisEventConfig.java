package com.popcorn.checkIns.config;

import com.popcorn.checkIns.event.CheckInRedisEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * CheckIns 서비스 Redis Pub/Sub 이벤트 설정
 *
 * 결제 승인 이벤트를 수신하여 QR 코드 생성 및 체크인 관련 로직 처리
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "redis.events.enabled", havingValue = "true", matchIfMissing = true)
public class CheckInRedisEventConfig {

    private final CheckInRedisEventListener checkInRedisEventListener;

    /**
     * Redis 메시지 리스너 컨테이너 설정
     */
    @Bean
    public RedisMessageListenerContainer checkInRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 주문 관련 이벤트 구독
        container.addMessageListener(
                new MessageListenerAdapter(checkInRedisEventListener),
                new PatternTopic("events:order-*")
        );

        // 결제 관련 이벤트 구독
        container.addMessageListener(
                new MessageListenerAdapter(checkInRedisEventListener),
                new PatternTopic("events:payment-*")
        );

        // QR 관련 이벤트 구독
        container.addMessageListener(
                new MessageListenerAdapter(checkInRedisEventListener),
                new PatternTopic("events:qr-*")
        );

        // 체크인 관련 이벤트 구독
        container.addMessageListener(
                new MessageListenerAdapter(checkInRedisEventListener),
                new PatternTopic("events:checkin-*")
        );

        // 모든 이벤트 구독 (모니터링 목적)
        container.addMessageListener(
                new MessageListenerAdapter(checkInRedisEventListener),
                new PatternTopic("events:*")
        );

        return container;
    }
}