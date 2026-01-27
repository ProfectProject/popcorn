package com.popcorn.order.config;

import com.popcorn.order.event.OrderRedisStreamListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;

/**
 * Order 서비스 Redis Stream 이벤트 설정
 *
 * Store 서비스에서 발생한 재고 처리 결과 및 가격 조회 응답 이벤트를 Stream으로 수신
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "redis.events.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class RedisEventConfig {

    private final OrderRedisStreamListener orderRedisStreamListener;
    private final RedisTemplate<String, Object> redisTemplate;

    // Stream 이름 상수
    private static final String RESPONSE_EVENTS_STREAM = "response-events";
    private static final String STOCK_EVENTS_STREAM = "stock-events";

    // Consumer Group 이름
    private static final String ORDER_CONSUMER_GROUP = "order-service-group";
    private static final String ORDER_CONSUMER_NAME = "order-consumer-1";

    @PostConstruct
    public void initializeStreamsAndConsumerGroups() {
        try {
            // Consumer Group 생성 (이미 존재하면 무시)
            createConsumerGroupIfNotExists(RESPONSE_EVENTS_STREAM);
            createConsumerGroupIfNotExists(STOCK_EVENTS_STREAM);

            log.info("✅ Order Service Redis Stream Consumer Groups 초기화 완료");
        } catch (Exception e) {
            log.warn("⚠️ Redis Stream 초기화 중 오류 (정상 동작 가능): {}", e.getMessage());
        }
    }

    private void createConsumerGroupIfNotExists(String streamName) {
        try {
            redisTemplate.opsForStream().createGroup(streamName, ReadOffset.from("0"), ORDER_CONSUMER_GROUP);
            log.info("📝 Order Consumer Group 생성: {} - {}", streamName, ORDER_CONSUMER_GROUP);
        } catch (Exception e) {
            // Consumer Group이 이미 존재하는 경우 무시
            log.debug("Order Consumer Group 이미 존재: {} - {}", streamName, ORDER_CONSUMER_GROUP);
        }
    }

    /**
     * Redis Stream 메시지 리스너 컨테이너 설정
     */
    @Bean
    public StreamMessageListenerContainer orderStreamListenerContainer(
            RedisConnectionFactory connectionFactory) {

        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .<String, MapRecord<String, String, Object>>builder()
                        .batchSize(10)  // 한 번에 처리할 메시지 수
                        .pollTimeout(Duration.ofMillis(100))  // 폴링 타임아웃
                        .build();

        var container = StreamMessageListenerContainer.create(connectionFactory, options);

        // 응답 이벤트 Stream 구독 (가격 조회 응답)
        container.receive(
                Consumer.from(ORDER_CONSUMER_GROUP, ORDER_CONSUMER_NAME),
                StreamOffset.create(RESPONSE_EVENTS_STREAM, ReadOffset.lastConsumed()),  // 새로운 메시지만 읽기
                (org.springframework.data.redis.stream.StreamListener) orderRedisStreamListener
        );

        // 재고 이벤트 Stream 구독 (재고 차감 결과)
        container.receive(
                Consumer.from(ORDER_CONSUMER_GROUP, ORDER_CONSUMER_NAME),
                StreamOffset.create(STOCK_EVENTS_STREAM, ReadOffset.lastConsumed()),
                (org.springframework.data.redis.stream.StreamListener) orderRedisStreamListener
        );

        container.start();
        log.info("🚀 Order Redis Stream Listener Container 시작됨");

        return container;
    }
}
