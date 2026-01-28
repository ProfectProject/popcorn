package com.popcorn.demo.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * User 서비스 Redis Stream 이벤트 리스너
 * - 주문, 결제 관련 이벤트를 Stream으로 수신
 * - 사용자 통계, 알림, 포인트 적립 등 처리
 * - Consumer Group 기반 메시지 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserRedisStreamListener implements StreamListener<String, MapRecord<String, String, Object>> {

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    private final com.popcorn.users.users.repository.UserAddressRepository userAddressRepository;

    @Override
    public void onMessage(MapRecord<String, String, Object> record) {
        try {
            String streamName = record.getStream();
            String recordId = record.getId().getValue();
            Map<String, Object> values = record.getValue();

            log.info("🔔 [USERS] Stream 메시지 수신 - stream: {}, recordId: {}, eventType: {}",
                    streamName, recordId, values.get("eventType"));

            String eventType = (String) values.get("eventType");
            // eventType에서 따옴표 제거
            if (eventType != null) {
                eventType = eventType.trim().replaceAll("^\"|\"$", "");
            }
            handleStreamEvent(eventType, values);

            // 메시지 처리 완료 후 ACK (자동으로 처리됨)
            log.debug("✅ [USERS] 메시지 처리 완료 - stream: {}, recordId: {}", streamName, recordId);

        } catch (Exception e) {
            log.error("🚨 [USERS] Stream 메시지 처리 실패 - record: {}, error: {}",
                    record, e.getMessage(), e);
            // TODO: 실패한 메시지를 DLQ(Dead Letter Queue)로 이동하거나 재시도 로직 구현
        }
    }

    /**
     * Stream 이벤트 타입별 처리
     */
    private void handleStreamEvent(String eventType, Map<String, Object> values) {
        try {
            switch (eventType) {
                // 주문 관련 이벤트
                case "order-created":
                    log.info("📦 [USERS] 주문 생성 이벤트 수신 - orderId: {}, userId: {}",
                            values.get("orderId"), values.get("userId"));
                    // 사용자 주문 통계 업데이트
                    handleOrderCreated(values);
                    break;
                case "order-paid":
                    log.info("💳 [USERS] 주문 결제 완료 이벤트 수신 - orderId: {}, userId: {}",
                            values.get("orderId"), values.get("userId"));
                    // 사용자 구매 이력 업데이트
                    handleOrderPaid(values);
                    break;

                // 결제 관련 이벤트
                case "payment-approved":
                    log.info("✅ [USERS] 결제 승인 이벤트 수신 - paymentId: {}", values.get("paymentId"));
                    // 포인트 적립, 사용자 알림 등
                    handlePaymentApproved(values);
                    break;
                case "payment-failed":
                    log.warn("❌ [USERS] 결제 실패 이벤트 수신 - paymentId: {}", values.get("paymentId"));
                    // 결제 실패 알림
                    handlePaymentFailed(values);
                    break;
                case "payment-completed":
                    log.info("✅ [USERS] 결제 완료 이벤트 수신 - paymentId: {}", values.get("paymentId"));
                    // 최종 포인트 적립
                    handlePaymentCompleted(values);
                    break;

                // 사용자 관련 이벤트
                case "user-created":
                    log.info("👤 [USERS] 사용자 생성 이벤트 수신 - userId: {}", values.get("userId"));
                    handleUserCreated(values);
                    break;
                case "user-updated":
                    log.info("👤 [USERS] 사용자 업데이트 이벤트 수신 - userId: {}", values.get("userId"));
                    handleUserUpdated(values);
                    break;
                case "user-address-lookup-requested":
                    log.info("🏠 [USERS] 사용자 주소 조회 요청 수신 - userId: {}", values.get("userId"));
                    handleUserAddressLookupRequest(values);
                    break;

                default:
                    log.info("🔔 [USERS] 기타 이벤트 수신 - eventType: {}", eventType);
                    break;
            }
        } catch (Exception e) {
            log.error("🚨 [USERS] 이벤트 처리 실패 - eventType: {}, error: {}", eventType, e.getMessage(), e);
        }
    }

    /**
     * 주문 생성 이벤트 처리
     */
    private void handleOrderCreated(Map<String, Object> values) {
        try {
            String userId = (String) values.get("userId");
            String orderId = (String) values.get("orderId");

            log.info("📊 [USERS] 사용자 주문 통계 업데이트 - userId: {}, orderId: {}", userId, orderId);
            // 사용자 주문 통계, 활동 로그 업데이트 등

        } catch (Exception e) {
            log.error("🚨 [USERS] 주문 생성 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    /**
     * 주문 결제 완료 이벤트 처리
     */
    private void handleOrderPaid(Map<String, Object> values) {
        try {
            String userId = (String) values.get("userId");
            String orderId = (String) values.get("orderId");
            String totalAmount = (String) values.get("totalAmount");

            log.info("💰 [USERS] 사용자 구매 이력 업데이트 - userId: {}, amount: {}", userId, totalAmount);
            // 사용자 구매 이력, 등급 업데이트 등

        } catch (Exception e) {
            log.error("🚨 [USERS] 주문 결제 완료 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    /**
     * 결제 승인 이벤트 처리
     */
    private void handlePaymentApproved(Map<String, Object> values) {
        try {
            String paymentId = (String) values.get("paymentId");
            String amount = (String) values.get("amount");

            log.info("🎁 [USERS] 포인트 적립 처리 - paymentId: {}, amount: {}", paymentId, amount);
            // 포인트 적립, 푸시 알림 발송 등

        } catch (Exception e) {
            log.error("🚨 [USERS] 결제 승인 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    /**
     * 결제 실패 이벤트 처리
     */
    private void handlePaymentFailed(Map<String, Object> values) {
        try {
            String paymentId = (String) values.get("paymentId");
            String reason = (String) values.get("reason");

            log.info("📱 [USERS] 결제 실패 알림 발송 - paymentId: {}, reason: {}", paymentId, reason);
            // 결제 실패 푸시 알림, 이메일 발송 등

        } catch (Exception e) {
            log.error("🚨 [USERS] 결제 실패 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    /**
     * 결제 완료 이벤트 처리
     */
    private void handlePaymentCompleted(Map<String, Object> values) {
        try {
            String paymentId = (String) values.get("paymentId");

            log.info("🏆 [USERS] 최종 포인트 적립 - paymentId: {}", paymentId);
            // 최종 포인트 적립, 사용자 등급 업데이트 등

        } catch (Exception e) {
            log.error("🚨 [USERS] 결제 완료 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    /**
     * 사용자 생성 이벤트 처리
     */
    private void handleUserCreated(Map<String, Object> values) {
        try {
            String userId = (String) values.get("userId");

            log.info("🎉 [USERS] 신규 사용자 환영 처리 - userId: {}", userId);
            // 환영 이메일, 첫 가입 혜택 등

        } catch (Exception e) {
            log.error("🚨 [USERS] 사용자 생성 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    /**
     * 사용자 업데이트 이벤트 처리
     */
    private void handleUserUpdated(Map<String, Object> values) {
        try {
            String userId = (String) values.get("userId");

            log.info("🔄 [USERS] 사용자 정보 업데이트 처리 - userId: {}", userId);
            // 프로필 변경 알림 등

        } catch (Exception e) {
            log.error("🚨 [USERS] 사용자 업데이트 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    /**
     * 사용자 주소 조회 요청 처리
     */
    private void handleUserAddressLookupRequest(Map<String, Object> values) {
        try {
            String userId = (String) values.get("userId");
            String correlationId = (String) values.get("correlationId");
            String requestType = (String) values.get("requestType");

            // 따옴표 제거
            if (userId != null) {
                userId = userId.trim().replaceAll("^\"|\"$", "");
            }
            if (correlationId != null) {
                correlationId = correlationId.trim().replaceAll("^\"|\"$", "");
            }
            if (requestType != null) {
                requestType = requestType.trim().replaceAll("^\"|\"$", "");
            }

            log.info("🏠 [USERS] 사용자 주소 조회 요청 처리 - userId: {}, correlationId: {}, requestType: {}",
                    userId, correlationId, requestType);

            if (userId == null || correlationId == null || requestType == null) {
                log.warn("🏠 [USERS] 사용자 주소 조회 요청 필수 데이터 누락 - userId: {}, correlationId: {}, requestType: {}",
                        userId, correlationId, requestType);
                publishUserAddressLookupResponse(correlationId, requestType, userId, false, "필수 데이터 누락",
                    null, null, null, null, null);
                return;
            }

            // 실제 사용자 주소 조회
            Long userIdLong = Long.parseLong(userId);
            var addresses = userAddressRepository.findByUserUserId(userIdLong);

            // isDefault = true인 기본 주소 찾기
            var defaultAddress = addresses.stream()
                    .filter(addr -> Boolean.TRUE.equals(addr.getIsDefault()) && addr.getDeletedAt() == null)
                    .findFirst();

            if (defaultAddress.isPresent()) {
                log.info("🏠 [USERS] 기본 배송지 조회 성공 - userId: {}, addressId: {}, addressName: {}",
                        userId, defaultAddress.get().getId(), defaultAddress.get().getAddrName());

                // 각 주소 필드를 개별적으로 전송
                publishUserAddressLookupResponse(correlationId, requestType, userId, true, "OK",
                    defaultAddress.get().getId().toString(),
                    defaultAddress.get().getAddrName() != null ? defaultAddress.get().getAddrName() : "",
                    defaultAddress.get().getAddress1(),
                    defaultAddress.get().getAddress2() != null ? defaultAddress.get().getAddress2() : "",
                    defaultAddress.get().getPostalCode() != null ? defaultAddress.get().getPostalCode() : "");
            } else {
                log.warn("🏠 [USERS] 기본 배송지 없음 - userId: {}, 전체 주소 개수: {}", userId, addresses.size());
                publishUserAddressLookupResponse(correlationId, requestType, userId, false, "기본 배송지가 설정되어 있지 않습니다",
                    null, null, null, null, null);
            }

        } catch (Exception e) {
            log.error("🚨 [USERS] 사용자 주소 조회 요청 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);

            String correlationId = (String) values.get("correlationId");
            String requestType = (String) values.get("requestType");
            String userId = (String) values.get("userId");

            publishUserAddressLookupResponse(correlationId, requestType, userId, false, "시스템 오류",
                null, null, null, null, null);
        }
    }

    /**
     * 사용자 주소 조회 응답 발행
     */
    private void publishUserAddressLookupResponse(String correlationId, String requestType, String userId,
                                                boolean success, String message,
                                                String addressId, String addrName, String address1,
                                                String address2, String postalCode) {
        try {
            // Redis Stream에 응답 이벤트 발행
            Map<String, String> eventData = new java.util.HashMap<>();
            eventData.put("eventType", "user-address-lookup-response");
            eventData.put("eventId", java.util.UUID.randomUUID().toString());
            eventData.put("correlationId", correlationId != null ? correlationId : "");
            eventData.put("requestType", requestType != null ? requestType : "");
            eventData.put("userId", userId != null ? userId : "");
            eventData.put("success", String.valueOf(success));
            eventData.put("message", message != null ? message : "");
            eventData.put("respondedAt", java.time.LocalDateTime.now().toString());
            eventData.put("eventTime", java.time.LocalDateTime.now().toString());

            // 개별 주소 필드 추가 (성공한 경우에만)
            if (success && addressId != null) {
                eventData.put("addressId", addressId);
                eventData.put("addrName", addrName != null ? addrName : "");
                eventData.put("address1", address1 != null ? address1 : "");
                eventData.put("address2", address2 != null ? address2 : "");
                eventData.put("postalCode", postalCode != null ? postalCode : "");
                eventData.put("isDefault", "true");
            }

            org.springframework.data.redis.connection.stream.StringRecord record =
                    org.springframework.data.redis.connection.stream.StreamRecords.string(eventData)
                            .withStreamKey("order-address-response");  // Order 서비스에서 수신할 Stream

            String recordId = redisTemplate.opsForStream().add(record).getValue();

            log.info("✅ [USERS] 사용자 주소 조회 응답 발행 완료 - correlationId: {}, userId: {}, success: {}, recordId: {}",
                    correlationId, userId, success, recordId);

        } catch (Exception e) {
            log.error("🚨 [USERS] 사용자 주소 조회 응답 발행 실패 - correlationId: {}, error: {}",
                    correlationId, e.getMessage(), e);
        }
    }
}