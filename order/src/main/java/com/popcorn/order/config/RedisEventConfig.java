package com.popcorn.order.config;

import com.popcorn.order.event.RedisEventSubscriber;
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
 * Store 서비스와의 이벤트 기반 통신을 위한 Redis 구독 설정
 * 나중에 Kafka로 전환할 때는 이 설정 클래스를 교체하면 됨
 */
@Configuration
@RequiredArgsConstructor
public class RedisEventConfig {

    private final RedisEventSubscriber redisEventSubscriber;

    /**
     * Redis 메시지 리스너 컨테이너 설정
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 재고 차감 성공 이벤트 구독
        container.addMessageListener(
                new MessageListenerAdapter(redisEventSubscriber),
                new PatternTopic("events:stock-deduction-success")
        );

        // 재고 차감 실패 이벤트 구독
        container.addMessageListener(
                new MessageListenerAdapter(redisEventSubscriber),
                new PatternTopic("events:stock-deduction-failed")
        );

        // 가격 조회 응답 이벤트 구독
        container.addMessageListener(
                new MessageListenerAdapter(redisEventSubscriber),
                new PatternTopic("events:price-lookup-response")
        );

        return container;
    }
}
