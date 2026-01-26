package com.popcorn.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.common.annotation.Idempotent;

import com.popcorn.order.dto.command.CreateOrderCommand;
import com.popcorn.order.dto.response.CreateOrderResponse;
import com.popcorn.order.entity.Order;
import com.popcorn.order.entity.OrderItem;
import com.popcorn.order.entity.OrderItemType;
import com.popcorn.order.entity.OrderStatus;
import com.popcorn.order.entity.OrderStatusHistory;
import com.popcorn.order.entity.OrderType;
import com.popcorn.order.event.OrderCreatedEvent;
import com.popcorn.order.event.OrderStatusChangedEvent;
import com.popcorn.order.event.OrderCancelledEvent;
import com.popcorn.order.event.OrderEventPublisher;
import com.popcorn.order.event.StockReservedEvent;
import com.popcorn.order.event.StockReservationFailedEvent;
import com.popcorn.order.repository.OrderRepository;
import com.popcorn.order.repository.OrderItemRepository;
import com.popcorn.order.repository.OrderStatusHistoryRepository;
import com.popcorn.order.client.PaymentClient;
import com.popcorn.order.client.UserClient;
import com.popcorn.order.client.StoreClient;
import com.popcorn.order.dto.payment.CreatePaymentRequest;
import com.popcorn.order.dto.payment.CreatePaymentResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.popcorn.order.util.PaymentTokenUtil;

/**
 * 주문 명령(Command) 처리 서비스
 *
 * CQRS 패턴의 Command 쪽 담당 - 데이터 변경 작업만 처리
 * 주문 생성, 상태 변경, 취소 등 데이터를 바꾸는 모든 작업을 여기서 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCommandService {

    private final OrderDomainService orderDomainService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderEventPublisher orderEventPublisher;
    private final PaymentClient paymentClient;
    private final UserClient userClient;
    private final PaymentTokenUtil paymentTokenUtil;
    private final StoreClient storeClient;
    private final OrderCacheService orderCacheService;

    @org.springframework.beans.factory.annotation.Value("${frontend.base-url:${FRONTEND_BASE_URL:http://localhost:3000}}")
    private String frontendBaseUrl;

    /**
     * 새로운 주문 생성하기 (멱등성 처리)
     * keyExpression: 사용자ID + 팝업ID로 고유 키 생성
     * ttlSeconds: 5분간 멱등성 보장 (실수로 빠르게 연속 클릭해도 안전)
     */
    @Transactional
    @Idempotent(
        keyExpression = "#command.userId + ':' + #command.popupId",
        keyPrefix = "order:create",
        ttlSeconds = 300,  // 5분
        responseType = CreateOrderResponse.class
    )
    public CreateOrderResponse createOrder(CreateOrderCommand command) {
        log.info("주문 생성 시작 - 사용자: {}, 팝업: {}", command.getUserId(), command.getPopupId());

        try {
            return executeOrderCreation(command);
        } catch (Exception e) {
            log.error("주문 생성 실패 - 사용자: {}, 에러: {}", command.getUserId(), e.getMessage(), e);
            throw new RuntimeException("주문 생성 중 문제가 발생했어요: " + e.getMessage(), e);
        }
    }

    /**
     * 실제 주문 생성 로직 실행
     */
    private CreateOrderResponse executeOrderCreation(CreateOrderCommand command) {
        // 1. 명령을 엔티티로 변환
        List<OrderItem> orderItems = convertToOrderItems(command.getItems());
        OrderType orderType = OrderType.valueOf(command.getOrderType());

        // 1-1. 굿즈 주문이면 기본 배송지 확인
        validateDefaultAddressIfNeeded(command);

        // 2. 도메인 서비스로 주문 생성 (popupId 기반으로 변경)
        Order order = orderDomainService.createOrder(
                command.getUserId(),
                command.getPopupId(),
                orderType,
                orderItems
        );

        // 3. 데이터베이스에 저장
        Order savedOrder = orderRepository.save(order);

        // 4. 주문 항목들 저장 (orderId 설정 후)
        savedOrder.getOrderItems().forEach(item -> item.setOrderId(savedOrder.getId()));
        orderItemRepository.saveAll(savedOrder.getOrderItems());

        // 5. 상태 이력 저장 (주문 생성)
        OrderStatusHistory createdHistory = OrderStatusHistory.builder()
                .orderId(savedOrder.getId())
                .fromStatus(null)
                .toStatus(savedOrder.getStatus())
                .reason("주문 생성")
                .changedAt(LocalDateTime.now())
                .build();

        orderStatusHistoryRepository.save(createdHistory);

        // 6. 재고 예약 시도
        try {
            reserveStockForOrder(savedOrder);

            // 재고 예약 성공 - 상태를 RESERVED로 변경
            savedOrder.updateStatus(OrderStatus.RESERVED);
            orderRepository.save(savedOrder);

            // 재고 예약 성공 이력 저장
            OrderStatusHistory reservedHistory = OrderStatusHistory.builder()
                    .orderId(savedOrder.getId())
                    .fromStatus(OrderStatus.REQUESTED)
                    .toStatus(OrderStatus.RESERVED)
                    .reason("재고 예약 완료")
                    .changedAt(LocalDateTime.now())
                    .build();

            orderStatusHistoryRepository.save(reservedHistory);

            log.info("재고 예약 성공 - 주문번호: {}", savedOrder.getOrderNo());

        } catch (Exception e) {
            log.error("재고 예약 실패 - 주문번호: {}, 에러: {}", savedOrder.getOrderNo(), e.getMessage(), e);

            // 재고 예약 실패 - 주문 취소
            savedOrder.updateStatus(OrderStatus.CANCELLED);
            orderRepository.save(savedOrder);

            // 재고 예약 실패 이력 저장
            OrderStatusHistory failedHistory = OrderStatusHistory.builder()
                    .orderId(savedOrder.getId())
                    .fromStatus(OrderStatus.REQUESTED)
                    .toStatus(OrderStatus.CANCELLED)
                    .reason("재고 예약 실패: " + e.getMessage())
                    .changedAt(LocalDateTime.now())
                    .build();

            orderStatusHistoryRepository.save(failedHistory);

            throw new RuntimeException("재고가 부족합니다. 주문이 취소되었습니다: " + e.getMessage(), e);
        }

        // 7. 이벤트 발행
        eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder, null));

        // 7. 결제 URL 생성 (실제 결제 기록은 결제 완료 시점에 생성)
        String paymentMethod = determinePaymentMethod(savedOrder);
        CreatePaymentResponse paymentResponse = requestPaymentUrl(savedOrder, paymentMethod);
        String paymentUrl = paymentResponse != null ? paymentResponse.getPaymentUrl() : null;
        LocalDateTime paymentExpiresAt = paymentResponse != null && paymentResponse.getExpiresAt() != null
            ? paymentResponse.getExpiresAt()
            : LocalDateTime.now().plusMinutes(30);
        String paymentStatus = paymentResponse != null && paymentResponse.getStatus() != null
            ? paymentResponse.getStatus()
            : "READY";
        String paymentMessage = paymentUrl != null
            ? "결제 링크가 생성되었습니다. 링크를 통해 결제를 완료해주세요."
            : "결제가 준비 중입니다. 잠시 후 결제 링크를 받으실 수 있습니다.";

        // 8. 응답 생성 (결제 URL 포함, 실제 Payment 엔티티는 생성하지 않음)
        CreateOrderResponse response = CreateOrderResponse.fromOrderWithPayment(
                savedOrder,
                paymentResponse != null ? paymentResponse.getPaymentId() : null,
                paymentStatus,
                paymentMethod,
                paymentUrl,
                paymentExpiresAt,
                paymentMessage
        );

        log.info("주문 생성 완료 - 주문번호: {}, 결제방법: {}", response.getOrderNo(), paymentMethod);

        orderCacheService.evictMyOrdersCache(savedOrder.getCustomerId());

        return response;
    }

    private void validateDefaultAddressIfNeeded(CreateOrderCommand command) {
        if (command == null || command.getItems() == null) {
            return;
        }

        boolean requiresShipping = command.getItems().stream()
            .anyMatch(item -> OrderItemType.GOODS.equals(item.getOrderItemType()));
        if (!requiresShipping) {
            return;
        }

        userClient.getDefaultAddress(command.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("기본 배송지가 필요합니다."));
    }

    /**
     * 주문 상태 변경하기 (멱등성 처리)
     *
     * 같은 주문을 같은 상태로 여러 번 변경해도 한 번만 처리됩니다.
     */
    @Transactional
    @Idempotent(
        keyExpression = "#orderId + ':' + #status",
        keyPrefix = "order:status",
        ttlSeconds = 300,
        responseType = Order.class
    )
    public Order updateOrderStatus(UUID orderId, String status, String reason) {
        log.info("주문 상태 변경 - 주문ID: {}, 새상태: {}, 이유: {}", orderId, status, reason);

        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없어요: " + orderId));

        OrderStatus currentStatus = order.getStatus();

        // 2. 이미 취소된 주문은 변경 불가
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new RuntimeException("이미 취소된 주문은 상태를 변경할 수 없어요");
        }

        // 3. 새 상태 검증
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("올바르지 않은 주문 상태예요: " + status);
        }

        // 4. 도메인 규칙 검증
        if (!orderDomainService.canChangeStatus(currentStatus, newStatus)) {
            throw new RuntimeException(String.format("상태 변경이 불가능해요: %s → %s", currentStatus, newStatus));
        }

        // 5. 상태 변경 및 저장
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        // 6. 상태 변경 이력 저장
        OrderStatusHistory statusHistory = OrderStatusHistory.builder()
                .orderId(savedOrder.getId())
                .fromStatus(currentStatus)
                .toStatus(newStatus)
                .reason(reason)
                .changedAt(LocalDateTime.now())
                .build();
        orderStatusHistoryRepository.save(statusHistory);

        // 7. 이벤트 발행
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                currentStatus,
                newStatus,
                reason,
                "SYSTEM"
        ));

        if (newStatus == OrderStatus.PAID) {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(savedOrder.getId());
            savedOrder.setOrderItems(orderItems);
            orderEventPublisher.publishOrderPaidEvent(savedOrder);
        }

        // 8. 특별한 상태 변경시 추가 이벤트
        if (newStatus == OrderStatus.CANCELLED) {
            eventPublisher.publishEvent(new OrderCancelledEvent(
                    savedOrder.getId(),
                    savedOrder.getCustomerId(),
                    currentStatus,
                    reason,
                    "SYSTEM",
                    null // 환불 금액은 별도 계산 필요
            ));
        }

        log.info("주문 상태 변경 완료 - 주문ID: {}, {} → {}", orderId, currentStatus, newStatus);
        return savedOrder;
    }

    /**
     * 주문 취소하기 (멱등성 처리)
     *
     * [초보자 가이드]
     * 같은 주문을 같은 이유로 여러 번 취소해도 한 번만 처리됩니다.
     */
    @Transactional
    @Idempotent(
        keyExpression = "#orderId + ':cancel:' + #reason",
        keyPrefix = "order:cancel",
        ttlSeconds = 300,
        responseType = Order.class
    )
    public Order cancelOrder(UUID orderId, String reason) {
        log.info("주문 취소 요청 - 주문ID: {}, 이유: {}", orderId, reason);

        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없어요: " + orderId));

        // 2. 취소 가능 여부 확인
        if (!orderDomainService.canCancelOrder(order)) {
            throw new RuntimeException("취소할 수 없는 주문이에요. 시간이 지났거나 이미 처리된 상태예요.");
        }

        // 3. 취소 상태로 변경
        return updateOrderStatus(orderId, OrderStatus.CANCELLED.name(), reason);
    }

    // ================ 헬퍼 메서드들 ================

    /**
     * 명령 객체를 주문 항목 엔티티로 변환
     */
    private List<OrderItem> convertToOrderItems(List<CreateOrderCommand.OrderItemCommand> itemCommands) {
        return itemCommands.stream()
                .map(this::convertToOrderItem)
                .toList();
    }

    /**
     * 개별 주문 항목 변환
     */
    private OrderItem convertToOrderItem(CreateOrderCommand.OrderItemCommand itemCommand) {
        // 단가 결정 (실제로는 가격 서비스에서 조회)
        Integer unitPrice = determineUnitPrice(itemCommand);
        Integer lineAmount = unitPrice * itemCommand.getQty();

        return OrderItem.builder()
                .orderItemType(itemCommand.getOrderItemType())
                .qty(itemCommand.getQty())
                .unitPrice(unitPrice)
                .lineAmount(lineAmount)
                .sessionOptionId(itemCommand.getSessionId())
                .goodsVariantId(itemCommand.getGoodsVariantId())
                .build();
    }

    /**
     * 상품 단가 결정하기
     * 실제 가격 서비스와 연동하여 정확한 가격 조회
     */
    private Integer determineUnitPrice(CreateOrderCommand.OrderItemCommand itemCommand) {
        OrderItemType itemType = itemCommand.getOrderItemType();

        if (OrderItemType.RESERVATION.equals(itemType)) {
            // 예약형: 세션 가격 조회
            UUID sessionId = itemCommand.getSessionId();
            if (sessionId == null) {
                throw new RuntimeException("예약형 상품은 세션 정보가 필요해요");
            }
            return getSessionPrice(sessionId);

        } else if (OrderItemType.GOODS.equals(itemType)) {
            // 구매형: 굿즈 가격 조회
            UUID goodsVariantId = itemCommand.getGoodsVariantId();
            if (goodsVariantId == null) {
                throw new RuntimeException("구매형 상품은 굿즈 정보가 필요해요");
            }
            return getGoodsVariantPrice(goodsVariantId);

        } else {
            throw new RuntimeException("알 수 없는 상품 타입이에요: " + itemType);
        }
    }

    /**
     * 세션 가격 조회
     * Store 서비스의 실제 API를 통해 세션 가격 정보 조회
     */
    private Integer getSessionPrice(UUID sessionId) {
        try {
            log.info("세션 가격 조회 요청 - sessionId: {}", sessionId);

            // Store 서비스에서 실제 세션 가격 조회
            var sessionPriceResponse = storeClient.getSessionPrice(sessionId);

            if (sessionPriceResponse != null && sessionPriceResponse.getPrice() != null) {
                log.info("세션 가격 조회 성공 - sessionId: {}, price: {}원",
                        sessionId, sessionPriceResponse.getPrice());
                return sessionPriceResponse.getPrice();
            } else {
                log.warn("세션 가격 정보가 비어있습니다 - sessionId: {}, 기본값 사용", sessionId);
                return 15000; // 기본 가격
            }
        } catch (Exception e) {
            log.error("세션 가격 조회 실패 - sessionId: {}, 기본값 사용, 에러: {}", sessionId, e.getMessage(), e);
            return 15000; // 기본 가격
        }
    }

    /**
     * 굿즈 상품 변형 가격 조회
     * Store 서비스의 실제 API를 통해 굿즈 가격 정보 조회
     */
    private Integer getGoodsVariantPrice(UUID goodsVariantId) {
        try {
            log.info("굿즈 가격 조회 요청 - goodsVariantId: {}", goodsVariantId);

            // Store 서비스에서 실제 굿즈 가격 조회
            var goodsPriceResponse = storeClient.getGoodsVariantPrice(goodsVariantId);

            if (goodsPriceResponse != null && goodsPriceResponse.getPrice() != null) {
                log.info("굿즈 가격 조회 성공 - goodsVariantId: {}, price: {}원, stock: {}개",
                        goodsVariantId, goodsPriceResponse.getPrice(), goodsPriceResponse.getStockQuantity());
                return goodsPriceResponse.getPrice();
            } else {
                log.warn("굿즈 가격 정보가 비어있습니다 - goodsVariantId: {}, 기본값 사용", goodsVariantId);
                return 25000; // 기본 가격
            }
        } catch (Exception e) {
            log.error("굿즈 가격 조회 실패 - goodsVariantId: {}, 기본값 사용, 에러: {}", goodsVariantId, e.getMessage(), e);
            return 25000; // 기본 가격
        }
    }

    /**
     * 주문 생성 후 결제 프로세스 시작 및 결제 URL 받기 (비동기 처리)
     *
     * [개선사항]
     * - Payment 서비스 호출은 비동기로 수행
     * - 결제 URL은 즉시 발급하여 응답에 포함
     * - 결제 실패 시에도 주문은 유지되며 나중에 결제 가능
     */
    private CreatePaymentResponse startPaymentProcessAndGetUrl(Order order, CreateOrderCommand command) {
        log.info("결제 프로세스 시작 - 주문번호: {}, 금액: {}원",
                order.getOrderNo(), order.getTotalAmount());

        // 1. 주문 상태를 결제 대기로 변경
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        orderRepository.save(order);

        // 2. 상태 변경 이력 저장
        OrderStatusHistory paymentPendingHistory = OrderStatusHistory.builder()
                .orderId(order.getId())
                .fromStatus(OrderStatus.REQUESTED)
                .toStatus(OrderStatus.PAYMENT_PENDING)
                .reason("결제 프로세스 시작")
                .changedAt(LocalDateTime.now())
                .build();
        orderStatusHistoryRepository.save(paymentPendingHistory);

        // 비동기 결제 요청 및 토큰 URL 발급
        return requestPaymentUrl(order, determinePaymentMethod(order));
    }

    /**
     * 주문 생성 후 결제 프로세스 시작 (기존 비동기 방식 - 호환성 유지)
     *
     * [초보자 가이드]
     * 주문이 생성된 후 자동으로 결제를 시작합니다.
     * - 주문 상태 → PAYMENT_PENDING으로 변경
     * - Payment 마이크로서비스에 결제 요청
     * - 비동기로 처리 (결제 실패해도 주문은 유지)
     *
     * @deprecated 새로운 동기 방식으로 대체됨. startPaymentProcessAndGetUrl() 사용 권장
     */
    @Deprecated
    private void startPaymentProcess(Order order, CreateOrderCommand command) {
        log.info("결제 프로세스 시작 - 주문번호: {}, 금액: {}원",
                order.getOrderNo(), order.getTotalAmount());

        // 1. 주문 상태를 결제 대기로 변경
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        orderRepository.save(order);

        // 2. 상태 변경 이력 저장
        OrderStatusHistory paymentPendingHistory = OrderStatusHistory.builder()
                .orderId(order.getId())
                .fromStatus(OrderStatus.REQUESTED)
                .toStatus(OrderStatus.PAYMENT_PENDING)
                .reason("결제 프로세스 시작")
                .changedAt(LocalDateTime.now())
                .build();
        orderStatusHistoryRepository.save(paymentPendingHistory);

        // 3. Payment 서비스에 결제 요청 (비동기)
        CreatePaymentRequest paymentRequest = CreatePaymentRequest.fromOrder(
                order.getId(),
                order.getCustomerId(),
                order.getOrderNo(),
                order.getTotalAmount(),
                determinePaymentMethod(order) // 사용자가 선택한 결제 방법 결정
        );

        // 4. 비동기로 Payment 서비스 호출
        paymentClient.createPayment(paymentRequest)
                .subscribe(
                    // 결제 생성 성공
                    this::handlePaymentSuccess,
                    // 결제 생성 실패
                    error -> handlePaymentError(order.getId(), error)
                );

        log.info("결제 요청 전송 완료 - 주문번호: {}", order.getOrderNo());
    }

    /**
     * 결제 생성 성공 처리
     */
    private void handlePaymentSuccess(CreatePaymentResponse paymentResponse) {
        try {
            log.info("결제 생성 성공 - 주문ID: {}, 결제ID: {}, 상태: {}",
                    paymentResponse.getOrderId(), paymentResponse.getPaymentId(), paymentResponse.getStatus());

            // 결제가 즉시 완료된 경우 (예: 간편결제)
            if (paymentResponse.isCompleted()) {
                updateOrderStatus(paymentResponse.getOrderId(),
                                OrderStatus.PAID.name(),
                                "결제 완료");
            }
            // 결제 대기 상태라면 별도 처리 불필요 (이미 PAYMENT_PENDING)

            // 결제 URL이 있다면 고객에게 알림 발송 (결제 대기 상태인 경우만)
            if (!paymentResponse.isCompleted() && paymentResponse.getPaymentUrl() != null) {
                sendPaymentUrlNotificationToCustomer(paymentResponse);
            }

        } catch (Exception e) {
            log.error("결제 성공 처리 중 오류 - 주문ID: {}, 에러: {}",
                    paymentResponse.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * 결제 생성 실패 처리
     */
    private void handlePaymentError(UUID orderId, Throwable error) {
        try {
            log.error("결제 생성 실패 - 주문ID: {}, 에러: {}", orderId, error.getMessage());

            // 주문 상태를 다시 요청 상태로 되돌림 (수동 결제 대기)
            updateOrderStatus(orderId, OrderStatus.REQUESTED.name(),
                            "결제 생성 실패, 수동 처리 필요: " + error.getMessage());

            // 관리자에게 결제 시스템 장애 알림 발송
            sendPaymentErrorNotificationToAdmin(orderId, error);

        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류 - 주문ID: {}, 에러: {}", orderId, e.getMessage(), e);
        }
    }

    /**
     * 결제 방법 결정
     * 주문 정보를 바탕으로 적절한 결제 방법을 결정
     */
    private String determinePaymentMethod(Order order) {
        // TODO: 실제로는 주문 생성 시 사용자가 선택한 결제 방법을 전달받아야 함
        // CreateOrderCommand에 paymentMethod 필드 추가 필요

        try {
            // 주문 금액에 따른 기본 결제 방법 결정 (임시 로직)
            Integer totalAmount = order.getTotalAmount();

            if (totalAmount >= 100000) {
                return "CARD"; // 고액 결제는 카드 결제
            } else if (totalAmount >= 50000) {
                return "TRANSFER"; // 중간 금액은 계좌이체
            } else {
                return "MOBILE_PHONE"; // 소액은 휴대폰 결제
            }
        } catch (Exception e) {
            log.warn("결제 방법 결정 중 오류, 기본값 사용: orderId={}", order.getId(), e);
            return "CARD"; // 기본값
        }
    }

    /**
     * 고객에게 결제 URL 알림 발송
     * 결제 대기 상태일 때 고객이 결제를 완료할 수 있도록 안내
     */
    private void sendPaymentUrlNotificationToCustomer(CreatePaymentResponse paymentResponse) {
        try {
            log.info("고객 결제 URL 알림 발송: 주문ID={}, 결제ID={}, URL={}",
                    paymentResponse.getOrderId(),
                    paymentResponse.getPaymentId(),
                    paymentResponse.getPaymentUrl());

            // TODO: 실제 고객 알림 서비스 연동
            // - 푸시 알림, SMS, 카카오톡 등을 통한 결제 링크 전송
            // - NotificationClient.sendPaymentUrl(customerId, paymentUrl, orderNo)

            // 현재는 로깅만 수행
        } catch (Exception e) {
            log.error("고객 결제 URL 알림 발송 실패: 주문ID={}",
                    paymentResponse.getOrderId(), e);
        }
    }

    /**
     * 관리자에게 결제 시스템 장애 알림 발송
     * 결제 시스템 장애로 주문이 수동 처리가 필요한 경우 관리자에게 즉시 알림
     */
    private void sendPaymentErrorNotificationToAdmin(UUID orderId, Throwable error) {
        try {
            log.warn("관리자 결제 장애 알림 발송: 주문ID={}, 에러={}",
                    orderId, error.getMessage());

            // TODO: 실제 관리자 알림 시스템 연동
            // - Slack, Teams, Email 등을 통한 장애 알림
            // - AdminNotificationClient.sendPaymentError(orderId, error, urgency=HIGH)

            // 현재는 경고 로깅만 수행 (모니터링 시스템에서 수집 가능)
        } catch (Exception e) {
            log.error("관리자 결제 장애 알림 발송 실패: 주문ID={}", orderId, e);
        }
    }

    /**
     * 프론트엔드 결제 페이지 URL 생성 + Payment 서비스에 결제 생성 요청
     *
     * [수정된 결제 플로우]
     * - 1단계: Payment 서비스에 실제 결제 생성 요청
     * - 2단계: 토큰 생성하여 프론트엔드 URL 생성
     * - 이렇게 하면 결제 승인 로그도 정상적으로 기록됨
     */
    private CreatePaymentResponse requestPaymentUrl(Order order, String paymentMethod) {
        try {
            log.info("💳 결제 생성 + 토큰 URL 생성 시작 - 주문번호: {}, 금액: {}원, 결제방법: {}",
                    order.getOrderNo(), order.getTotalAmount(), paymentMethod);

            // 1단계: Payment 서비스에 실제 결제 생성 요청
            CreatePaymentRequest paymentRequest = CreatePaymentRequest.fromOrder(
                    order.getId(),
                    order.getCustomerId(),
                    order.getOrderNo(),
                    order.getTotalAmount(),
                    paymentMethod
            );

            log.info("🔄 Payment 서비스 비동기 호출 시작 - 주문번호: {}", order.getOrderNo());
            paymentClient.createPayment(paymentRequest)
                    .doOnSuccess(response ->
                        log.info("✅ Payment 서비스 비동기 응답 수신 - 주문번호: {}, 결제ID: {}, 상태: {}",
                                order.getOrderNo(),
                                response != null ? response.getPaymentId() : null,
                                response != null ? response.getStatus() : null))
                    .doOnError(error ->
                        log.error("❌ Payment 서비스 비동기 호출 실패 - 주문번호: {}, 에러: {}",
                                order.getOrderNo(), error.getMessage(), error))
                    .subscribe();

            // 2단계: 토큰 생성 및 프론트엔드 URL 생성
            // 고객 키 생성 (사용자 ID 기반)
            String customerKey = "customer_" + order.getCustomerId().toString().replace("-", "");

            // 주문명 생성
            String orderName = generateOrderName(order);

            try {
                // 결제 정보를 암호화하여 토큰 생성
                String paymentToken = paymentTokenUtil.generatePaymentToken(
                        order.getId(),
                        order.getOrderNo(),
                        order.getTotalAmount(),
                        orderName,
                        customerKey,
                        paymentMethod
                );

                // 프론트엔드 결제 페이지 URL 생성
                String paymentUrl = String.format("%s/auto-payment?token=%s", frontendBaseUrl, paymentToken);

                // CreatePaymentResponse 생성 (Payment 서비스 응답 우선, 실패시 토큰 정보 사용)
                CreatePaymentResponse finalResponse = CreatePaymentResponse.builder()
                        .paymentId(null) // 비동기 요청이므로 즉시 결제 ID 미확정
                        .orderId(order.getId())
                        .amount(order.getTotalAmount())
                        .status("READY") // 결제 준비 상태
                        .paymentMethod(paymentMethod)
                        .paymentUrl(paymentUrl) // 토큰 방식 URL
                        .expiresAt(LocalDateTime.now().plusMinutes(30))
                        .createdAt(LocalDateTime.now())
                        .build();

                log.info("✅ 결제 생성 + 토큰 URL 생성 완료 - 주문번호: {}, 결제ID: {}, 토큰 길이: {}자",
                        order.getOrderNo(), finalResponse.getPaymentId(), paymentToken.length());
                log.debug("🔗 생성된 결제 URL: {}", paymentUrl);

                return finalResponse;

            } catch (Exception e) {
                log.error("💥 결제 토큰 암호화 실패 - 주문번호: {}, 에러: {}",
                        order.getOrderNo(), e.getMessage(), e);
                throw e;
            }

        } catch (Exception e) {
            log.error("💥 결제 생성 + 토큰 URL 생성 실패 - 주문번호: {}, 에러: {}",
                    order.getOrderNo(), e.getMessage(), e);

            // 실패 시에도 기본 응답 반환 (프론트엔드 에러 페이지 URL 포함)
            String errorUrl = frontendBaseUrl + "/payments/fail?reason=payment-creation-failed&orderId=" + order.getId();
            log.warn("🚨 결제 생성 실패로 프론트엔드 에러 페이지 반환: {}", errorUrl);

            return CreatePaymentResponse.builder()
                    .paymentId(null)
                    .orderId(order.getId())
                    .amount(order.getTotalAmount())
                    .status("ERROR")
                    .paymentMethod(paymentMethod)
                    .paymentUrl(errorUrl)
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * 주문명 생성 (결제 화면에 표시될 이름)
     */
    private String generateOrderName(Order order) {
        try {
            List<OrderItem> items = order.getOrderItems();
            if (items.isEmpty()) {
                return "팝콘 주문";
            }

            OrderItem firstItem = items.get(0);
            String itemName;

            if (OrderItemType.RESERVATION.equals(firstItem.getOrderItemType())) {
                itemName = "팝업 예약";
            } else if (OrderItemType.GOODS.equals(firstItem.getOrderItemType())) {
                itemName = "굿즈 구매";
            } else {
                itemName = "팝콘 상품";
            }

            if (items.size() == 1) {
                return itemName;
            } else {
                return itemName + " 외 " + (items.size() - 1) + "건";
            }

        } catch (Exception e) {
            log.warn("주문명 생성 실패, 기본명 사용: orderId={}", order.getId(), e);
            return "팝콘 주문";
        }
    }

    /**
     * 주문에 포함된 굿즈 항목들의 재고를 예약합니다.
     *
     * @param order 재고 예약할 주문
     * @throws RuntimeException 재고 부족 또는 예약 실패 시
     */
    private void reserveStockForOrder(Order order) {
        log.info("주문 재고 예약 시작 - 주문번호: {}", order.getOrderNo());

        // 굿즈 항목만 필터링 (예약형 상품은 재고 예약 불필요)
        List<OrderItem> goodsItems = order.getOrderItems().stream()
                .filter(item -> OrderItemType.GOODS.equals(item.getOrderItemType()))
                .toList();

        if (goodsItems.isEmpty()) {
            log.info("굿즈 항목이 없어 재고 예약을 건너뜁니다 - 주문번호: {}", order.getOrderNo());
            return;
        }

        // 각 굿즈 항목에 대해 재고 예약 시도
        for (OrderItem item : goodsItems) {
            if (item.getGoodsVariantId() == null) {
                log.warn("굿즈 변형 ID가 없어 재고 예약을 건너뜁니다 - 주문번호: {}, 항목ID: {}",
                        order.getOrderNo(), item.getId());
                continue;
            }

            try {
                log.info("굿즈 재고 예약 시도 - 주문번호: {}, 팝업ID: {}, 굿즈변형ID: {}, 수량: {}",
                        order.getOrderNo(), order.getPopupId(), item.getGoodsVariantId(), item.getQty());

                storeClient.reserveGoods(
                        order.getPopupId(),
                        item.getGoodsVariantId(),
                        item.getQty()
                );

                log.info("굿즈 재고 예약 성공 - 주문번호: {}, 굿즈변형ID: {}, 수량: {}",
                        order.getOrderNo(), item.getGoodsVariantId(), item.getQty());

            } catch (Exception e) {
                log.error("굿즈 재고 예약 실패 - 주문번호: {}, 굿즈변형ID: {}, 수량: {}, 에러: {}",
                        order.getOrderNo(), item.getGoodsVariantId(), item.getQty(), e.getMessage(), e);

                // 이전에 예약한 항목들 롤백
                rollbackStockReservations(order, goodsItems, item);

                // 재고 예약 실패 이벤트 발행
                publishStockReservationFailedEvent(order, item, e.getMessage());

                throw new RuntimeException("재고 예약 실패 - 굿즈변형ID: " + item.getGoodsVariantId() +
                        ", 수량: " + item.getQty() + ", 에러: " + e.getMessage(), e);
            }
        }

        // 재고 예약 성공 이벤트 발행
        publishStockReservedEvent(order, goodsItems);

        log.info("주문 재고 예약 완료 - 주문번호: {}", order.getOrderNo());
    }

    /**
     * 재고 예약 실패 시 이전에 예약한 항목들을 롤백합니다.
     *
     * @param order 주문
     * @param goodsItems 모든 굿즈 항목들
     * @param failedItem 실패한 항목 (이 항목 이전까지만 롤백)
     */
    private void rollbackStockReservations(Order order, List<OrderItem> goodsItems, OrderItem failedItem) {
        log.info("재고 예약 롤백 시작 - 주문번호: {}", order.getOrderNo());

        for (OrderItem item : goodsItems) {
            // 실패한 항목에 도달하면 중단
            if (item.equals(failedItem)) {
                break;
            }

            if (item.getGoodsVariantId() == null) {
                continue;
            }

            try {
                log.info("굿즈 재고 예약 취소 시도 - 주문번호: {}, 굿즈변형ID: {}, 수량: {}",
                        order.getOrderNo(), item.getGoodsVariantId(), item.getQty());

                storeClient.cancelGoodsReservation(
                        order.getPopupId(),
                        item.getGoodsVariantId(),
                        item.getQty()
                );

                log.info("굿즈 재고 예약 취소 성공 - 주문번호: {}, 굿즈변형ID: {}",
                        order.getOrderNo(), item.getGoodsVariantId());

            } catch (Exception e) {
                log.error("굿즈 재고 예약 취소 실패 - 주문번호: {}, 굿즈변형ID: {}, 에러: {}",
                        order.getOrderNo(), item.getGoodsVariantId(), e.getMessage(), e);
                // 롤백 실패는 로그만 남기고 계속 진행
            }
        }

        log.info("재고 예약 롤백 완료 - 주문번호: {}", order.getOrderNo());
    }

    /**
     * OrderItem에서 제품명 생성
     * OrderItem 엔티티에 제품명 필드가 없으므로 타입에 따라 임시 이름 생성
     */
    private String generateProductName(OrderItem item) {
        if (item == null || item.getOrderItemType() == null) {
            return "알 수 없는 상품";
        }

        switch (item.getOrderItemType()) {
            case RESERVATION:
                return "팝업 예약";
            case GOODS:
                return "굿즈 상품";
            default:
                return "상품";
        }
    }

    /**
     * 재고 예약 성공 이벤트 발행
     *
     * @param order 주문 정보
     * @param goodsItems 예약된 굿즈 항목들
     */
    private void publishStockReservedEvent(Order order, List<OrderItem> goodsItems) {
        try {
            List<StockReservedEvent.ReservedStockItem> reservedItems = goodsItems.stream()
                    .filter(item -> item.getGoodsVariantId() != null)
                    .map(item -> StockReservedEvent.ReservedStockItem.create(
                            item.getGoodsVariantId(),
                            item.getQty(),
                            item.getUnitPrice(),
                            generateProductName(item)
                    ))
                    .collect(java.util.stream.Collectors.toList());

            if (!reservedItems.isEmpty()) {
                orderEventPublisher.publishStockReservedEvent(order, reservedItems);
            }

        } catch (Exception e) {
            log.error("재고 예약 성공 이벤트 발행 실패 - 주문번호: {}, 에러: {}",
                    order.getOrderNo(), e.getMessage(), e);
            // 이벤트 발행 실패는 주문 처리에 영향을 주지 않음
        }
    }

    /**
     * 재고 예약 실패 이벤트 발행
     *
     * @param order 주문 정보
     * @param failedItem 실패한 항목
     * @param failureReason 실패 이유
     */
    private void publishStockReservationFailedEvent(Order order, OrderItem failedItem, String failureReason) {
        try {
            List<StockReservationFailedEvent.FailedStockItem> failedItems = List.of(
                    StockReservationFailedEvent.FailedStockItem.create(
                            failedItem.getGoodsVariantId(),
                            failedItem.getQty(),
                            0, // 사용 가능한 수량은 Store에서만 알 수 있음
                            generateProductName(failedItem),
                            failureReason
                    )
            );

            orderEventPublisher.publishStockReservationFailedEvent(order, failedItems, failureReason);

        } catch (Exception e) {
            log.error("재고 예약 실패 이벤트 발행 실패 - 주문번호: {}, 에러: {}",
                    order.getOrderNo(), e.getMessage(), e);
            // 이벤트 발행 실패는 주문 처리에 영향을 주지 않음
        }
    }

}
