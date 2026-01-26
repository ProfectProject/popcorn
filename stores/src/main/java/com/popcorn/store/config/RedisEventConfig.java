package com.popcorn.store.config;

import com.popcorn.store.event.RedisEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Redis Pub/Sub 이벤트 설정
 *
 * Order 서비스와의 이벤트 기반 통신을 위한 Redis 리스너 설정
 * 나중에 Kafka로 전환할 때는 이 설정 클래스를 교체하면 됨
 */
@Configuration
@RequiredArgsConstructor
public class RedisEventConfig {

    private final RedisEventListener redisEventListener;

    /**
     * Redis 메시지 리스너 컨테이너 설정
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 재고 차감 요청 이벤트 리스너 등록
        container.addMessageListener(
                new MessageListenerAdapter(redisEventListener),
                new PatternTopic("events:stock-deduction-requested")
        );

        return container;
    }
}