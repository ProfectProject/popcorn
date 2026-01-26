package com.popcorn.demo.config;

import com.popcorn.demo.event.UserEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Users 서비스 Redis Pub/Sub 이벤트 설정
 *
 * 다른 마이크로서비스에서 발생한 이벤트를 수신하여 사용자 관련 로직 처리
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "redis.events.enabled", havingValue = "true", matchIfMissing = true)
public class UserRedisEventConfig {

    private final UserEventListener userEventListener;

    /**
     * Redis 메시지 리스너 컨테이너 설정
     */
    @Bean
    public RedisMessageListenerContainer userRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 모든 이벤트 패턴 구독 (Users 서비스는 모니터링 목적)
        container.addMessageListener(
                new MessageListenerAdapter(userEventListener),
                new PatternTopic("events:*")
        );

        return container;
    }
}