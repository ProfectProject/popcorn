package com.popcorn.checkIns.config;

import com.popcorn.checkIns.event.CheckInRedisStreamListener;
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
 * CheckIns 서비스 Redis Stream 이벤트 설정
 *
 * 결제 승인, QR 관련 이벤트를 Stream으로 수신하여 체크인 로직 처리
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "redis.events.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class CheckInRedisEventConfig {

    private final CheckInRedisStreamListener checkInRedisStreamListener;
    private final RedisTemplate<String, Object> redisTemplate;

    // Stream 이름 상수
    private static final String ORDER_EVENTS_STREAM = "order-events";
    private static final String PAYMENT_EVENTS_STREAM = "payment-events";
    private static final String QR_EVENTS_STREAM = "qr-events";

    // Consumer Group 이름
    private static final String CHECKIN_CONSUMER_GROUP = "checkin-service-group";
    private static final String CHECKIN_CONSUMER_NAME = "checkin-consumer-1";

    @PostConstruct
    public void initializeStreamsAndConsumerGroups() {
        try {
            // Consumer Group 생성 (이미 존재하면 무시)
            createConsumerGroupIfNotExists(ORDER_EVENTS_STREAM);
            createConsumerGroupIfNotExists(PAYMENT_EVENTS_STREAM);
            createConsumerGroupIfNotExists(QR_EVENTS_STREAM);

            log.info("✅ CheckIns Service Redis Stream Consumer Groups 초기화 완료");
        } catch (Exception e) {
            log.warn("⚠️ Redis Stream 초기화 중 오류 (정상 동작 가능): {}", e.getMessage());
        }
    }

    private void createConsumerGroupIfNotExists(String streamName) {
        try {
            redisTemplate.opsForStream().createGroup(streamName, ReadOffset.from("0"), CHECKIN_CONSUMER_GROUP);
            log.info("📝 CheckIns Consumer Group 생성: {} - {}", streamName, CHECKIN_CONSUMER_GROUP);
        } catch (Exception e) {
            // Consumer Group이 이미 존재하는 경우 무시
            log.debug("CheckIns Consumer Group 이미 존재: {} - {}", streamName, CHECKIN_CONSUMER_GROUP);
        }
    }

    /**
     * Redis Stream 메시지 리스너 컨테이너 설정
     */
    @Bean
    public StreamMessageListenerContainer checkInStreamListenerContainer(
            RedisConnectionFactory connectionFactory) {

        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .<String, MapRecord<String, String, Object>>builder()
                        .batchSize(5)  // QR/체크인은 높은 처리량이 필요하지 않음
                        .pollTimeout(Duration.ofMillis(100))
                        .build();

        var container = StreamMessageListenerContainer.create(connectionFactory, options);

        // 주문 이벤트 Stream 구독
        container.receive(
                Consumer.from(CHECKIN_CONSUMER_GROUP, CHECKIN_CONSUMER_NAME),
                StreamOffset.create(ORDER_EVENTS_STREAM, ReadOffset.lastConsumed()),
                (org.springframework.data.redis.stream.StreamListener) checkInRedisStreamListener
        );

        // 결제 이벤트 Stream 구독 (QR 코드 생성 트리거)
        container.receive(
                Consumer.from(CHECKIN_CONSUMER_GROUP, CHECKIN_CONSUMER_NAME),
                StreamOffset.create(PAYMENT_EVENTS_STREAM, ReadOffset.lastConsumed()),
                (org.springframework.data.redis.stream.StreamListener) checkInRedisStreamListener
        );

        // QR 이벤트 Stream 구독
        container.receive(
                Consumer.from(CHECKIN_CONSUMER_GROUP, CHECKIN_CONSUMER_NAME),
                StreamOffset.create(QR_EVENTS_STREAM, ReadOffset.lastConsumed()),
                (org.springframework.data.redis.stream.StreamListener) checkInRedisStreamListener
        );

        container.start();
        log.info("🚀 CheckIns Redis Stream Listener Container 시작됨");

        return container;
    }
}