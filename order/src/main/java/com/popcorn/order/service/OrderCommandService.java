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
import com.popcorn.order.repository.OrderRepository;
import com.popcorn.order.repository.OrderItemRepository;
import com.popcorn.order.repository.OrderStatusHistoryRepository;
import com.popcorn.order.client.PaymentClient;
import com.popcorn.order.client.UserClient;
import com.popcorn.order.dto.payment.CreatePaymentRequest;
import com.popcorn.order.dto.payment.CreatePaymentResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final PaymentClient paymentClient;
    private final UserClient userClient;

    /**
     * 새로운 주문 생성하기 (멱등성 처리)
     *
     * [초보자 가이드]
     * @Idempotent: 중복 요청 방지 - 같은 사용자가 같은 팝업에 중복 주문 시도해도 한 번만 처리
     *
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

        // 5. 상태 이력 저장
        OrderStatusHistory createdHistory = OrderStatusHistory.builder()
                .orderId(savedOrder.getId())
                .fromStatus(null)
                .toStatus(savedOrder.getStatus())
                .reason("주문 생성")
                .changedAt(LocalDateTime.now())
                .build();
        orderStatusHistoryRepository.save(createdHistory);

        // 6. 이벤트 발행
        eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder, null));

        // 7. 결제 프로세스 시작
        try {
            startPaymentProcess(savedOrder, command);
        } catch (Exception e) {
            log.warn("결제 시작 실패 - 주문번호: {}, 나중에 수동 처리: {}",
                savedOrder.getOrderNo(), e.getMessage());
            // 결제 실패해도 주문은 생성됨 (나중에 수동 결제 가능)
        }

        // 8. 응답 생성 (결제 정보 포함)
        String paymentMethod = determinePaymentMethod(savedOrder);
        LocalDateTime paymentExpiresAt = LocalDateTime.now().plusMinutes(30); // 30분 후 만료

        CreateOrderResponse response = CreateOrderResponse.fromOrderWithPayment(
                savedOrder,
                null, // 결제 ID는 비동기 생성 후 확정
                "PENDING", // 결제 진행 중
                paymentMethod,
                null, // 결제 URL은 비동기 생성 후 확정
                paymentExpiresAt,
                "결제가 준비 중입니다. 잠시 후 결제 링크를 받으실 수 있습니다."
        );

        log.info("주문 생성 완료 - 주문번호: {}, 결제방법: {}", response.getOrderNo(), paymentMethod);

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
     * [초보자 가이드]
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
     * 향후 Popup/Session 서비스와 연동 예정
     */
    private Integer getSessionPrice(UUID sessionId) {
        try {
            // TODO: 실제 Popup/Session 서비스 API 호출
            // SessionPriceResponse response = sessionClient.getSessionPrice(sessionId);
            // return response.getPrice();

            // 현재는 Mock 데이터로 처리
            log.info("세션 가격 조회: sessionId={}", sessionId);

            // 세션별 차등 가격 적용 (Mock)
            String sessionIdStr = sessionId.toString();
            if (sessionIdStr.hashCode() % 3 == 0) {
                return 20000; // VIP 세션
            } else if (sessionIdStr.hashCode() % 3 == 1) {
                return 15000; // 일반 세션
            } else {
                return 12000; // 할인 세션
            }
        } catch (Exception e) {
            log.error("세션 가격 조회 실패: sessionId={}", sessionId, e);
            return 15000; // 기본 가격
        }
    }

    /**
     * 굿즈 상품 변형 가격 조회
     * 향후 Product/Goods 서비스와 연동 예정
     */
    private Integer getGoodsVariantPrice(UUID goodsVariantId) {
        try {
            // TODO: 실제 Product/Goods 서비스 API 호출
            // GoodsPriceResponse response = goodsClient.getGoodsVariantPrice(goodsVariantId);
            // return response.getPrice();

            // 현재는 Mock 데이터로 처리
            log.info("굿즈 가격 조회: goodsVariantId={}", goodsVariantId);

            // 상품별 차등 가격 적용 (Mock)
            String variantIdStr = goodsVariantId.toString();
            int hash = variantIdStr.hashCode();
            if (Math.abs(hash) % 4 == 0) {
                return 45000; // 한정판 굿즈
            } else if (Math.abs(hash) % 4 == 1) {
                return 25000; // 일반 굿즈
            } else if (Math.abs(hash) % 4 == 2) {
                return 15000; // 소형 굿즈
            } else {
                return 35000; // 프리미엄 굿즈
            }
        } catch (Exception e) {
            log.error("굿즈 가격 조회 실패: goodsVariantId={}", goodsVariantId, e);
            return 25000; // 기본 가격
        }
    }

    /**
     * 주문 생성 후 결제 프로세스 시작
     *
     * [초보자 가이드]
     * 주문이 생성된 후 자동으로 결제를 시작합니다.
     * - 주문 상태 → PAYMENT_PENDING으로 변경
     * - Payment 마이크로서비스에 결제 요청
     * - 비동기로 처리 (결제 실패해도 주문은 유지)
     */
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

}
