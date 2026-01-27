package com.popcorn.gateway.config;

import com.popcorn.gateway.event.GatewayEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Gateway 서비스 Redis Pub/Sub 이벤트 설정
 *
 * 다른 마이크로서비스에서 발생한 이벤트를 수신하여 Gateway 메트릭 및 모니터링 처리
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "redis.events.enabled", havingValue = "true", matchIfMissing = true)
public class GatewayRedisEventConfig {

    private final GatewayEventListener gatewayEventListener;

    /**
     * Redis 메시지 리스너 컨테이너 설정
     */
    @Bean
    public RedisMessageListenerContainer gatewayRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 모든 이벤트 구독 (Gateway는 모니터링 및 메트릭 목적)
        container.addMessageListener(
                new MessageListenerAdapter(gatewayEventListener),
                new PatternTopic("events:*")
        );

        // 서비스 헬스체크 이벤트 구독
        container.addMessageListener(
                new MessageListenerAdapter(gatewayEventListener),
                new PatternTopic("events:service-*")
        );

        // Rate Limit 관련 이벤트 구독
        container.addMessageListener(
                new MessageListenerAdapter(gatewayEventListener),
                new PatternTopic("events:rate-limit-*")
        );

        return container;
    }
}