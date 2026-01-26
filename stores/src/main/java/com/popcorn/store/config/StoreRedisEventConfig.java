package com.popcorn.store.config;

import com.popcorn.store.event.StoreRedisEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Stores 서비스 Redis Pub/Sub 이벤트 설정
 *
 * 주문, 결제 서비스에서 발생한 이벤트를 수신하여 재고 처리 로직 실행
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "redis.events.enabled", havingValue = "true", matchIfMissing = true)
public class StoreRedisEventConfig {

    private final StoreRedisEventListener storeRedisEventListener;

    /**
     * Redis 메시지 리스너 컨테이너 설정
     */
    @Bean
    public RedisMessageListenerContainer storeRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 주문 관련 이벤트 구독
        container.addMessageListener(
                new MessageListenerAdapter(storeRedisEventListener),
                new PatternTopic("events:order-*")
        );

        // 결제 관련 이벤트 구독
        container.addMessageListener(
                new MessageListenerAdapter(storeRedisEventListener),
                new PatternTopic("events:payment-*")
        );

        // 재고 관련 이벤트 구독
        container.addMessageListener(
                new MessageListenerAdapter(storeRedisEventListener),
                new PatternTopic("events:inventory-*")
        );

        // 굿즈 관련 이벤트 구독
        container.addMessageListener(
                new MessageListenerAdapter(storeRedisEventListener),
                new PatternTopic("events:goods-*")
        );

        return container;
    }
}
