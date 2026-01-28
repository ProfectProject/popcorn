package com.popcorn.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.order.dto.command.CreateOrderCommand;
import com.popcorn.order.dto.response.OrderCreateResponse;
import com.popcorn.order.entity.Order;
import com.popcorn.order.entity.OrderStatus;
import com.popcorn.order.entity.ItemType;
import com.popcorn.order.repository.OrderRepository;
import com.popcorn.order.repository.OrderStatusHistoryRepository;
import com.popcorn.order.event.OrderEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    // 데이터베이스 작업을 위한 도구들
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    // 이벤트 발행을 위한 도구
    private final OrderEventPublisher orderEventPublisher;

    // 주문 관련 상수들 - 한 곳에서 관리하면 나중에 바꾸기 쉬워요
    private static final int DEFAULT_CANCEL_MINUTES = 30;  // 기본 취소 가능 시간 (30분)
    private static final int DEFAULT_ITEM_PRICE = 10000;   // 임시 기본 가격

   
    @Transactional  // 데이터베이스 작업이 안전하게 처리되도록 보장
    public OrderCreateResponse createOrder(CreateOrderCommand command) {
        // 로그 찍기 - 어떤 일이 일어나는지 기록해둬요
        log.info("새 주문 만들기 시작! 사용자: {}, 팝업: {}",
                command.getUserId(), command.getPopupId());

        try {
            // 단계 1: 주문 정보가 올바른지 확인하기
            checkOrderInfo(command);

            // 단계 2: 주문 만들기
            Order newOrder = createOrderEntity(command);

            // 단계 3: 데이터베이스에 저장하기
            Order savedOrder = saveOrderToDatabase(newOrder);

            // 단계 4: 응답 만들어서 돌려주기
            OrderCreateResponse response = createOrderResponse(savedOrder);

            log.info("주문 만들기 성공! 주문번호: {}", response.getOrderNo());
            return response;

        } catch (Exception error) {
            // 뭔가 잘못됐을 때는 로그에 남기고 에러를 던져요
            log.error("주문 만들기 실패 - 사용자: {}, 에러: {}",
                     command.getUserId(), error.getMessage());
            throw new RuntimeException("주문을 만드는 중에 문제가 생겼어요: " + error.getMessage());
        }
    }

   
    private void checkOrderInfo(CreateOrderCommand command) {
        // 아예 정보가 없으면 안돼요
        if (command == null) {
            throw new IllegalArgumentException("주문 정보를 보내주세요!");
        }

        // 누가 주문하는지 알아야 해요
        if (command.getUserId() == null) {
            throw new IllegalArgumentException("사용자 정보가 필요해요!");
        }

        // 어떤 팝업에서 주문하는지 알아야 해요
        if (command.getPopupId() == null) {
            throw new IllegalArgumentException("팝업 정보가 필요해요!");
        }

        // 예약인지 구매인지 알아야 해요
        if (isEmptyString(command.getOrderType())) {
            throw new IllegalArgumentException("주문 타입을 선택해주세요!");
        }

        // 뭘 주문할지 알아야 해요
        if (isEmptyList(command.getItems())) {
            throw new IllegalArgumentException("주문할 상품을 최소 1개는 선택해주세요!");
        }

        log.debug("주문 정보 확인 완료 - 모든 정보가 올바릅니다!");
    }

  
    private boolean isEmptyString(String text) {
        return text == null || text.trim().isEmpty();
    }

   
    private boolean isEmptyList(java.util.List<?> list) {
        return list == null || list.isEmpty();
    }

 
    private Order createOrderEntity(CreateOrderCommand command) {
        // 주문 번호 자동 생성
        String orderNo = Order.generateOrderNo();

        // 주문 타입 변환
        ItemType orderType = ItemType.valueOf(command.getOrderType());

        // 총 금액 계산 (임시로 고정값 사용)
        Integer totalAmount = calculateTotalAmount(command);

        // 취소 가능 시간 설정 (30분 후)
        LocalDateTime cancelableUntil = LocalDateTime.now().plusMinutes(30);

        return Order.builder()
                .orderNo(orderNo)
                .customerId(command.getUserId())
                .popupId(command.getPopupId())
                .orderType(orderType)
                .status(OrderStatus.REQUESTED)
                .totalAmount(totalAmount)
                .cancelableUntil(cancelableUntil)
                .build();
    }

 
    private Integer calculateTotalAmount(CreateOrderCommand command) {
        // TODO: 실제 가격 계산 로직 구현
        // - 각 항목의 단가 조회
        // - 수량과 곱셈
        // - 할인 적용
        // - 배송비 추가 등

        return 10000; // 임시 고정값
    }

   
    private Order saveOrderToDatabase(Order order) {
        log.debug("주문 데이터베이스 저장 시작 - 주문번호: {}", order.getOrderNo());

        try {
            Order savedOrder = orderRepository.save(order);
            log.info("주문 저장 완료 - ID: {}, 주문번호: {}", savedOrder.getId(), savedOrder.getOrderNo());
            return savedOrder;
        } catch (Exception e) {
            log.error("주문 저장 실패 - 주문번호: {}", order.getOrderNo(), e);
            throw new RuntimeException("주문 저장 중 오류가 발생했습니다", e);
        }
    }

    private OrderCreateResponse createOrderResponse(Order order) {
        log.debug("주문 응답 생성 - 주문번호: {}", order.getOrderNo());

        return OrderCreateResponse.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .popupId(order.getPopupId())
                .orderType(order.getOrderType().name())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .cancelableUntil(order.getCancelableUntil())
                .createdAt(order.getCreatedAt())
                .items(List.of()) // TODO: 실제 주문 항목 구현 후 수정
                .build();
    }


    @Transactional
    public void updateOrderStatus(UUID orderId, String status, String reason) {
        log.info("주문 상태 변경 - ID: {}, 상태: {}, 사유: {}",
                orderId, status, reason);

        try {
          

            log.info("주문 상태 변경 완료 - ID: {}", orderId);

        } catch (Exception e) {
            log.error("주문 상태 변경 실패 - ID: {}", orderId, e);
            throw new RuntimeException("주문 상태 변경 중 오류가 발생했습니다", e);
        }
    }

   
    @Transactional
    public void handlePaymentCompleted(UUID orderId) {
        try {
            log.info("결제 완료 처리 시작 - orderId: {}", orderId);

           
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

         
            if (order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
                log.warn("잘못된 주문 상태 - 현재 상태: {}, 주문ID: {}",
                        order.getOrderStatus(), orderId);
                throw new IllegalStateException("결제 처리가 가능한 주문 상태가 아닙니다.");
            }

          
            OrderStatus oldStatus = order.getOrderStatus();
            order.markAsPaid();

            Order savedOrder = orderRepository.save(order);

           
            saveOrderStatusHistory(order, oldStatus, "결제 완료");

          
            orderEventPublisher.publishOrderPaidEvent(savedOrder);

            log.info("결제 완료 처리 성공 - orderId: {}, 상태: {} -> {}",
                    orderId, oldStatus, OrderStatus.PAID);

        } catch (Exception e) {
            log.error("결제 완료 처리 실패 - orderId: {}", orderId, e);
            throw new RuntimeException("결제 완료 처리 중 오류가 발생했습니다", e);
        }
    }

  
    @Transactional
    public void handleOrderCancellation(UUID orderId, String reason) {
        try {
            log.info("주문 취소 처리 시작 - orderId: {}, reason: {}", orderId, reason);

           
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

           
            if (order.isCancelled()) {
                log.warn("이미 취소된 주문입니다 - orderId: {}", orderId);
                return;
            }

           
            OrderStatus oldStatus = order.getOrderStatus();
            order.markAsCancelled(reason);

            Order savedOrder = orderRepository.save(order);

           
            saveOrderStatusHistory(order, oldStatus, reason);

           
            orderEventPublisher.publishOrderCancelledEvent(savedOrder, reason);

            log.info("주문 취소 처리 성공 - orderId: {}, 상태: {} -> {}, 사유: {}",
                    orderId, oldStatus, OrderStatus.CANCELLED, reason);

        } catch (Exception e) {
            log.error("주문 취소 처리 실패 - orderId: {}, reason: {}", orderId, reason, e);
            throw new RuntimeException("주문 취소 처리 중 오류가 발생했습니다", e);
        }
    }

   
    @Transactional
    public void handleOrderConfirmation(UUID orderId) {
        try {
            log.info("주문 확정 처리 시작 - orderId: {}", orderId);

           
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

      
            if (!order.isPaid()) {
                log.warn("주문 확정 불가 - 현재 상태: {}, 주문ID: {}",
                        order.getOrderStatus(), orderId);
                throw new IllegalStateException("결제가 완료되지 않은 주문은 확정할 수 없습니다.");
            }

            // 3. 주문 상태를 CONFIRMED로 변경
            OrderStatus oldStatus = order.getOrderStatus();
            order.markAsConfirmed();

            Order savedOrder = orderRepository.save(order);

            // 4. 주문 상태 변경 이력 기록
            saveOrderStatusHistory(order, oldStatus, "재고 차감 성공 - 주문 확정");

            // 5. OrderCompletedEvent 발행 (알림 발송)
            orderEventPublisher.publishOrderCompletedEvent(savedOrder);

            log.info("주문 확정 처리 성공 - orderId: {}, 상태: {} -> {}",
                    orderId, oldStatus, OrderStatus.COMPLETED);

        } catch (Exception e) {
            log.error("주문 확정 처리 실패 - orderId: {}", orderId, e);
            throw new RuntimeException("주문 확정 처리 중 오류가 발생했습니다", e);
        }
    }

    
    private void saveOrderStatusHistory(Order order, OrderStatus oldStatus, String reason) {
        try {
            orderStatusHistoryRepository.save(order.toHistory(oldStatus, reason));
        } catch (Exception e) {
            log.error("주문 상태 이력 저장 실패 - orderId: {}", order.getId(), e);
            // 이력 저장 실패는 주요 로직을 중단시키지 않음
        }
    }

}
