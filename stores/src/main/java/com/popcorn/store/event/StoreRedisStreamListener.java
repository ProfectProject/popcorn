package com.popcorn.store.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import com.popcorn.store.domain.goods.entity.ReservationType;
import com.popcorn.store.domain.goods.entity.GoodsVariant;
import com.popcorn.store.domain.goods.service.GoodsService;
import com.popcorn.store.event.order.StockDeductionSuccessEvent;
import com.popcorn.store.event.order.StockDeductionFailedEvent;
import com.popcorn.store.domain.goods.service.GoodsOrderReservationService;
import com.popcorn.store.domain.goods.repository.GoodsVariantRepository;
import com.popcorn.store.domain.popup.entity.PopupSchedule;
import com.popcorn.store.domain.popup.repository.owner.jpa.JpaOwnerPopupRepository;
import com.popcorn.store.domain.store.repository.jpa.JpaStoreRepository;
import com.popcorn.store.domain.popup.repository.owner.jpa.JpaOwnerPopupScheduleRepository;
import com.popcorn.store.event.payment.InventoryConfirmationRequestedEvent;
import com.popcorn.store.event.order.OrderPaidEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Store 서비스 Redis Stream 이벤트 리스너
 * - Redis Stream 메시지 수신 및 처리
 * - Consumer Group 기반 메시지 처리
 * - 메시지 ACK 자동 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoreRedisStreamListener implements StreamListener<String, MapRecord<String, String, Object>> {

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final GoodsOrderReservationService reservationService;
    private final GoodsService goodsService;
    private final StoreRedisEventPublisher storeRedisEventPublisher;
    private final GoodsVariantRepository goodsVariantRepository;
    private final JpaOwnerPopupScheduleRepository popupScheduleRepository;
    private final JpaOwnerPopupRepository popupRepository;
    private final JpaStoreRepository storeRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(MapRecord<String, String, Object> record) {
        try {
            String streamName = record.getStream();
            String recordId = record.getId().getValue();
            Map<String, Object> values = record.getValue();

            log.info("🔔 [STORES] Stream 메시지 수신 - stream: {}, recordId: {}, eventType: {}",
                    streamName, recordId, values.get("eventType"));

            String eventType = (String) values.get("eventType");
            // eventType에서 따옴표 제거
            if (eventType != null) {
                eventType = eventType.trim().replaceAll("^\"|\"$", "");
            }
            handleStreamEvent(eventType, values);

            // 메시지 처리 완료 후 ACK (자동으로 처리됨)
            log.debug("✅ [STORES] 메시지 처리 완료 - stream: {}, recordId: {}", streamName, recordId);

        } catch (Exception e) {
            log.error("🚨 [STORES] Stream 메시지 처리 실패 - record: {}, error: {}",
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
                case "order-paid":
                    log.info("💳 [STORES] 주문 결제 완료 이벤트 수신");
                    publishOrderPaidEvent(values);
                    break;
                case "goods-reservation-requested":
                    log.info("📋 [STORES] 굿즈 재고 예약 요청 이벤트 수신");
                    publishGoodsReservationRequestedEvent(values);
                    break;
                case "goods-reserved":
                    log.info("✅ [STORES] 굿즈 재고 예약 성공 이벤트 수신 (내부) - orderId: {}, goodsVariantId: {}, quantity: {}개 예약 완료",
                            values.get("orderId"), values.get("goodsVariantId"), values.get("quantity"));
                    // 내부적으로 발행한 이벤트 - Order 서비스에서 처리 예정
                    break;
                case "goods-reservation-failed":
                    log.warn("❌ [STORES] 굿즈 재고 예약 실패 이벤트 수신 (내부) - orderId: {}, goodsVariantId: {}, reason: {}",
                            values.get("orderId"), values.get("goodsVariantId"), values.get("reason"));
                    // 내부적으로 발행한 이벤트 - Order 서비스에서 처리 예정
                    break;
                case "goods-reservation-cancel-requested":
                    log.info("↩️ [STORES] 굿즈 예약 취소 요청 이벤트 수신");
                    handleGoodsReservationCancelRequested(values);
                    break;
                case "stock-deduction-requested":
                    log.info("📦 [STORES] 재고 차감 요청 이벤트 수신 - orderId: {}", values.get("orderId"));
                    handleStockDeductionRequest(values);
                    break;
                case "stock-deduction-success":
                    log.info("📦✅ [STORES] 재고 차감 성공 이벤트 수신 (→Order 전송됨) - orderId: {}, orderNo: {}, stockDetails: {}",
                            values.get("orderId"), values.get("orderNo"), values.get("stockDetails"));
                    // Order 서비스로 성공 결과 전송 완료
                    break;
                case "stock-deduction-failed":
                    log.warn("📦❌ [STORES] 재고 차감 실패 이벤트 수신 (→Order 전송됨) - orderId: {}, orderNo: {}, reason: {}",
                            values.get("orderId"), values.get("orderNo"), values.get("reason"));
                    // Order 서비스로 실패 결과 전송 완료
                    break;
                case "price-lookup-requested":
                    log.info("💰 [STORES] 가격 조회 요청 이벤트 수신");
                    publishPriceLookupResponseEvent(values);
                    break;
                case "popup-info-lookup-requested":
                    log.info("🏬 [STORES] 팝업 정보 조회 요청 이벤트 수신");
                    handlePopupInfoLookupRequested(values);
                    break;
                default:
                    log.info("🔔 [STORES] 알 수 없는 이벤트 타입 - type: {}", eventType);
                    break;
            }
        } catch (Exception e) {
            log.error("🚨 [STORES] 이벤트 처리 실패 - eventType: {}, error: {}", eventType, e.getMessage(), e);
        }
    }

    private void publishOrderPaidEvent(Map<String, Object> values) {
        try {
            OrderPaidEvent event = OrderPaidEvent.builder()
                    .eventId((String) values.get("eventId"))
                    .orderId(UUID.fromString((String) values.get("orderId")))
                    .orderNo((String) values.get("orderNo"))
                    .totalAmount(Integer.parseInt((String) values.get("totalAmount")))
                    .paidAt(java.time.LocalDateTime.parse((String) values.get("paidAt")))
                    .eventTime(java.time.LocalDateTime.parse((String) values.get("eventTime")))
                    .build();

            eventPublisher.publishEvent(event);
            log.info("📨 [STORES] 주문 결제 완료 이벤트 발행 - orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("🚨 [STORES] 주문 결제 완료 이벤트 변환 실패 - values: {}, error: {}",
                    values, e.getMessage(), e);
        }
    }

    private void publishGoodsReservationRequestedEvent(Map<String, Object> values) {
        try {
            String reservationItemsJson = (String) values.get("reservationItems");

            // JSON 문자열 정규화 (Redis Stream에서 전송된 JSON 문자열 처리)
            if (reservationItemsJson != null) {
                // 1. 시작/끝 따옴표 제거
                reservationItemsJson = reservationItemsJson.trim().replaceAll("^\"|\"$", "");
                // 2. 이스케이프된 따옴표를 정상 따옴표로 변환
                reservationItemsJson = reservationItemsJson.replace("\\\"", "\"");

                log.debug("🔧 [STORES] JSON 정규화 완료 - reservationItems: {}", reservationItemsJson);
            }

            List<GoodsReservationItem> reservationItems = objectMapper.readValue(
                    reservationItemsJson, new TypeReference<List<GoodsReservationItem>>() {}
            );

            // orderId와 popupId에서 따옴표 제거
            String orderIdStr = (String) values.get("orderId");
            String popupIdStr = (String) values.get("popupId");

            if (orderIdStr != null) {
                orderIdStr = orderIdStr.trim().replaceAll("^\"|\"$", "");
            }
            if (popupIdStr != null) {
                popupIdStr = popupIdStr.trim().replaceAll("^\"|\"$", "");
            }

            UUID orderId;
            try {
                orderId = UUID.fromString(normalizeUuidString(orderIdStr));
            } catch (Exception e) {
                log.error("📦 [STORES] 재고 차감 요청 orderId UUID 파싱 실패 - orderId: {}, error: {}",
                        orderIdStr, e.getMessage(), e);
                return;
            }
            String orderNo = (String) values.get("orderNo");
            UUID popupId = (popupIdStr == null || popupIdStr.isEmpty()) ?
                    null : UUID.fromString(popupIdStr);

            if (reservationItems == null || reservationItems.isEmpty()) {
                log.warn("📋 [STORES] 굿즈 재고 예약 요청 항목이 없음 - orderId: {}", orderId);
                return;
            }

            for (GoodsReservationItem item : reservationItems) {
                if (item.goodsVariantId == null || item.quantity == null) {
                    log.warn("📋 [STORES] 굿즈 재고 예약 요청 항목 누락 - orderId: {}, item: {}",
                            orderId, item);
                    continue;
                }

                try {
                    UUID resolvedPopupId = popupId;
                    if (resolvedPopupId == null) {
                        resolvedPopupId = goodsService.resolvePopupId(item.goodsVariantId);
                    }

                    goodsService.reservationGoods(resolvedPopupId, item.goodsVariantId, item.quantity);
                    reservationService.createGoodsReservation(
                            orderId, orderNo, resolvedPopupId, item.goodsVariantId, item.quantity
                    );

                    log.info("✅ [STORES] 굿즈 재고 예약 완료 - orderId: {}, goodsVariantId: {}, qty: {}",
                            orderId, item.goodsVariantId, item.quantity);

                    storeRedisEventPublisher.publishGoodsReservedEvent(
                            orderId, resolvedPopupId, item.goodsVariantId, item.quantity
                    );

                } catch (Exception e) {
                    log.error("❌ [STORES] 굿즈 재고 예약 실패 - orderId: {}, goodsVariantId: {}, qty: {}, error: {}",
                            orderId, item.goodsVariantId, item.quantity, e.getMessage(), e);

                    storeRedisEventPublisher.publishGoodsReservationFailedEvent(
                            orderId, popupId, item.goodsVariantId, item.quantity, 0, e.getMessage()
                    );
                }
            }

        } catch (Exception e) {
            log.error("🚨 [STORES] 굿즈 재고 예약 요청 이벤트 변환 실패 - values: {}, error: {}",
                    values, e.getMessage(), e);
        }
    }

    private void publishPriceLookupResponseEvent(Map<String, Object> values) {
        try {
            String correlationId = (String) values.get("correlationId");
            String requestType = (String) values.get("requestType");

            // correlationId에서 따옴표 제거
            if (correlationId != null) {
                correlationId = correlationId.trim().replaceAll("^\"|\"$", "");
            }

            // requestType에서 따옴표 제거
            if (requestType != null) {
                requestType = requestType.trim().replaceAll("^\"|\"$", "");
            }

            if (correlationId == null || requestType == null) {
                log.warn("💰 [STORES] 가격 조회 요청 누락 - values: {}", values);
                return;
            }

            log.info("💰 [STORES] 가격 조회 요청 처리 - correlationId: {}, requestType: {}",
                    correlationId, requestType);

            if ("SESSION".equals(requestType)) {
                handleSessionPriceLookup(values);
            } else if ("GOODS".equals(requestType)) {
                handleGoodsPriceLookup(values);
            } else {
                log.warn("💰 [STORES] 지원하지 않는 가격 조회 타입 - type: \"{}\"", requestType);
                publishPriceLookupFailure(values, "지원하지 않는 가격 조회 타입");
            }

        } catch (Exception e) {
            log.error("🚨 [STORES] 가격 조회 요청 이벤트 변환 실패 - values: {}, error: {}",
                    values, e.getMessage(), e);
        }
    }

    private void handleSessionPriceLookup(Map<String, Object> values) {
        try {
            String sessionIdStr = (String) values.get("sessionId");
            String correlationId = (String) values.get("correlationId");
            String requestType = (String) values.get("requestType");

            // 따옴표 제거
            if (sessionIdStr != null) {
                sessionIdStr = sessionIdStr.trim().replaceAll("^\"|\"$", "");
            }
            if (correlationId != null) {
                correlationId = correlationId.trim().replaceAll("^\"|\"$", "");
            }
            if (requestType != null) {
                requestType = requestType.trim().replaceAll("^\"|\"$", "");
            }

            if (sessionIdStr == null || sessionIdStr.isEmpty()) {
                publishPriceLookupFailure(values, "sessionId가 없습니다.");
                return;
            }

            UUID sessionId = UUID.fromString(sessionIdStr);
            PopupSchedule schedule = popupScheduleRepository.findById(sessionId)
                    .filter(value -> value.getDeletedAt() == null)
                    .orElse(null);

            if (schedule == null || schedule.getPrice() == null) {
                publishPriceLookupFailure(values, "세션 정보를 찾을 수 없습니다.");
                return;
            }

            StoreRedisEventPublisher.PriceLookupResponseEventDto response =
                    StoreRedisEventPublisher.PriceLookupResponseEventDto.builder()
                            .eventId(UUID.randomUUID().toString())
                            .correlationId(correlationId)
                            .requestType(requestType)
                            .sessionId(sessionId)
                            .price(schedule.getPrice())
                            .success(true)
                            .message("OK")
                            .respondedAt(java.time.LocalDateTime.now())
                            .eventTime(java.time.LocalDateTime.now())
                            .build();

            storeRedisEventPublisher.publishPriceLookupResponseEvent(response);

            log.info("✅ [STORES] 세션 가격 조회 응답 완료 - sessionId: {}, price: {}원",
                    sessionId, schedule.getPrice());

        } catch (Exception e) {
            log.error("🚨 [STORES] 세션 가격 조회 처리 실패 - values: {}, error: {}",
                    values, e.getMessage(), e);
            publishPriceLookupFailure(values, "세션 가격 조회 처리 실패");
        }
    }

    private void handleGoodsPriceLookup(Map<String, Object> values) {
        try {
            String goodsIdStr = (String) values.get("goodsId");
            String correlationId = (String) values.get("correlationId");
            String requestType = (String) values.get("requestType");

            // 따옴표 제거
            if (goodsIdStr != null) {
                goodsIdStr = goodsIdStr.trim().replaceAll("^\"|\"$", "");
            }
            if (correlationId != null) {
                correlationId = correlationId.trim().replaceAll("^\"|\"$", "");
            }
            if (requestType != null) {
                requestType = requestType.trim().replaceAll("^\"|\"$", "");
            }

            if (goodsIdStr == null || goodsIdStr.isEmpty()) {
                publishPriceLookupFailure(values, "goodsId가 없습니다.");
                return;
            }

            log.info("💰 [STORES] 굿즈 가격 조회 시작 - goodsId: {}, correlationId: {}",
                    goodsIdStr, correlationId);

            UUID goodsId = UUID.fromString(goodsIdStr);
            GoodsVariant variant = goodsVariantRepository.findById(goodsId)
                    .filter(value -> value.getDeletedAt() == null)
                    .orElse(null);

            if (variant == null) {
                log.warn("💰 [STORES] 굿즈 정보 없음 - goodsId: {}", goodsId);
                publishPriceLookupFailure(values, "굿즈 정보를 찾을 수 없습니다.");
                return;
            }

            StoreRedisEventPublisher.PriceLookupResponseEventDto response =
                    StoreRedisEventPublisher.PriceLookupResponseEventDto.builder()
                            .eventId(UUID.randomUUID().toString())
                            .correlationId(correlationId)
                            .requestType(requestType)
                            .goodsVariantId(goodsId)
                            .price(variant.getGoodsPrice())
                            .stockQuantity(variant.getStock())
                            .success(true)
                            .message("OK")
                            .respondedAt(java.time.LocalDateTime.now())
                            .eventTime(java.time.LocalDateTime.now())
                            .build();

            storeRedisEventPublisher.publishPriceLookupResponseEvent(response);

            log.info("✅ [STORES] 굿즈 가격 조회 응답 완료 - goodsId: {}, price: {}원, stock: {}개",
                    goodsId, variant.getGoodsPrice(), variant.getStock());

        } catch (Exception e) {
            log.error("🚨 [STORES] 굿즈 가격 조회 처리 실패 - values: {}, error: {}",
                    values, e.getMessage(), e);
            publishPriceLookupFailure(values, "굿즈 가격 조회 처리 실패");
        }
    }

    private void handleGoodsReservationCancelRequested(Map<String, Object> values) {
        try {
            String popupIdStr = (String) values.get("popupId");
            String goodsVariantIdStr = (String) values.get("goodsVariantId");
            String quantityStr = (String) values.get("quantity");

            if (popupIdStr != null) {
                popupIdStr = popupIdStr.trim().replaceAll("^\"|\"$", "");
            }
            if (goodsVariantIdStr != null) {
                goodsVariantIdStr = goodsVariantIdStr.trim().replaceAll("^\"|\"$", "");
            }
            if (quantityStr != null) {
                quantityStr = quantityStr.trim().replaceAll("^\"|\"$", "");
            }

            if (goodsVariantIdStr == null || goodsVariantIdStr.isEmpty() || quantityStr == null || quantityStr.isEmpty()) {
                log.warn("↩️ [STORES] 굿즈 예약 취소 요청 데이터 누락 - values: {}", values);
                return;
            }

            UUID goodsVariantId = UUID.fromString(goodsVariantIdStr);
            UUID popupId = popupIdStr != null && !popupIdStr.isEmpty() ? UUID.fromString(popupIdStr) : goodsService.resolvePopupId(goodsVariantId);
            int quantity = Integer.parseInt(quantityStr);

            goodsService.cancelReservationGoods(popupId, goodsVariantId, quantity);

            log.info("↩️ [STORES] 굿즈 예약 취소 완료 - goodsVariantId: {}, quantity: {}", goodsVariantId, quantity);

        } catch (Exception e) {
            log.error("↩️ [STORES] 굿즈 예약 취소 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    private void handlePopupInfoLookupRequested(Map<String, Object> values) {
        try {
            String popupIdStr = (String) values.get("popupId");
            String correlationId = (String) values.get("correlationId");

            if (popupIdStr != null) {
                popupIdStr = popupIdStr.trim().replaceAll("^\"|\"$", "");
            }
            if (correlationId != null) {
                correlationId = correlationId.trim().replaceAll("^\"|\"$", "");
            }

            if (popupIdStr == null || popupIdStr.isEmpty() || correlationId == null || correlationId.isEmpty()) {
                log.warn("🏬 [STORES] 팝업 정보 조회 요청 누락 - values: {}", values);
                return;
            }

            UUID popupId = UUID.fromString(popupIdStr);
            var popupOpt = popupRepository.findById(popupId);
            if (popupOpt.isEmpty() || popupOpt.get().getDeletedAt() != null) {
                publishPopupInfoLookupFailure(correlationId, popupId, "팝업 정보를 찾을 수 없습니다.");
                return;
            }

            var popup = popupOpt.get();
            var storeOpt = storeRepository.findById(popup.getStoreId());

            String storeName = storeOpt.map(s -> s.getName()).orElse("");

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("eventType", "popup-info-lookup-response");
            response.put("eventId", UUID.randomUUID().toString());
            response.put("correlationId", correlationId);
            response.put("popupId", popup.getId().toString());
            response.put("success", true);
            response.put("message", "OK");
            response.put("title", popup.getTitle());
            response.put("description", popup.getDescription());
            response.put("storeId", popup.getStoreId() != null ? popup.getStoreId().toString() : "");
            response.put("storeName", storeName);
            response.put("address1", popup.getAddressRoad());
            response.put("address2", popup.getAddressDetail());
            response.put("phoneNumber", "");
            response.put("status", popup.getStatus() != null ? popup.getStatus().name() : "");
            response.put("startDate", popup.getReservationOpenAt() != null ? popup.getReservationOpenAt().toString() : "");
            response.put("endDate", "");
            response.put("respondedAt", java.time.LocalDateTime.now().toString());

            storeRedisEventPublisher.publishPopupInfoLookupResponseEvent(response);

        } catch (Exception e) {
            log.error("🏬 [STORES] 팝업 정보 조회 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
        }
    }

    private void publishPopupInfoLookupFailure(String correlationId, UUID popupId, String message) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("eventType", "popup-info-lookup-response");
        response.put("eventId", UUID.randomUUID().toString());
        response.put("correlationId", correlationId);
        response.put("popupId", popupId.toString());
        response.put("success", false);
        response.put("message", message);
        response.put("respondedAt", java.time.LocalDateTime.now().toString());
        storeRedisEventPublisher.publishPopupInfoLookupResponseEvent(response);
    }

    private void publishPriceLookupFailure(Map<String, Object> values, String reason) {
        try {
            String sessionIdStr = (String) values.get("sessionId");
            String goodsIdStr = (String) values.get("goodsId");  // goodsVariantId -> goodsId 수정
            String correlationId = (String) values.get("correlationId");
            String requestType = (String) values.get("requestType");

            // 따옴표 제거
            if (correlationId != null) {
                correlationId = correlationId.trim().replaceAll("^\"|\"$", "");
            }
            if (requestType != null) {
                requestType = requestType.trim().replaceAll("^\"|\"$", "");
            }
            if (sessionIdStr != null) {
                sessionIdStr = sessionIdStr.trim().replaceAll("^\"|\"$", "");
            }
            if (goodsIdStr != null) {
                goodsIdStr = goodsIdStr.trim().replaceAll("^\"|\"$", "");
            }

            // UUID 안전 파싱
            UUID sessionId = null;
            if (sessionIdStr != null && !sessionIdStr.isEmpty() && !sessionIdStr.equals("null")) {
                try {
                    sessionId = UUID.fromString(sessionIdStr);
                } catch (IllegalArgumentException e) {
                    log.warn("💰 [STORES] 유효하지 않은 sessionId UUID - sessionId: {}", sessionIdStr);
                }
            }

            UUID goodsVariantId = null;
            if (goodsIdStr != null && !goodsIdStr.isEmpty() && !goodsIdStr.equals("null")) {
                try {
                    goodsVariantId = UUID.fromString(goodsIdStr);
                } catch (IllegalArgumentException e) {
                    log.warn("💰 [STORES] 유효하지 않은 goodsId UUID - goodsId: {}", goodsIdStr);
                }
            }

            StoreRedisEventPublisher.PriceLookupResponseEventDto response =
                    StoreRedisEventPublisher.PriceLookupResponseEventDto.builder()
                            .eventId(UUID.randomUUID().toString())
                            .correlationId(correlationId)
                            .requestType(requestType)
                            .sessionId(sessionId)
                            .goodsVariantId(goodsVariantId)
                            .success(false)
                            .message(reason)
                            .respondedAt(java.time.LocalDateTime.now())
                            .eventTime(java.time.LocalDateTime.now())
                            .build();

            storeRedisEventPublisher.publishPriceLookupResponseEvent(response);

        } catch (Exception e) {
            log.error("🚨 [STORES] 가격 조회 실패 이벤트 발행 실패 - error: {}", e.getMessage(), e);
        }
    }

    /**
     * 재고 차감 요청 이벤트 처리
     */
    private void handleStockDeductionRequest(Map<String, Object> values) {
        try {
            // 이벤트 데이터 추출
            String orderIdStr = normalizeQuotedString((String) values.get("orderId"));
            String orderNo = normalizeQuotedString((String) values.get("orderNo"));
            String itemsJson = normalizeQuotedString((String) values.get("items"));

            if (orderIdStr == null || orderNo == null || itemsJson == null) {
                log.error("📦 [STORES] 재고 차감 요청 필수 데이터 누락 - orderId: {}, orderNo: {}, items: {}",
                         orderIdStr, orderNo, itemsJson);
                return;
            }

            UUID orderId = UUID.fromString(normalizeUuidString(orderIdStr));
            log.info("📦 [STORES] 재고 차감 요청 처리 시작 - orderId: {}, orderNo: {}", orderId, orderNo);

            // items JSON 역직렬화
            List<Map<String, Object>> itemsList;
            try {
                itemsList = objectMapper.readValue(itemsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            } catch (Exception e) {
                log.error("📦 [STORES] 재고 차감 요청 JSON 파싱 실패 - orderId: {}, itemsJson: {}, error: {}",
                         orderId, itemsJson, e.getMessage(), e);
                publishStockDeductionFailure(orderId, orderNo, null, "JSON 파싱 실패", e.getMessage());
                return;
            }

            if (itemsList.isEmpty()) {
                log.warn("📦 [STORES] 재고 차감 요청 항목이 비어있음 - orderId: {}", orderId);
                publishStockDeductionFailure(orderId, orderNo, null, "차감 항목 없음", "items 리스트가 비어있습니다");
                return;
            }

            // popupId는 첫 번째 항목에서 추출 (모든 항목이 같은 팝업에 속함)
            UUID popupId = null;
            StringBuilder stockDetails = new StringBuilder();
            boolean allSuccess = true;
            String failureReason = "";

            // 각 항목에 대해 재고 차감 처리
            for (Map<String, Object> item : itemsList) {
                try {
                    String goodsVariantIdStr = (String) item.get("goodsVariantId");
                    Integer quantity = null;

                    Object quantityObj = item.get("quantity");
                    if (quantityObj instanceof Integer) {
                        quantity = (Integer) quantityObj;
                    } else if (quantityObj instanceof String) {
                        quantity = Integer.parseInt((String) quantityObj);
                    }

                    if (goodsVariantIdStr == null || quantity == null || quantity <= 0) {
                        log.error("📦 [STORES] 재고 차감 항목 데이터 오류 - goodsVariantId: {}, quantity: {}",
                                 goodsVariantIdStr, quantity);
                        allSuccess = false;
                        failureReason = "항목 데이터 오류";
                        break;
                    }

                    UUID goodsVariantId;
                    try {
                        goodsVariantId = UUID.fromString(normalizeUuidString(goodsVariantIdStr));
                    } catch (Exception e) {
                        log.error("📦 [STORES] 재고 차감 항목 goodsVariantId UUID 파싱 실패 - goodsVariantId: {}, error: {}",
                                goodsVariantIdStr, e.getMessage(), e);
                        allSuccess = false;
                        failureReason = "goodsVariantId UUID 파싱 실패";
                        break;
                    }
                    String productName = (String) item.get("productName");
                    String variantName = (String) item.get("variantName");

                    log.info("📦 [STORES] 재고 차감 처리 - goodsVariantId: {}, quantity: {}, product: {}",
                            goodsVariantId, quantity, productName);

                    // 실제 재고 차감 처리 (GoodsService 호출)
                    // popupId는 실제로는 별도 조회가 필요하지만, 임시로 goodsVariantId를 사용
                    if (popupId == null) {
                        // 첫 번째 항목에서 popupId를 결정 (실제로는 goodsVariant에서 조회해야 함)
                        popupId = goodsVariantId; // 임시 처리
                    }

                    var stockResponse = goodsService.completeReservationGoods(popupId, goodsVariantId, quantity);

                    // 성공 정보 누적
                    if (stockDetails.length() > 0) {
                        stockDetails.append(", ");
                    }
                    stockDetails.append(String.format("%s(%s):%d개->재고:%d",
                        productName != null ? productName : "상품",
                        variantName != null ? variantName : "기본",
                        quantity,
                        stockResponse.getStock()));

                    log.info("📦 [STORES] 재고 차감 성공 - goodsVariantId: {}, quantity: {}, currentStock: {}",
                            goodsVariantId, quantity, stockResponse.getStock());

                } catch (Exception e) {
                    log.error("📦 [STORES] 재고 차감 실패 - goodsVariantId: {}, error: {}",
                             item.get("goodsVariantId"), e.getMessage(), e);
                    allSuccess = false;
                    failureReason = e.getMessage();
                    break;
                }
            }

            // 결과에 따라 성공/실패 이벤트 발행
            if (allSuccess) {
                publishStockDeductionSuccess(orderId, orderNo, popupId, stockDetails.toString());
            } else {
                publishStockDeductionFailure(orderId, orderNo, popupId, failureReason, stockDetails.toString());
            }

        } catch (Exception e) {
            log.error("📦 [STORES] 재고 차감 요청 처리 실패 - values: {}, error: {}", values, e.getMessage(), e);
            try {
                String orderIdStr = (String) values.get("orderId");
                String orderNo = (String) values.get("orderNo");
                if (orderIdStr != null && orderNo != null) {
                    UUID safeOrderId;
                    try {
                        safeOrderId = UUID.fromString(normalizeUuidString(orderIdStr));
                    } catch (Exception ignored) {
                        return;
                    }
                    publishStockDeductionFailure(safeOrderId, orderNo, null,
                            "시스템 오류", e.getMessage());
                }
            } catch (Exception ignored) {
                // 추가 에러 발생 시 무시
            }
        }
    }

    /**
     * 재고 차감 성공 이벤트 발행
     */
    private void publishStockDeductionSuccess(UUID orderId, String orderNo, UUID popupId, String stockDetails) {
        try {
            log.info("✅ [STORES] 재고 차감 성공 이벤트 발행 - orderId: {}, details: {}", orderId, stockDetails);

            StockDeductionSuccessEvent event = StockDeductionSuccessEvent.create(
                orderId, orderNo, popupId, stockDetails);

            storeRedisEventPublisher.publishStockDeductionSuccessEvent(event);

            log.info("✅ [STORES] 재고 차감 성공 이벤트 발행 완료 - orderId: {}, eventId: {}",
                    orderId, event.getEventId());
        } catch (Exception e) {
            log.error("🚨 [STORES] 재고 차감 성공 이벤트 발행 실패 - orderId: {}, error: {}",
                     orderId, e.getMessage(), e);
        }
    }

    /**
     * 재고 차감 실패 이벤트 발행
     */
    private void publishStockDeductionFailure(UUID orderId, String orderNo, UUID popupId,
                                            String reason, String details) {
        try {
            log.warn("❌ [STORES] 재고 차감 실패 이벤트 발행 - orderId: {}, reason: {}, details: {}",
                    orderId, reason, details);

            StockDeductionFailedEvent event = StockDeductionFailedEvent.forSystemError(
                orderId, orderNo, popupId, String.format("%s - %s", reason, details));

            storeRedisEventPublisher.publishStockDeductionFailedEvent(event);

            log.info("❌ [STORES] 재고 차감 실패 이벤트 발행 완료 - orderId: {}, eventId: {}",
                    orderId, event.getEventId());
        } catch (Exception e) {
            log.error("🚨 [STORES] 재고 차감 실패 이벤트 발행 실패 - orderId: {}, error: {}",
                     orderId, e.getMessage(), e);
        }
    }

    private static class GoodsReservationItem {
        public UUID goodsVariantId;
        public Integer quantity;
    }

    private String normalizeUuidString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2) {
            char first = trimmed.charAt(0);
            char last = trimmed.charAt(trimmed.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }
        return trimmed;
    }

    private String normalizeQuotedString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2) {
            char first = trimmed.charAt(0);
            char last = trimmed.charAt(trimmed.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                try {
                    // Decode JSON-escaped strings like "\"O2026\"" or "\"[{\\\"a\\\":1}]\""
                    return objectMapper.readValue(trimmed, String.class).trim();
                } catch (Exception ignored) {
                    return trimmed.substring(1, trimmed.length() - 1).trim();
                }
            }
        }
        return trimmed;
    }
}
