package com.popcorn.order.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.popcorn.common.annotation.Idempotent;

import com.popcorn.order.dto.command.CreateOrderCommand;
import com.popcorn.order.dto.response.OrderCreateResponse;
import com.popcorn.order.entity.Order;
import com.popcorn.order.entity.OrderItem;
import com.popcorn.order.entity.OrderStatus;
import com.popcorn.order.entity.OrderStatusHistory;
import com.popcorn.order.entity.ItemType;
import com.popcorn.order.event.OrderCreatedEvent;
import com.popcorn.order.event.OrderStatusChangedEvent;
import com.popcorn.order.event.OrderCancelledEvent;
import com.popcorn.order.event.OrderEventPublisher;
import com.popcorn.order.event.standard.StandardOrderEventPublisher;
import com.popcorn.order.event.standard.StandardOrderCreatedEvent;
import com.popcorn.order.event.standard.StandardOrderStatusUpdatedEvent;
import com.popcorn.order.event.StockReservedEvent;
import com.popcorn.order.event.StockReservationFailedEvent;
import com.popcorn.order.event.StockDeductionRequestedEvent;
import com.popcorn.order.repository.OrderRepository;
import com.popcorn.order.repository.OrderItemRepository;
import com.popcorn.order.repository.OrderStatusHistoryRepository;
import com.popcorn.order.service.OrderUserLookupService;
import com.popcorn.order.dto.payment.CreatePaymentRequest;
import com.popcorn.order.dto.payment.CreatePaymentResponse;
import com.popcorn.order.dto.payment.PaymentUrlResponse;

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
    private final StandardOrderEventPublisher standardOrderEventPublisher;
    private final OrderUserLookupService orderUserLookupService;
    private final PaymentTokenUtil paymentTokenUtil;
    private final OrderCacheService orderCacheService;
    private final OrderPriceLookupService orderPriceLookupService;
    private final TransactionTemplate transactionTemplate;
    private final OrderReservationAwaiter orderReservationAwaiter;

    // 성능 최적화 서비스들
    private final OrderPriceCacheService orderPriceCacheService;
    private final OrderUserAddressCacheService orderUserAddressCacheService;

    @org.springframework.beans.factory.annotation.Value("${frontend.base-url:${FRONTEND_BASE_URL:http://localhost:3000}}")
    private String frontendBaseUrl;

    @org.springframework.beans.factory.annotation.Value("${order.reservation.wait-timeout-ms:5000}")
    private long reservationWaitTimeoutMs;


    @Idempotent(
        keyExpression = "#command.userId + ':' + #command.popupId",
        keyPrefix = "order:create",
        ttlSeconds = 300,  // 5분
        responseType = OrderCreateResponse.class
    )
    public OrderCreateResponse createOrder(CreateOrderCommand command) {
        log.info("주문 생성 시작 - 사용자: {}, 팝업: {}", command.getUserId(), command.getPopupId());

        try {
            return executeOrderCreation(command);
        } catch (com.popcorn.order.exception.OrderReservationFailedException
                 | com.popcorn.order.exception.OrderReservationTimeoutException e) {
            throw e;
        } catch (Exception e) {
            log.error("주문 생성 실패 - 사용자: {}, 에러: {}", command.getUserId(), e.getMessage(), e);
            throw new RuntimeException("주문 생성 중 문제가 발생했어요: " + e.getMessage(), e);
        }
    }

    private OrderCreateResponse executeOrderCreation(CreateOrderCommand command) {
        long startTime = System.currentTimeMillis();

        OrderCreationResult result = transactionTemplate.execute(status -> createOrderInTransaction(command));
        if (result == null || result.getOrder() == null) {
            throw new RuntimeException("주문 생성에 실패했습니다.");
        }

        Order savedOrder = result.getOrder();
        boolean hasGoodsItems = result.hasGoodsItems();
        boolean hasReservationItems = result.hasReservationItems();

        log.info("⚡ 주문 DB 생성 완료 - 주문번호: {}, 처리시간: {}ms",
                savedOrder.getOrderNo(), System.currentTimeMillis() - startTime);

        // 예약/재고 응답 대기 준비
        orderReservationAwaiter.register(savedOrder.getId());

        // 📅🛍️ 스케줄 예약 및 굿즈 재고 예약 처리 (트랜잭션 커밋 후 바로 발행)
        if (hasReservationItems && hasGoodsItems) {
            log.info("복합 주문 처리 시작 - 주문번호: {} (스케줄 + 굿즈)", savedOrder.getOrderNo());
            reserveScheduleForOrder(savedOrder);
            log.info("복합 주문 - 스케줄 예약 요청 완료, 굿즈 재고는 스케줄 성공 후 진행 - 주문번호: {}",
                    savedOrder.getOrderNo());
        } else if (hasReservationItems) {
            log.info("스케줄 전용 주문 처리 시작 - 주문번호: {}", savedOrder.getOrderNo());
            reserveScheduleForOrder(savedOrder);
            log.info("스케줄 예약 요청 완료 - 주문번호: {} (응답 이벤트 대기)", savedOrder.getOrderNo());
        } else if (hasGoodsItems) {
            log.info("굿즈 전용 주문 처리 시작 - 주문번호: {}", savedOrder.getOrderNo());
            reserveStockForOrder(savedOrder);
            log.info("굿즈 재고 예약 요청 완료 - 주문번호: {} (응답 이벤트 대기)", savedOrder.getOrderNo());
        } else {
            log.error("🚨 비즈니스 룰 위반: 예약도 굿즈도 없는 주문! - 주문번호: {}", savedOrder.getOrderNo());
            throw new IllegalStateException("예약 또는 굿즈 중 최소 하나는 필요합니다.");
        }

       
        eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder, null));

    
        OrderReservationAwaiter.ReservationOutcome outcome;
        try {
            long optimizedTimeout = 2000; // 5000ms → 2000ms로 단축
            outcome = orderReservationAwaiter.await(savedOrder.getId(),
                    java.time.Duration.ofMillis(optimizedTimeout));

            long waitTime = System.currentTimeMillis() - startTime;
            log.info("⚡ 재고 예약 응답 완료 - 주문번호: {}, 대기시간: {}ms (최적화된 타임아웃: {}ms)",
                    savedOrder.getOrderNo(), waitTime, optimizedTimeout);

        } catch (java.util.concurrent.TimeoutException e) {
            cancelOrderSafely(savedOrder.getId(), "예약 응답 타임아웃 (최적화된 2초)");
            throw new com.popcorn.order.exception.OrderReservationTimeoutException(
                    "예약 응답이 지연되어 주문 생성에 실패했습니다.");
        }

        if (!outcome.isSuccess()) {
            cancelOrderSafely(savedOrder.getId(), "예약 실패: " + outcome.getFailureReason());
            throw new com.popcorn.order.exception.OrderReservationFailedException(
                    outcome.getFailureReason() != null ? outcome.getFailureReason() : "예약 실패");
        }

        Order latestOrder = orderRepository.findById(savedOrder.getId()).orElse(savedOrder);
        latestOrder.setOrderItems(orderItemRepository.findByOrderId(latestOrder.getId()));
        String paymentMethod = determinePaymentMethod(latestOrder);
        com.popcorn.order.dto.payment.PaymentUrlResponse paymentUrl = outcome.getPaymentUrl();

        OrderCreateResponse response = OrderCreateResponse.fromOrderWithPayment(
                latestOrder,
                null,
                "PAYMENT_PENDING",
                paymentMethod,
                paymentUrl != null ? paymentUrl.getPaymentUrl() : null,
                paymentUrl != null ? paymentUrl.getExpiresAt() : null,
                paymentUrl != null
                        ? "예약 완료. 결제 링크가 발급되었습니다."
                        : "예약 완료. 결제 링크 생성 중입니다."
        );

        long totalElapsed = System.currentTimeMillis() - startTime;
        log.info("⚡ 주문 생성 완료 (캐시+타임아웃 최적화) - 주문번호: {}, 상태: {}, 총 처리시간: {}ms (목표: <2000ms)",
                response.getOrderNo(), response.getStatus(), totalElapsed);

        publishStandardOrderCreatedEvent(latestOrder);
        orderCacheService.evictMyOrdersCache(latestOrder.getCustomerId());

        return response;
    }

    @Transactional
    protected OrderCreationResult createOrderInTransaction(CreateOrderCommand command) {
        List<OrderItem> orderItems = convertToOrderItems(command.getItems());
        ItemType orderType = ItemType.valueOf(command.getOrderType());

        validateDefaultAddressIfNeeded(command);

        Order order = orderDomainService.createOrder(
                command.getUserId(),
                command.getPopupId(),
                orderType,
                orderItems
        );

        Order savedOrder = orderRepository.save(order);

        savedOrder.getOrderItems().forEach(item -> item.setOrderId(savedOrder.getId()));
        orderItemRepository.saveAll(savedOrder.getOrderItems());

        OrderStatusHistory createdHistory = OrderStatusHistory.builder()
                .orderId(savedOrder.getId())
                .fromStatus(null)
                .toStatus(savedOrder.getStatus())
                .reason("주문 생성")
                .changedAt(LocalDateTime.now())
                .build();
        orderStatusHistoryRepository.save(createdHistory);

        boolean hasGoodsItems = savedOrder.getOrderItems().stream()
                .anyMatch(item -> ItemType.GOODS.equals(item.getOrderItemType()));

        boolean hasReservationItems = savedOrder.getOrderItems().stream()
                .anyMatch(item -> ItemType.RESERVATION.equals(item.getOrderItemType()));

        return new OrderCreationResult(savedOrder, hasReservationItems, hasGoodsItems);
    }

    private void cancelOrderSafely(UUID orderId, String reason) {
        try {
            updateOrderStatus(orderId, OrderStatus.CANCELLED.name(), reason);
        } catch (Exception e) {
            log.warn("주문 취소 처리 실패 - orderId: {}, reason: {}", orderId, reason, e);
        }
    }

    private static class OrderCreationResult {
        private final Order order;
        private final boolean hasReservationItems;
        private final boolean hasGoodsItems;

        private OrderCreationResult(Order order, boolean hasReservationItems, boolean hasGoodsItems) {
            this.order = order;
            this.hasReservationItems = hasReservationItems;
            this.hasGoodsItems = hasGoodsItems;
        }

        public Order getOrder() {
            return order;
        }

        public boolean hasReservationItems() {
            return hasReservationItems;
        }

        public boolean hasGoodsItems() {
            return hasGoodsItems;
        }
    }

    private void validateDefaultAddressIfNeeded(CreateOrderCommand command) {
        if (command == null || command.getItems() == null) {
            return;
        }

        boolean requiresShipping = command.getItems().stream()
            .anyMatch(item -> ItemType.GOODS.equals(item.getOrderItemType()));
        if (!requiresShipping) {
            return;
        }

        // 캐시 최적화된 주소 조회 (1000ms → 10ms)
        long startTime = System.currentTimeMillis();
        orderUserAddressCacheService.getDefaultAddress(command.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("기본 배송지가 필요합니다."));

        long elapsed = System.currentTimeMillis() - startTime;
        log.debug("⚡ 기본 주소 검증 완료 - userId: {}, 처리시간: {}ms", command.getUserId(), elapsed);
    }

    @Transactional
    @Idempotent(
        keyExpression = "#orderId + ':' + #status",
        keyPrefix = "order:status",
        ttlSeconds = 300,
        responseType = Order.class
    )
    public Order updateOrderStatus(UUID orderId, String status, String reason) {
        log.info("주문 상태 변경 - 주문ID: {}, 새상태: {}, 이유: {}", orderId, status, reason);

   
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없어요: " + orderId));

        OrderStatus currentStatus = order.getStatus();

      
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new RuntimeException("이미 취소된 주문은 상태를 변경할 수 없어요");
        }

     
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("올바르지 않은 주문 상태예요: " + status);
        }

       
        if (currentStatus == newStatus) {
            log.info("주문 상태 변경 멱등 처리 - 주문ID: {}, 상태: {}", orderId, newStatus);
            return order;
        }

       
        if (!orderDomainService.canChangeStatus(currentStatus, newStatus)) {
            throw new RuntimeException(String.format("상태 변경이 불가능해요: %s → %s", currentStatus, newStatus));
        }

   
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

      
        OrderStatusHistory statusHistory = OrderStatusHistory.builder()
                .orderId(savedOrder.getId())
                .fromStatus(currentStatus)
                .toStatus(newStatus)
                .reason(reason)
                .changedAt(LocalDateTime.now())
                .build();
        orderStatusHistoryRepository.save(statusHistory);

     
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

    
        if (newStatus == OrderStatus.CANCELLED) {
            eventPublisher.publishEvent(new OrderCancelledEvent(
                    savedOrder.getId(),
                    savedOrder.getCustomerId(),
                    currentStatus,
                    reason,
                    "SYSTEM",
                    null 
            ));
        }

        log.info("주문 상태 변경 완료 - 주문ID: {}, {} → {}", orderId, currentStatus, newStatus);
        return savedOrder;
    }

  
    @Transactional
    @Idempotent(
        keyExpression = "#orderId + ':cancel:' + #reason",
        keyPrefix = "order:cancel",
        ttlSeconds = 300,
        responseType = Order.class
    )
    public Order cancelOrder(UUID orderId, String reason) {
        log.info("주문 취소 요청 - 주문ID: {}, 이유: {}", orderId, reason);

   
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없어요: " + orderId));

       
        if (!orderDomainService.canCancelOrder(order)) {
            throw new RuntimeException("취소할 수 없는 주문이에요. 시간이 지났거나 이미 처리된 상태예요.");
        }

        
        return updateOrderStatus(orderId, OrderStatus.CANCELLED.name(), reason);
    }

  
    private List<OrderItem> convertToOrderItems(List<CreateOrderCommand.OrderItemCommand> itemCommands) {
        return itemCommands.stream()
                .map(this::convertToOrderItem)
                .toList();
    }


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
                .goodsId(itemCommand.getGoodsId())
                .build();
    }


    private Integer determineUnitPrice(CreateOrderCommand.OrderItemCommand itemCommand) {
        ItemType itemType = itemCommand.getOrderItemType();

        if (ItemType.RESERVATION.equals(itemType)) {
            // 예약형: 세션 가격 조회
            UUID sessionId = itemCommand.getSessionId();
            if (sessionId == null) {
                throw new RuntimeException("예약형 상품은 세션 정보가 필요해요");
            }
            return getSessionPrice(sessionId);

        } else if (ItemType.GOODS.equals(itemType)) {
            // 구매형: 굿즈 가격 조회
            UUID goodsId = itemCommand.getGoodsId();
            if (goodsId == null) {
                throw new RuntimeException("구매형 상품은 굿즈 정보가 필요해요");
            }
            return getGoodsVariantPrice(goodsId);

        } else {
            throw new RuntimeException("알 수 없는 상품 타입이에요: " + itemType);
        }
    }

  
    private Integer getSessionPrice(UUID sessionId) {
        try {
            long startTime = System.currentTimeMillis();

        
            Integer price = orderPriceCacheService.getSessionPrice(sessionId);

            long elapsed = System.currentTimeMillis() - startTime;
            if (price != null) {
                log.info("⚡ 세션 가격 조회 성공 - sessionId: {}, price: {}원, 처리시간: {}ms",
                        sessionId, price, elapsed);
                return price;
            } else {
                log.warn("세션 가격 정보가 비어있습니다 - sessionId: {}, 기본값 사용, 처리시간: {}ms", sessionId, elapsed);
                return 15000; 
            }
        } catch (Exception e) {
            log.error("세션 가격 조회 실패 - sessionId: {}, 기본값 사용, 에러: {}", sessionId, e.getMessage(), e);
            return 15000; 
        }
    }

    private Integer getGoodsVariantPrice(UUID goodsId) {
        try {
            long startTime = System.currentTimeMillis();

            // 캐시 우선 조회
            Integer price = orderPriceCacheService.getGoodsPrice(goodsId);

            long elapsed = System.currentTimeMillis() - startTime;
            if (price != null) {
                log.info("⚡ 굿즈 가격 조회 성공 - goodsId: {}, price: {}원, 처리시간: {}ms",
                        goodsId, price, elapsed);
                return price;
            } else {
                log.warn("굿즈 가격 정보가 비어있습니다 - goodsId: {}, 기본값 사용, 처리시간: {}ms", goodsId, elapsed);
                return 5000; // Store DB에 넣은 실제 가격과 동일한 기본값
            }
        } catch (Exception e) {
            log.error("굿즈 가격 조회 실패 - goodsId: {}, 기본값 사용, 에러: {}", goodsId, e.getMessage(), e);
            return 5000; // Store DB에 넣은 실제 가격과 동일한 기본값
        }
    }


    
    private String determinePaymentMethod(Order order) {
        // 🚀 Toss Payment로 고정 (카드, 계좌이체, 가상계좌, 휴대폰 결제 모두 지원)
        return "TOSS_PAYMENT";
    }

 
    private void sendPaymentUrlNotificationToCustomer(CreatePaymentResponse paymentResponse) {
        try {
            log.info("고객 결제 URL 알림 발송: 주문ID={}, 결제ID={}, URL={}",
                    paymentResponse.getOrderId(),
                    paymentResponse.getPaymentId(),
                    paymentResponse.getPaymentUrl());

  
        } catch (Exception e) {
            log.error("고객 결제 URL 알림 발송 실패: 주문ID={}",
                    paymentResponse.getOrderId(), e);
        }
    }

  
    private void sendPaymentErrorNotificationToAdmin(UUID orderId, Throwable error) {
        try {
            log.warn("관리자 결제 장애 알림 발송: 주문ID={}, 에러={}",
                    orderId, error.getMessage());

        } catch (Exception e) {
            log.error("관리자 결제 장애 알림 발송 실패: 주문ID={}", orderId, e);
        }
    }


    private CreatePaymentResponse requestPaymentUrl(Order order, String paymentMethod) {
        try {
            log.info("💳 결제 생성 + 토큰 URL 생성 시작 - 주문번호: {}, 금액: {}원, 결제방법: {}",
                    order.getOrderNo(), order.getTotalAmount(), paymentMethod);

            log.info("🔄 결제 생성 이벤트는 재고 예약 완료 후 발행됩니다 - 주문번호: {}", order.getOrderNo());

        
            String customerKey = "customer_" + order.getCustomerId().toString().replace("-", "");

  
            String orderName = generateOrderName(order);

            try {
       
                String paymentToken = paymentTokenUtil.generatePaymentToken(
                        order.getId(),
                        order.getOrderNo(),
                        order.getTotalAmount(),
                        orderName,
                        customerKey,
                        paymentMethod
                );

         
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

  
    @Transactional
    public PaymentUrlResponse generatePaymentUrlAfterReservation(Order order) {
        if (order == null) {
            return null;
        }
        String paymentMethod = determinePaymentMethod(order);
        CreatePaymentResponse paymentResponse = requestPaymentUrl(order, paymentMethod);

        String paymentUrl = paymentResponse != null ? paymentResponse.getPaymentUrl() : null;
        if (paymentUrl == null) {
            return null;
        }

        String token = extractToken(paymentUrl);
        LocalDateTime createdAt = paymentResponse.getCreatedAt() != null
                ? paymentResponse.getCreatedAt() : LocalDateTime.now();
        LocalDateTime expiresAt = paymentResponse.getExpiresAt() != null
                ? paymentResponse.getExpiresAt() : LocalDateTime.now().plusMinutes(30);

        PaymentUrlResponse urlResponse = PaymentUrlResponse.builder()
                .paymentUrl(paymentUrl)
                .token(token)
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .amount(order.getTotalAmount() != null ? order.getTotalAmount().longValue() : null)
                .paymentMethod(paymentMethod)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .expiresInMinutes((int) java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes())
                .build();

        orderCacheService.storePaymentUrl(order.getId(), urlResponse);
        sendPaymentUrlNotificationToCustomer(paymentResponse);
        return urlResponse;
    }

    private String extractToken(String paymentUrl) {
        if (paymentUrl == null) {
            return null;
        }
        int idx = paymentUrl.indexOf("token=");
        if (idx < 0) {
            return null;
        }
        return paymentUrl.substring(idx + "token=".length());
    }

 
    private String generateOrderName(Order order) {
        try {
            List<OrderItem> items = order.getOrderItems();
            if (items.isEmpty()) {
                return "팝콘 주문";
            }

            OrderItem firstItem = items.get(0);
            String itemName;

            if (ItemType.RESERVATION.equals(firstItem.getOrderItemType())) {
                itemName = "팝업 예약";
            } else if (ItemType.GOODS.equals(firstItem.getOrderItemType())) {
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


    private void reserveStockForOrder(Order order) {
        log.info("주문 재고 예약 요청 시작(이벤트) - 주문번호: {}", order.getOrderNo());

        // 굿즈 항목만 필터링 (예약형 상품은 재고 예약 불필요)
        List<OrderItem> goodsItems = order.getOrderItems().stream()
                .filter(item -> ItemType.GOODS.equals(item.getOrderItemType()))
                .toList();

        if (goodsItems.isEmpty()) {
            log.info("굿즈 항목이 없어 재고 예약을 건너뜁니다 - 주문번호: {}", order.getOrderNo());
            return;
        }

        List<com.popcorn.order.event.GoodsReservationRequestedEvent.ReservationItem> reservationItems =
                goodsItems.stream()
                        .filter(item -> item.getGoodsId() != null)
                        .map(item -> com.popcorn.order.event.GoodsReservationRequestedEvent.ReservationItem.create(
                                item.getGoodsId(),
                                item.getQty()
                        ))
                        .toList();

        if (reservationItems.isEmpty()) {
            log.warn("굿즈 변형 ID가 없어 재고 예약 요청을 건너뜁니다 - 주문번호: {}", order.getOrderNo());
            return;
        }

        publishAfterCommit(() -> orderEventPublisher.publishGoodsReservationRequestedEvent(order, reservationItems));

        log.info("주문 재고 예약 요청 이벤트 발행 완료 - 주문번호: {}, items: {}",
                order.getOrderNo(), reservationItems.size());
    }

    private void reserveScheduleForOrder(Order order) {
        log.info("📅 주문 스케줄 예약 요청 시작(이벤트) - 주문번호: {}", order.getOrderNo());

        // 예약 항목만 필터링 (굿즈 항목은 스케줄 예약 불필요)
        List<OrderItem> reservationItems = order.getOrderItems().stream()
                .filter(item -> ItemType.RESERVATION.equals(item.getOrderItemType()))
                .toList();

        if (reservationItems.isEmpty()) {
            log.info("📅 예약 항목이 없어 스케줄 예약을 건너뜁니다 - 주문번호: {}", order.getOrderNo());
            return;
        }

        List<com.popcorn.order.event.ScheduleReservationRequestedEvent.ReservationItem> scheduleReservationItems =
                reservationItems.stream()
                        .filter(item -> item.getSessionOptionId() != null)
                        .map(item -> com.popcorn.order.event.ScheduleReservationRequestedEvent.ReservationItem.create(
                                item.getSessionOptionId(),
                                item.getQty(),
                                generateSessionName(item),
                                null // sessionTime은 Store 서비스에서 조회
                        ))
                        .toList();

        if (scheduleReservationItems.isEmpty()) {
            log.warn("📅 세션 옵션 ID가 없어 스케줄 예약 요청을 건너뜁니다 - 주문번호: {}", order.getOrderNo());
            return;
        }

        publishAfterCommit(() -> orderEventPublisher.publishScheduleReservationRequestedEvent(order, scheduleReservationItems));

        log.info("📅 주문 스케줄 예약 요청 이벤트 발행 완료 - 주문번호: {}, 세션수: {}",
                order.getOrderNo(), scheduleReservationItems.size());
    }

    private String generateSessionName(OrderItem item) {
        return "팝업 세션"; // 실제로는 Store 서비스에서 조회해야 함
    }

    private void publishAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    public void publishPaymentCreateRequestedEvent(UUID orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없어요: " + orderId));

            String paymentMethod = determinePaymentMethod(order);
            orderEventPublisher.publishPaymentCreateRequestedEvent(order, paymentMethod);
        } catch (Exception e) {
            log.error("결제 생성 요청 이벤트 발행 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
        }
    }


    private void rollbackStockReservations(Order order, List<OrderItem> goodsItems, OrderItem failedItem) {
        log.info("재고 예약 롤백 시작 - 주문번호: {}", order.getOrderNo());

        for (OrderItem item : goodsItems) {
            // 실패한 항목에 도달하면 중단
            if (item.equals(failedItem)) {
                break;
            }

            if (item.getGoodsId() == null) {
                continue;
            }

            try {
                log.info("굿즈 재고 예약 취소 시도 - 주문번호: {}, 굿즈변형ID: {}, 수량: {}",
                        order.getOrderNo(), item.getGoodsId(), item.getQty());

                orderEventPublisher.publishGoodsReservationCancelRequestedEvent(
                        order,
                        item.getGoodsId(),
                        item.getQty()
                );

                log.info("굿즈 재고 예약 취소 성공 - 주문번호: {}, 굿즈변형ID: {}",
                        order.getOrderNo(), item.getGoodsId());

            } catch (Exception e) {
                log.error("굿즈 재고 예약 취소 실패 - 주문번호: {}, 굿즈변형ID: {}, 에러: {}",
                        order.getOrderNo(), item.getGoodsId(), e.getMessage(), e);
                // 롤백 실패는 로그만 남기고 계속 진행
            }
        }

        log.info("재고 예약 롤백 완료 - 주문번호: {}", order.getOrderNo());
    }


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


    private void publishStockReservedEvent(Order order, List<OrderItem> goodsItems) {
        try {
            List<StockReservedEvent.ReservedStockItem> reservedItems = goodsItems.stream()
                    .filter(item -> item.getGoodsId() != null)
                    .map(item -> StockReservedEvent.ReservedStockItem.create(
                            item.getGoodsId(),
                            item.getQty(),
                            item.getUnitPrice(),
                            generateProductName(item)
                    ))
                    .collect(java.util.stream.Collectors.toList());


            log.info("재고 예약 완료 - 주문번호: {}, 예약 항목: {}개", order.getOrderNo(), reservedItems.size());

        } catch (Exception e) {
            log.error("재고 예약 성공 이벤트 발행 실패 - 주문번호: {}, 에러: {}",
                    order.getOrderNo(), e.getMessage(), e);
           
        }
    }

   
    private void publishStockReservationFailedEvent(Order order, OrderItem failedItem, String failureReason) {
        try {
            List<StockReservationFailedEvent.FailedStockItem> failedItems = List.of(
                    StockReservationFailedEvent.FailedStockItem.create(
                            failedItem.getGoodsId(),
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
          
        }
    }

   
    private void publishStandardOrderCreatedEvent(Order order) {
        try {
            log.info("🚀 [STANDARD-ORDER] 표준 ORDER_CREATED 이벤트 발행 시작 - 주문번호: {}", order.getOrderNo());

            
            StandardOrderCreatedEvent event = convertToStandardOrderCreatedEvent(order);

         
            standardOrderEventPublisher.publishOrderCreatedEvent(event);

            log.info("✅ [STANDARD-ORDER] 표준 ORDER_CREATED 이벤트 발행 완료 - 주문번호: {}, eventId: {}",
                    order.getOrderNo(), event.getEventId());

        } catch (Exception e) {
            log.error("❌ [STANDARD-ORDER] 표준 ORDER_CREATED 이벤트 발행 실패 - 주문번호: {}, 에러: {}",
                    order.getOrderNo(), e.getMessage(), e);
            // 이벤트 발행 실패는 주문 처리에 영향을 주지 않음
        }
    }

   
    private StandardOrderCreatedEvent convertToStandardOrderCreatedEvent(Order order) {
        // lines 배열 생성
        List<com.popcorn.order.event.standard.EventLineItem> lines = order.getOrderItems().stream()
                .map(this::convertToEventLineItem)
                .collect(java.util.stream.Collectors.toList());

        return StandardOrderCreatedEvent.builder()
                .eventType(com.popcorn.order.event.standard.StandardEventType.ORDER_CREATED)
                .producer("order-service")
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getCustomerId())
                .storeId(null) // 추후 추가
                .popupId(order.getPopupId())
                .orderedAt(order.getCreatedAt())
                .orderStatus(order.getStatus().toString())
                .totalAmount(order.getTotalAmount())
                .hasReservation(hasReservation(order))
                .hasGoods(hasGoods(order))
                .lines(lines)
                .build();
    }

   
    private com.popcorn.order.event.standard.EventLineItem convertToEventLineItem(OrderItem orderItem) {
        return com.popcorn.order.event.standard.EventLineItem.builder()
                .orderGoodsId(orderItem.getId())
                .itemType(orderItem.getOrderItemType().toString())
                .scheduleId(orderItem.getSessionOptionId())
                .goodsId(orderItem.getGoodsId())
                .qty(orderItem.getQty())
                .unitPrice(orderItem.getUnitPrice())
                .linePrice(orderItem.getLineAmount())
                .build();
    }

 
    private Boolean hasReservation(Order order) {
        return order.getOrderItems().stream()
                .anyMatch(item -> ItemType.RESERVATION.equals(item.getOrderItemType()));
    }

 
    private Boolean hasGoods(Order order) {
        return order.getOrderItems().stream()
                .anyMatch(item -> ItemType.GOODS.equals(item.getOrderItemType()));
    }


    public void publishOrderCompletedEvent(UUID orderId) {
        try {
            log.info("주문 완료 이벤트 발행 시작 - orderId: {}", orderId);

            // 주문 조회
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없어요: " + orderId));

            // 주문 완료 이벤트 발행
            orderEventPublisher.publishOrderCompletedEvent(order);

            log.info("✅ 주문 완료 이벤트 발행 완료 - orderId: {}, orderNo: {}", orderId, order.getOrderNo());

        } catch (Exception e) {
            log.error("❌ 주문 완료 이벤트 발행 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
            // 이벤트 발행 실패는 주문 처리에 영향을 주지 않음 (로그만 남김)
        }
    }


    public void cancelStockReservationsForOrder(UUID orderId) {
        try {
            log.info("주문 재고 예약 취소 시작 - orderId: {}", orderId);

            // 주문 조회
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없어요: " + orderId));

            // 주문 항목들 조회
            List<com.popcorn.order.entity.OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

            // 굿즈 항목만 필터링
            List<com.popcorn.order.entity.OrderItem> goodsItems = orderItems.stream()
                    .filter(item -> ItemType.GOODS.equals(item.getOrderItemType()))
                    .toList();

            if (goodsItems.isEmpty()) {
                log.info("굿즈 항목이 없어 재고 예약 취소를 건너뜁니다 - orderId: {}", orderId);
                return;
            }

            // 각 굿즈 항목에 대해 재고 예약 취소
            for (com.popcorn.order.entity.OrderItem item : goodsItems) {
                if (item.getGoodsId() == null) {
                    log.warn("굿즈 변형 ID가 없어 재고 예약 취소를 건너뜁니다 - orderId: {}, 항목ID: {}",
                            orderId, item.getId());
                    continue;
                }

                try {
                    log.info("굿즈 재고 예약 취소 시도 - orderId: {}, 굿즈변형ID: {}, 수량: {}",
                            orderId, item.getGoodsId(), item.getQty());

                    orderEventPublisher.publishGoodsReservationCancelRequestedEvent(
                            order,
                            item.getGoodsId(),
                            item.getQty()
                    );

                    log.info("✅ 굿즈 재고 예약 취소 완료 - orderId: {}, 굿즈변형ID: {}",
                            orderId, item.getGoodsId());

                } catch (Exception e) {
                    log.error("❌ 굿즈 재고 예약 취소 실패 - orderId: {}, 굿즈변형ID: {}, error: {}",
                            orderId, item.getGoodsId(), e.getMessage(), e);
                    // 개별 항목 취소 실패는 전체 처리를 중단시키지 않음
                }
            }

            log.info("✅ 주문 재고 예약 취소 완료 - orderId: {}, 처리된 굿즈 수: {}",
                    orderId, goodsItems.size());

        } catch (Exception e) {
            log.error("❌ 주문 재고 예약 취소 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
            // 재고 예약 취소 실패도 주문 상태 변경에 영향을 주지 않음 (로그만 남김)
        }
    }


    public void cancelPaymentForOrder(UUID orderId, String paymentId, String reason) {
        try {
            log.info("주문 결제 취소 시작 - orderId: {}, paymentId: {}, reason: {}", orderId, paymentId, reason);

            // 주문 조회
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없어요: " + orderId));

            // 결제 취소 요청 이벤트 발행 (Payment 서비스가 처리)
            orderEventPublisher.publishPaymentCancelRequestedEvent(
                    order,
                    paymentId != null ? paymentId : "",
                    reason != null ? reason : "주문 결제 실패로 인한 자동 취소"
            );

            log.info("✅ 결제 취소 요청 이벤트 발행 완료 - orderId: {}, paymentId: {}", orderId, paymentId);

        } catch (Exception e) {
            log.error("❌ 주문 결제 취소 실패 - orderId: {}, paymentId: {}, error: {}",
                    orderId, paymentId, e.getMessage(), e);
            // 결제 취소 실패도 주문 상태 변경에 영향을 주지 않음 (로그만 남김)
        }
    }


    public void requestStockDeduction(UUID orderId) {
        try {
            log.info("주문 재고 차감 요청 시작 - orderId: {}", orderId);

            // 주문 조회
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없어요: " + orderId));

            // 주문 항목들 조회
            List<com.popcorn.order.entity.OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

            // 굿즈 항목만 필터링 (예약형은 재고 차감 불필요)
            List<com.popcorn.order.entity.OrderItem> goodsItems = orderItems.stream()
                    .filter(item -> ItemType.GOODS.equals(item.getOrderItemType()))
                    .filter(item -> item.getGoodsId() != null)
                    .toList();

            if (goodsItems.isEmpty()) {
                log.info("굿즈 항목이 없어 재고 차감을 건너뜁니다 - orderId: {}", orderId);
                return;
            }

            // 재고 차감 항목 리스트 생성
            List<StockDeductionRequestedEvent.StockDeductionItem> deductionItems = goodsItems.stream()
                    .map(item -> StockDeductionRequestedEvent.StockDeductionItem.builder()
                            .goodsId(item.getGoodsId())
                            .quantity(item.getQty())
                            .productName(generateProductName(item))
                            .variantName(generateProductVariantName(item))
                            .build())
                    .toList();

            // 재고 차감 요청 이벤트 발행
            orderEventPublisher.publishStockDeductionRequestedEvent(
                    orderId,
                    order.getOrderNo(),
                    order.getPopupId(),
                    deductionItems
            );

            log.info("✅ 주문 재고 차감 요청 완료 - orderId: {}, 굿즈 항목 수: {}",
                    orderId, deductionItems.size());

        } catch (Exception e) {
            log.error("❌ 주문 재고 차감 요청 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
            // 재고 차감 요청 실패도 주문 상태 변경에 영향을 주지 않음 (로그만 남김)
        }
    }


    public void requestScheduleConfirmation(UUID orderId) {
        try {
            log.info("📅 주문 스케줄 확정 요청 시작 - orderId: {}", orderId);

            // 주문 조회
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없어요: " + orderId));

            // 주문 항목들 조회
            List<com.popcorn.order.entity.OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

            // 스케줄 항목만 필터링 (굿즈는 스케줄 확정 불필요)
            List<com.popcorn.order.entity.OrderItem> scheduleItems = orderItems.stream()
                    .filter(item -> ItemType.RESERVATION.equals(item.getOrderItemType()))
                    .filter(item -> item.getSessionOptionId() != null)
                    .toList();

            if (scheduleItems.isEmpty()) {
                log.info("스케줄 항목이 없어 스케줄 확정을 건너뜁니다 - orderId: {}", orderId);
                return;
            }

            // 스케줄 확정 항목 리스트 생성
            List<ScheduleConfirmationItem> confirmationItems = scheduleItems.stream()
                    .map(item -> ScheduleConfirmationItem.builder()
                            .scheduleId(item.getSessionOptionId())
                            .quantity(item.getQty())
                            .sessionName(generateProductName(item))
                            .sessionTime(generateSessionTimeInfo(item))
                            .build())
                    .collect(java.util.stream.Collectors.toList());

            // 스케줄 확정 요청 이벤트 발행
            orderEventPublisher.publishScheduleConfirmationRequestedEvent(
                    orderId,
                    order.getOrderNo(),
                    order.getPopupId(),
                    confirmationItems
            );

            log.info("✅ 주문 스케줄 확정 요청 완료 - orderId: {}, 스케줄 항목 수: {}",
                    orderId, confirmationItems.size());

        } catch (Exception e) {
            log.error("❌ 주문 스케줄 확정 요청 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
            // 스케줄 확정 요청 실패도 주문 상태 변경에 영향을 주지 않음 (로그만 남김)
        }
    }


    private String generateSessionTimeInfo(OrderItem item) {
        if (item == null || item.getSessionOptionId() == null) {
            return "시간 정보 없음";
        }

        // TODO: 실제 세션 시간 정보 조회 로직 구현 필요
        // 현재는 임시 정보만 반환
        return "세션 " + item.getSessionOptionId().toString().substring(0, 8);
    }


    @lombok.Builder
    public static class ScheduleConfirmationItem {
        private java.util.UUID scheduleId;
        private Integer quantity;
        private String sessionName;
        private String sessionTime;

        public java.util.UUID getScheduleId() { return scheduleId; }
        public Integer getQuantity() { return quantity; }
        public String getSessionName() { return sessionName; }
        public String getSessionTime() { return sessionTime; }
    }


    private String generateProductVariantName(OrderItem item) {
        if (item == null || item.getOrderItemType() == null) {
            return "기본";
        }

        switch (item.getOrderItemType()) {
            case RESERVATION:
                return "예약형";
            case GOODS:
                return "굿즈";
            default:
                return "기본";
        }
    }

}
