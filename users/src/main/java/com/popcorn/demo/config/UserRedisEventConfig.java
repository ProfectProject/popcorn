package com.popcorn.demo.config;

import com.popcorn.demo.event.UserRedisStreamListener;
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
 * Users 서비스 Redis Stream 이벤트 설정
 *
 * 다른 마이크로서비스에서 발생한 이벤트를 Stream으로 수신하여 사용자 관련 로직 처리
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "redis.events.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class UserRedisEventConfig {

    private final UserRedisStreamListener userRedisStreamListener;
    private final RedisTemplate<String, Object> redisTemplate;

    // Stream 이름 상수
    private static final String ORDER_EVENTS_STREAM = "order-events";
    private static final String PAYMENT_EVENTS_STREAM = "payment-events";
    private static final String USER_EVENTS_STREAM = "user-events";
    private static final String USER_ADDRESS_EVENTS_STREAM = "user-address-events";

    // Consumer Group 이름
    private static final String USER_CONSUMER_GROUP = "user-service-group";
    private static final String USER_CONSUMER_NAME = "user-consumer-1";

    @PostConstruct
    public void initializeStreamsAndConsumerGroups() {
        try {
            // Consumer Group 생성 (이미 존재하면 무시)
            createConsumerGroupIfNotExists(ORDER_EVENTS_STREAM);
            createConsumerGroupIfNotExists(PAYMENT_EVENTS_STREAM);
            createConsumerGroupIfNotExists(USER_EVENTS_STREAM);
            createConsumerGroupIfNotExists(USER_ADDRESS_EVENTS_STREAM);

            log.info("✅ Users Service Redis Stream Consumer Groups 초기화 완료");
        } catch (Exception e) {
            log.warn("⚠️ Redis Stream 초기화 중 오류 (정상 동작 가능): {}", e.getMessage());
        }
    }

    private void createConsumerGroupIfNotExists(String streamName) {
        try {
            redisTemplate.opsForStream().createGroup(streamName, ReadOffset.from("0"), USER_CONSUMER_GROUP);
            log.info("📝 User Consumer Group 생성: {} - {}", streamName, USER_CONSUMER_GROUP);
        } catch (Exception e) {
            // Consumer Group이 이미 존재하는 경우 무시
            log.debug("User Consumer Group 이미 존재: {} - {}", streamName, USER_CONSUMER_GROUP);
        }
    }

    /**
     * Redis Stream 메시지 리스너 컨테이너 설정
     */
    @Bean
    public StreamMessageListenerContainer userStreamListenerContainer(
            RedisConnectionFactory connectionFactory) {

        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .<String, MapRecord<String, String, Object>>builder()
                        .batchSize(5)  // 사용자 관련 이벤트 처리량
                        .pollTimeout(Duration.ofMillis(100))
                        .build();

        var container = StreamMessageListenerContainer.create(connectionFactory, options);

        // 주문 이벤트 Stream 구독 (사용자 통계, 알림 등)
        container.receive(
                Consumer.from(USER_CONSUMER_GROUP, USER_CONSUMER_NAME),
                StreamOffset.create(ORDER_EVENTS_STREAM, ReadOffset.lastConsumed()),
                (org.springframework.data.redis.stream.StreamListener) userRedisStreamListener
        );

        // 결제 이벤트 Stream 구독 (사용자 결제 내역, 포인트 적립 등)
        container.receive(
                Consumer.from(USER_CONSUMER_GROUP, USER_CONSUMER_NAME),
                StreamOffset.create(PAYMENT_EVENTS_STREAM, ReadOffset.lastConsumed()),
                (org.springframework.data.redis.stream.StreamListener) userRedisStreamListener
        );

        // 사용자 이벤트 Stream 구독
        container.receive(
                Consumer.from(USER_CONSUMER_GROUP, USER_CONSUMER_NAME),
                StreamOffset.create(USER_EVENTS_STREAM, ReadOffset.lastConsumed()),
                (org.springframework.data.redis.stream.StreamListener) userRedisStreamListener
        );

        // 사용자 주소 이벤트 Stream 구독 (Order 서비스의 주소 조회 요청 처리)
        container.receive(
                Consumer.from(USER_CONSUMER_GROUP, USER_CONSUMER_NAME),
                StreamOffset.create(USER_ADDRESS_EVENTS_STREAM, ReadOffset.lastConsumed()),
                (org.springframework.data.redis.stream.StreamListener) userRedisStreamListener
        );

        container.start();
        log.info("🚀 Users Redis Stream Listener Container 시작됨");

        return container;
    }
}