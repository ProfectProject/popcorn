package com.popcorn.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.common.cache.IdempotencyService;
import com.popcorn.order.dto.user.UserAddressResponse;
import com.popcorn.order.entity.OrderItemType;
import com.popcorn.order.entity.OrderStatus;
import com.popcorn.order.event.PopupInfoLookupResponseEvent;
import com.popcorn.order.repository.OrderRepository;
import com.popcorn.order.service.OrderCommandService;
import com.popcorn.order.service.OrderPopupLookupService;
import com.popcorn.order.service.OrderPriceLookupService;
import com.popcorn.order.service.OrderUserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Order 서비스 Redis Stream 이벤트 리스너
 *
 * 다른 마이크로서비스로부터 오는 응답 이벤트를 수신하고 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderRedisStreamListener implements StreamListener<String, MapRecord<String, String, Object>> {

    private final OrderPriceLookupService orderPriceLookupService;
    private final OrderUserLookupService orderUserLookupService;
    private final OrderCommandService orderCommandService;
    private final OrderRepository orderRepository;
    private final OrderPopupLookupService orderPopupLookupService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(MapRecord<String, String, Object> record) {
        try {
            String streamName = record.getStream();
            String recordId = record.getId().getValue();
            Map<String, Object> values = record.getValue();

            log.info("🔔 [ORDER] Stream 메시지 수신 - stream: {}, recordId: {}, eventType: {}",
                    streamName, recordId, values.get("eventType"));

            String eventType = (String) values.get("eventType");
            // eventType에서 따옴표 제거
            if (eventType != null) {
                eventType = eventType.trim().replaceAll("^\"|\"$", "");
            }
            handleStreamEvent(eventType, values);

            log.debug("✅ [ORDER] 메시지 처리 완료 - stream: {}, recordId: {}", streamName, recordId);

        } catch (Exception e) {
            log.error("🚨 [ORDER] Stream 메시지 처리 실패 - record: {}, error: {}",
                    record, e.getMessage(), e);
        }
    }

    /**
     * Stream 이벤트 타입별 처리
     */
    private void handleStreamEvent(String eventType, Map<String, Object> values) {
        try {
            switch (eventType) {
                case "price-lookup-response":
                    log.info("💰 [ORDER] 가격 조회 응답 이벤트 수신");
                    handlePriceLookupResponse(values);
                    break;
                case "user-address-lookup-response":
                    log.info("🏠 [ORDER] 사용자 주소 조회 응답 이벤트 수신");
                    handleUserAddressLookupResponse(values);
                    break;
                case "popup-info-lookup-response":
                    log.info("🏬 [ORDER] 팝업 정보 조회 응답 이벤트 수신");
                    handlePopupInfoLookupResponse(values);
                    break;
                case "payment-approved":
                    log.info("💳 [ORDER] 결제 승인 이벤트 수신");
                    handlePaymentApproved(values);
                    break;
                case "payment-completed":
                    log.info("✅ [ORDER] 결제 완료 이벤트 수신");
                    handlePaymentCompleted(values);
                    break;
                case "payment-failed":
                    log.info("❌ [ORDER] 결제 실패 이벤트 수신");
                    handlePaymentFailed(values);
                    break;
                case "stock-deduction-success":
                    log.info("📦✅ [ORDER] 재고 차감 성공 이벤트 수신");
                    handleStockDeductionSuccess(values);
                    break;
                case "stock-deduction-failed":
                    log.info("📦❌ [ORDER] 재고 차감 실패 이벤트 수신");
                    handleStockDeductionFailed(values);
                    break;
                case "stock-deduction-requested":
                    // Order가 발행한 이벤트이므로 수신 시 무시
                    log.debug("🔕 [ORDER] 재고 차감 요청 이벤트 무시 - eventId: {}",
                            values.get("eventId"));
                    break;
                case "goods-reserved":
                    log.info("📦✅ [ORDER] 굿즈 예약 성공 이벤트 수신");
                    handleGoodsReserved(values);
                    break;
                case "goods-reservation-failed":
                    log.info("📦❌ [ORDER] 굿즈 예약 실패 이벤트 수신");
                    handleGoodsReservationFailed(values);
                    break;
                case "goods-reservation-cancel-requested":
                    // Order가 발행한 이벤트이므로 수신 시 무시
                    log.debug("🔕 [ORDER] 굿즈 예약 취소 요청 이벤트 무시 - eventId: {}",
                            values.get("eventId"));
                    break;
                case "payment-create-requested":
                    // Order가 발행한 이벤트이므로 수신 시 무시
                    log.debug("🔕 [ORDER] 결제 생성 요청 이벤트 무시 - eventId: {}",
                            values.get("eventId"));
                    break;
                default:
                    log.debug("🔔 [ORDER] 알 수 없는 이벤트 타입 - type: {}", eventType);
                    break;
            }
        } catch (Exception e) {
            log.error("🚨 [ORDER] 이벤트 처리 실패 - eventType: {}, error: {}", eventType, e.getMessage(), e);
        }
    }

    /**
     * 가격 조회 응답 처리
     */
    private void handlePriceLookupResponse(Map<String, Object> values) {
        try {
            String correlationId = (String) values.get("correlationId");
            String requestType = (String) values.get("requestType");
            String successStr = (String) values.get("success");
            String message = (String) values.get("message");

            // 따옴표 제거
            if (correlationId != null) {
                correlationId = correlationId.trim().replaceAll("^\"|\"$", "");
            }
            if (requestType != null) {
                requestType = requestType.trim().replaceAll("^\"|\"$", "");
            }
            if (message != null) {
                message = message.trim().replaceAll("^\"|\"$", "");
            }
            if (successStr != null) {
                successStr = successStr.trim().replaceAll("^\"|\"$", "");
            }

            boolean success = Boolean.parseBoolean(successStr);

            if (correlationId == null || requestType == null) {
                log.warn("💰 [ORDER] 가격 조회 응답 필수 데이터 누락 - correlationId: {}, requestType: {}",
                        correlationId, requestType);
                return;
            }

            log.info("💰 [ORDER] 가격 조회 응답 처리 - correlationId: {}, type: {}, success: {}",
                    correlationId, requestType, success);

            Integer price = null;
            Integer stockQuantity = null;
            UUID sessionId = null;
            UUID goodsVariantId = null;

            if (success) {
                try {
                    String priceStr = (String) values.get("price");
                    if (priceStr != null && !priceStr.isEmpty()) {
                        priceStr = priceStr.trim().replaceAll("^\"|\"$", "");
                        if (!priceStr.isEmpty() && !priceStr.equals("null")) {
                            price = Integer.parseInt(priceStr);
                        }
                    }

                    String stockStr = (String) values.get("stockQuantity");
                    if (stockStr != null && !stockStr.isEmpty()) {
                        stockStr = stockStr.trim().replaceAll("^\"|\"$", "");
                        if (!stockStr.isEmpty() && !stockStr.equals("null")) {
                            stockQuantity = Integer.parseInt(stockStr);
                        }
                    }

                    String sessionIdStr = (String) values.get("sessionId");
                    if (sessionIdStr != null && !sessionIdStr.isEmpty()) {
                        sessionIdStr = sessionIdStr.trim().replaceAll("^\"|\"$", "");
                        if (!sessionIdStr.isEmpty() && !sessionIdStr.equals("null")) {
                            sessionId = UUID.fromString(sessionIdStr);
                        }
                    }

                    String goodsIdStr = (String) values.get("goodsId");
                    if (goodsIdStr != null && !goodsIdStr.isEmpty()) {
                        goodsIdStr = goodsIdStr.trim().replaceAll("^\"|\"$", "");
                        if (!goodsIdStr.isEmpty() && !goodsIdStr.equals("null")) {
                            goodsVariantId = UUID.fromString(goodsIdStr);
                        }
                    }

                } catch (Exception e) {
                    log.error("💰 [ORDER] 가격 조회 응답 데이터 파싱 실패 - correlationId: {}, error: {}",
                            correlationId, e.getMessage());
                    success = false;
                    message = "데이터 파싱 실패";
                }
            }

            // PriceLookupResponseEvent 생성 및 처리
            PriceLookupResponseEvent response = PriceLookupResponseEvent.builder()
                    .eventId((String) values.get("eventId"))
                    .correlationId(correlationId)
                    .requestType(requestType)
                    .sessionId(sessionId)
                    .goodsVariantId(goodsVariantId)
                    .price(price)
                    .stockQuantity(stockQuantity)
                    .success(success)
                    .message(message)
                    .respondedAt(java.time.LocalDateTime.now())
                    .build();

            // OrderPriceLookupService에 응답 전달
            orderPriceLookupService.handlePriceLookupResponse(response);

            log.info("✅ [ORDER] 가격 조회 응답 처리 완료 - correlationId: {}, price: {}원",
                    correlationId, price);

        } catch (Exception e) {
            log.error("🚨 [ORDER] 가격 조회 응답 처리 실패 - values: {}, error: {}",
                    values, e.getMessage(), e);
        }
    }

    /**
     * 사용자 주소 조회 응답 처리
     */
    private void handleUserAddressLookupResponse(Map<String, Object> values) {
        try {
            String correlationId = (String) values.get("correlationId");
            String requestType = (String) values.get("requestType");
            String successStr = (String) values.get("success");
            String message = (String) values.get("message");

            // 따옴표 제거
            if (correlationId != null) {
                correlationId = correlationId.trim().replaceAll("^\"|\"$", "");
            }
            if (requestType != null) {
                requestType = requestType.trim().replaceAll("^\"|\"$", "");
            }
            if (message != null) {
                message = message.trim().replaceAll("^\"|\"$", "");
            }
            if (successStr != null) {
                successStr = successStr.trim().replaceAll("^\"|\"$", "");
            }

            boolean success = Boolean.parseBoolean(successStr);

            if (correlationId == null || requestType == null) {
                log.warn("🏠 [ORDER] 사용자 주소 조회 응답 필수 데이터 누락 - correlationId: {}, requestType: {}",
                        correlationId, requestType);
                return;
            }

            log.info("🏠 [ORDER] 사용자 주소 조회 응답 처리 - correlationId: {}, type: {}, success: {}",
                    correlationId, requestType, success);

            Long userId = null;
            List<UserAddressResponse> addresses = null;
            try {
                String userIdStr = (String) values.get("userId");
                if (userIdStr != null && !userIdStr.isEmpty()) {
                    // userId에서도 따옴표 제거
                    userIdStr = userIdStr.trim().replaceAll("^\"|\"$", "");
                    if (!userIdStr.isEmpty() && !userIdStr.equals("null")) {
                        userId = Long.parseLong(userIdStr);
                    }
                }

                // 개별 주소 필드 파싱
                if (success) {
                    String addressId = (String) values.get("addressId");
                    String addrName = (String) values.get("addrName");
                    String address1 = (String) values.get("address1");
                    String address2 = (String) values.get("address2");
                    String postalCode = (String) values.get("postalCode");
                    String isDefaultStr = (String) values.get("isDefault");

                    if (addressId != null && !addressId.trim().isEmpty()) {
                        // 따옴표 제거
                        addressId = addressId.trim().replaceAll("^\"|\"$", "");
                        if (addrName != null) addrName = addrName.trim().replaceAll("^\"|\"$", "");
                        if (address1 != null) address1 = address1.trim().replaceAll("^\"|\"$", "");
                        if (address2 != null) address2 = address2.trim().replaceAll("^\"|\"$", "");
                        if (postalCode != null) postalCode = postalCode.trim().replaceAll("^\"|\"$", "");
                        if (isDefaultStr != null) isDefaultStr = isDefaultStr.trim().replaceAll("^\"|\"$", "");

                        boolean isDefault = "true".equals(isDefaultStr);

                        if (!addressId.isEmpty() && !addressId.equals("null")) {
                            UserAddressResponse address = UserAddressResponse.builder()
                                    .addrId(UUID.fromString(addressId))
                                    .userId(userId)
                                    .addrName(addrName)
                                    .address1(address1)
                                    .address2(address2)
                                    .postalCode(postalCode)
                                    .isDefault(isDefault)
                                    .build();

                            addresses = List.of(address);
                            log.info("🏠 [ORDER] 주소 데이터 파싱 성공 - addrId: {}, addrName: {}",
                                    address.getAddrId(), address.getAddrName());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("🏠 [ORDER] 사용자 주소 조회 응답 데이터 파싱 실패 - correlationId: {}, error: {}",
                        correlationId, e.getMessage());
                success = false;
                message = "데이터 파싱 실패";
                addresses = null;
            }

            // UserAddressLookupResponseEvent 생성 및 처리
            UserAddressLookupResponseEvent response = UserAddressLookupResponseEvent.builder()
                    .eventId((String) values.get("eventId"))
                    .correlationId(correlationId)
                    .requestType(requestType)
                    .userId(userId)
                    .addresses(addresses)
                    .success(success)
                    .message(message)
                    .respondedAt(java.time.LocalDateTime.now())
                    .build();

            // OrderUserLookupService에 응답 전달
            orderUserLookupService.handleUserAddressLookupResponse(response);

            log.info("✅ [ORDER] 사용자 주소 조회 응답 처리 완료 - correlationId: {}, userId: {}",
                    correlationId, userId);

        } catch (Exception e) {
            log.error("🚨 [ORDER] 사용자 주소 조회 응답 처리 실패 - values: {}, error: {}",
                    values, e.getMessage(), e);
        }
    }

    private void handlePopupInfoLookupResponse(Map<String, Object> values) {
        try {
            String correlationId = (String) values.get("correlationId");
            String successStr = (String) values.get("success");
            String popupIdStr = (String) values.get("popupId");

            if (correlationId != null) {
                correlationId = correlationId.trim().replaceAll("^\"|\"$", "");
            }
            if (successStr != null) {
                successStr = successStr.trim().replaceAll("^\"|\"$", "");
            }
            if (popupIdStr != null) {
                popupIdStr = popupIdStr.trim().replaceAll("^\"|\"$", "");
            }

            boolean success = Boolean.parseBoolean(successStr);

            java.util.UUID popupId = null;
            if (popupIdStr != null && !popupIdStr.isEmpty()) {
                popupId = java.util.UUID.fromString(popupIdStr);
            }

            PopupInfoLookupResponseEvent response = PopupInfoLookupResponseEvent.builder()
                    .eventId((String) values.get("eventId"))
                    .correlationId(correlationId)
                    .popupId(popupId)
                    .success(success)
                    .message((String) values.get("message"))
                    .title((String) values.get("title"))
                    .description((String) values.get("description"))
                    .storeId(parseUuidValue(values.get("storeId")))
                    .storeName((String) values.get("storeName"))
                    .address1((String) values.get("address1"))
                    .address2((String) values.get("address2"))
                    .phoneNumber((String) values.get("phoneNumber"))
                    .status((String) values.get("status"))
                    .startDate(parseDateValue(values.get("startDate")))
                    .endDate(parseDateValue(values.get("endDate")))
                    .respondedAt(parseDateValue(values.get("respondedAt")))
                    .eventTime(parseDateValue(values.get("eventTime")))
                    .build();

            orderPopupLookupService.handlePopupInfoLookupResponse(response);

        } catch (Exception e) {
            log.error("🚨 [ORDER] 팝업 정보 조회 응답 처리 실패 - values: {}, error: {}",
                    values, e.getMessage(), e);
        }
    }

    private java.util.UUID parseUuidValue(Object value) {
        if (value == null) {
            return null;
        }
        String raw = value.toString().trim().replaceAll("^\"|\"$", "");
        if (raw.isEmpty()) {
            return null;
        }
        return java.util.UUID.fromString(raw);
    }

    private java.time.LocalDateTime parseDateValue(Object value) {
        if (value == null) {
            return null;
        }
        String raw = value.toString().trim().replaceAll("^\"|\"$", "");
        if (raw.isEmpty()) {
            return null;
        }
        return java.time.LocalDateTime.parse(raw);
    }

    /**
     * 결제 승인 이벤트 처리
     */
    private void handlePaymentApproved(Map<String, Object> values) {
        try {
            String orderId = (String) values.get("orderId");
            String paymentId = (String) values.get("paymentId");
            String amount = (String) values.get("amount");

            // 따옴표 제거
            if (orderId != null) orderId = orderId.trim().replaceAll("^\"|\"$", "");
            if (paymentId != null) paymentId = paymentId.trim().replaceAll("^\"|\"$", "");
            if (amount != null) amount = amount.trim().replaceAll("^\"|\"$", "");

            log.info("💳 [ORDER] 결제 승인 처리 - orderId: {}, paymentId: {}, amount: {}",
                    orderId, paymentId, amount);

            if (orderId != null && !orderId.isEmpty()) {
                // 주문 상태를 PAID로 업데이트 (결제 완료 상태)
                UUID orderUuid = UUID.fromString(orderId);
                orderCommandService.updateOrderStatus(orderUuid, OrderStatus.PAID.name(),
                    "결제 승인 완료 - 결제ID: " + paymentId);

                log.info("✅ [ORDER] 주문 상태 업데이트 완료 - orderId: {}, status: PAID", orderId);
            }

        } catch (Exception e) {
            log.error("🚨 [ORDER] 결제 승인 이벤트 처리 실패 - values: {}, error: {}",
                    values, e.getMessage(), e);
        }
    }

    /**
     * 결제 완료 이벤트 처리
     */
    private void handlePaymentCompleted(Map<String, Object> values) {
        try {
            String orderId = (String) values.get("orderId");
            String paymentId = (String) values.get("paymentId");

            // 따옴표 제거
            if (orderId != null) orderId = orderId.trim().replaceAll("^\"|\"$", "");
            if (paymentId != null) paymentId = paymentId.trim().replaceAll("^\"|\"$", "");

            log.info("✅ [ORDER] 결제 완료 처리 - orderId: {}, paymentId: {}",
                    orderId, paymentId);

            if (orderId != null && !orderId.isEmpty()) {
                UUID orderUuid = UUID.fromString(orderId);

                // 1. 재고 차감 요청 (Store 서비스에 재고 차감 요청 전송)
                orderCommandService.requestStockDeduction(orderUuid);

                // 2. 주문 상태를 COMPLETED로 업데이트
                orderCommandService.updateOrderStatus(orderUuid, OrderStatus.COMPLETED.name(),
                    "결제 완료 - 결제ID: " + paymentId);

                // 3. 주문 완료 이벤트 발행 (알림 등 후속 처리용)
                orderCommandService.publishOrderCompletedEvent(orderUuid);

                log.info("✅ [ORDER] 주문 완료 처리 완료 - orderId: {}, status: COMPLETED", orderId);
            }

        } catch (Exception e) {
            log.error("🚨 [ORDER] 결제 완료 이벤트 처리 실패 - values: {}, error: {}",
                    values, e.getMessage(), e);
        }
    }

    /**
     * 결제 실패 이벤트 처리
     */
    private void handlePaymentFailed(Map<String, Object> values) {
        try {
            String orderId = (String) values.get("orderId");
            String paymentId = (String) values.get("paymentId");
            String reason = (String) values.get("reason");

            // 따옴표 제거
            if (orderId != null) orderId = orderId.trim().replaceAll("^\"|\"$", "");
            if (paymentId != null) paymentId = paymentId.trim().replaceAll("^\"|\"$", "");
            if (reason != null) reason = reason.trim().replaceAll("^\"|\"$", "");

            log.info("❌ [ORDER] 결제 실패 처리 - orderId: {}, paymentId: {}, reason: {}",
                    orderId, paymentId, reason);

            if (orderId != null && !orderId.isEmpty()) {
                UUID orderUuid = UUID.fromString(orderId);

                // 1. 결제 취소 처리 (결제 실패 시)
                if (paymentId != null && !paymentId.isEmpty()) {
                    orderCommandService.cancelPaymentForOrder(orderUuid, paymentId, reason);
                }

                // 2. 주문 상태를 CANCELLED로 업데이트 (결제 실패로 인한 취소)
                orderCommandService.updateOrderStatus(orderUuid, OrderStatus.CANCELLED.name(),
                    "결제 실패 - 결제ID: " + paymentId + ", 사유: " + reason);

                // 3. 재고 예약 해제
                orderCommandService.cancelStockReservationsForOrder(orderUuid);

                log.info("✅ [ORDER] 결제 실패 처리 완료 - orderId: {}, 결제취소: {}, status: CANCELLED, 재고 예약 해제됨",
                        orderId, paymentId);
            }

        } catch (Exception e) {
            log.error("🚨 [ORDER] 결제 실패 이벤트 처리 실패 - values: {}, error: {}",
                    values, e.getMessage(), e);
        }
    }

    /**
     * 재고 차감 성공 이벤트 처리
     */
    private void handleStockDeductionSuccess(Map<String, Object> values) {
        try {
            String orderId = (String) values.get("orderId");
            String orderNo = (String) values.get("orderNo");
            String stockDetails = (String) values.get("stockDetails");

            // 따옴표 제거
            if (orderId != null) orderId = orderId.trim().replaceAll("^\"|\"$", "");
            if (orderNo != null) orderNo = orderNo.trim().replaceAll("^\"|\"$", "");
            if (stockDetails != null) stockDetails = stockDetails.trim().replaceAll("^\"|\"$", "");

            log.info("📦✅ [ORDER] 재고 차감 성공 처리 - orderId: {}, orderNo: {}, stockDetails: {}",
                    orderId, orderNo, stockDetails);

            if (orderId != null && !orderId.isEmpty()) {
                UUID orderUuid = UUID.fromString(orderId);

                // 주문 상태를 COMPLETED로 업데이트 (재고 차감 성공 = 주문 완료)
                try {
                    orderCommandService.updateOrderStatus(orderUuid, OrderStatus.COMPLETED.name(),
                        "재고 차감 완료 - 주문 완료: " + stockDetails);
                } catch (IdempotencyService.IdempotencyException e) {
                    log.warn("📦✅ [ORDER] 재고 차감 성공 멱등 처리 중복 - orderId: {}, reason: {}",
                            orderId, e.getMessage());
                    return;
                }

                log.info("📦✅ [ORDER] 재고 차감 성공으로 주문 완료 상태 업데이트 완료 - orderId: {}", orderId);
            }

        } catch (Exception e) {
            log.error("🚨 [ORDER] 재고 차감 성공 이벤트 처리 실패 - values: {}, error: {}",
                    values, e.getMessage(), e);
        }
    }

    /**
     * 재고 차감 실패 이벤트 처리
     */
    private void handleStockDeductionFailed(Map<String, Object> values) {
        try {
            String orderId = (String) values.get("orderId");
            String orderNo = (String) values.get("orderNo");
            String reason = (String) values.get("reason");

            // 따옴표 제거
            if (orderId != null) orderId = orderId.trim().replaceAll("^\"|\"$", "");
            if (orderNo != null) orderNo = orderNo.trim().replaceAll("^\"|\"$", "");
            if (reason != null) reason = reason.trim().replaceAll("^\"|\"$", "");

            log.error("📦❌ [ORDER] 재고 차감 실패 처리 - orderId: {}, orderNo: {}, reason: {}",
                    orderId, orderNo, reason);

            if (orderId != null && !orderId.isEmpty()) {
                UUID orderUuid = UUID.fromString(orderId);

                // 1. 주문 상태를 CANCELLED로 업데이트 (재고 차감 실패)
                orderCommandService.updateOrderStatus(orderUuid, OrderStatus.CANCELLED.name(),
                    "재고 차감 실패로 인한 주문 취소 - " + reason);

                // 2. 결제 환불 처리 (결제가 완료된 상태에서 재고 차감 실패 시)
                try {
                    // 결제 정보는 별도 조회가 필요할 수 있음 (임시로 orderId 사용)
                    orderCommandService.cancelPaymentForOrder(orderUuid, orderUuid.toString(),
                        "재고 차감 실패로 인한 자동 환불: " + reason);
                } catch (Exception paymentCancelEx) {
                    log.error("📦❌ [ORDER] 재고 차감 실패 후 결제 환불 처리 실패 - orderId: {}, error: {}",
                            orderId, paymentCancelEx.getMessage(), paymentCancelEx);
                }

                log.info("📦❌ [ORDER] 재고 차감 실패로 주문 취소 처리 완료 - orderId: {}", orderId);
            }

        } catch (Exception e) {
            log.error("🚨 [ORDER] 재고 차감 실패 이벤트 처리 실패 - values: {}, error: {}",
                    values, e.getMessage(), e);
        }
    }

    /**
     * 굿즈 예약 성공 이벤트 처리
     */
    private void handleGoodsReserved(Map<String, Object> values) {
        try {
            String orderId = (String) values.get("orderId");
            String orderNo = (String) values.get("orderNo");

            if (orderId != null) orderId = orderId.trim().replaceAll("^\"|\"$", "");
            if (orderNo != null) orderNo = orderNo.trim().replaceAll("^\"|\"$", "");

            log.info("📦✅ [ORDER] 굿즈 예약 성공 처리 - orderId: {}, orderNo: {}", orderId, orderNo);

            if (orderId != null && !orderId.isEmpty()) {
                UUID orderUuid = UUID.fromString(orderId);

                // 주문 생성 트랜잭션 커밋 전에 이벤트가 도착할 수 있으므로 재시도
                boolean updated = tryUpdateOrderStatusWithRetry(orderUuid);
                if (!updated) {
                    log.warn("📦✅ [ORDER] 굿즈 예약 성공 처리 재시도 실패 - orderId: {}", orderId);
                    return;
                }

                // 결제 생성 요청 이벤트 발행
                orderCommandService.publishPaymentCreateRequestedEvent(orderUuid);
            }

        } catch (Exception e) {
            log.error("🚨 [ORDER] 굿즈 예약 성공 이벤트 처리 실패 - values: {}, error: {}",
                    values, e.getMessage(), e);
        }
    }

    private boolean tryUpdateOrderStatusWithRetry(UUID orderId) {
        int maxAttempts = 10;
        long delayMillis = 200L;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (!orderRepository.existsById(orderId)) {
                    if (attempt < maxAttempts) {
                        try {
                            Thread.sleep(delayMillis);
                        } catch (InterruptedException interruptedException) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                        continue;
                    }
                    return false;
                }

                orderCommandService.updateOrderStatus(orderId, OrderStatus.PAYMENT_PENDING.name(),
                        "재고 예약 완료 - 결제 진행");
                return true;
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "";
                if (message.contains("주문을 찾을 수 없어요") && attempt < maxAttempts) {
                    try {
                        Thread.sleep(delayMillis);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                    continue;
                }
                throw e;
            }
        }
        return false;
    }

    /**
     * 굿즈 예약 실패 이벤트 처리
     */
    private void handleGoodsReservationFailed(Map<String, Object> values) {
        try {
            String orderId = (String) values.get("orderId");
            String reason = (String) values.get("reason");

            if (orderId != null) orderId = orderId.trim().replaceAll("^\"|\"$", "");
            if (reason != null) reason = reason.trim().replaceAll("^\"|\"$", "");

            log.warn("📦❌ [ORDER] 굿즈 예약 실패 처리 - orderId: {}, reason: {}", orderId, reason);

            if (orderId != null && !orderId.isEmpty()) {
                UUID orderUuid = UUID.fromString(orderId);
                orderCommandService.updateOrderStatus(orderUuid, OrderStatus.REJECTED.name(),
                        "재고 부족 - 주문 실패: " + (reason != null ? reason : "재고 부족"));
            }

        } catch (Exception e) {
            log.error("🚨 [ORDER] 굿즈 예약 실패 이벤트 처리 실패 - values: {}, error: {}",
                    values, e.getMessage(), e);
        }
    }
}
