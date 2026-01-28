package com.popcorn.order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.popcorn.order.entity.Order;
import com.popcorn.order.entity.OrderItem;
import com.popcorn.order.entity.OrderStatus;
import com.popcorn.order.entity.ItemType;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderDomainService {

    // 상수들 - 비즈니스 규칙을 한 곳에 모아두기
    private static final int RESERVATION_CANCEL_MINUTES = 5;  // 예약형: 5분 후까지 취소 가능
    private static final int PURCHASE_CANCEL_MINUTES = 5;     // 구매형: 5분 후까지 취소 가능

    private static final Map<OrderStatus, EnumSet<OrderStatus>> STATUS_TRANSITIONS = buildStatusTransitions();

   
    private static Map<OrderStatus, EnumSet<OrderStatus>> buildStatusTransitions() {
        Map<OrderStatus, EnumSet<OrderStatus>> transitions = new EnumMap<>(OrderStatus.class);

        // 주문 요청됨 → 다음으로 갈 수 있는 상태들
        transitions.put(OrderStatus.REQUESTED, EnumSet.of(
                OrderStatus.ACCEPTED,        // 수락됨
                OrderStatus.PAYMENT_PENDING, // 결제 대기
                OrderStatus.RESERVED,        // 예약 확정
                OrderStatus.REJECTED,        // 거절됨
                OrderStatus.CANCELLED        // 취소됨
        ));

        // 수락됨 → 다음으로 갈 수 있는 상태들
        transitions.put(OrderStatus.ACCEPTED, EnumSet.of(
                OrderStatus.RESERVED,        // 예약 확정
                OrderStatus.CANCELLED        // 취소
        ));

        // 예약 확정됨 → 다음으로 갈 수 있는 상태들
        transitions.put(OrderStatus.RESERVED, EnumSet.of(
                OrderStatus.PAYMENT_PENDING, // 결제 대기
                OrderStatus.PAID,            // 결제 완료
                OrderStatus.CANCELLED        // 취소
        ));

        // 결제 대기 → 다음으로 갈 수 있는 상태들
        transitions.put(OrderStatus.PAYMENT_PENDING, EnumSet.of(
                OrderStatus.PAID,            // 결제 완료
                OrderStatus.CANCELLED        // 취소
        ));

        // 결제 완료 → 다음으로 갈 수 있는 상태들
        transitions.put(OrderStatus.PAID, EnumSet.of(
                OrderStatus.COMPLETED,       // 완료
                OrderStatus.CANCELLED        // 취소 (5분 이내만 허용)
        ));

        // 더 이상 변경 불가능한 상태들
        transitions.put(OrderStatus.REJECTED, EnumSet.noneOf(OrderStatus.class));
        transitions.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        transitions.put(OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class));

        return transitions;
    }

    public void validateOrderCreation(Long customerId, UUID popupId, List<OrderItem> orderItems) {
        log.debug("주문 생성 검증 시작 - 고객: {}, 팝업: {}", customerId, popupId);


        validateCustomerInfo(customerId);


        validatePopupInfo(popupId);

        validateOrderItems(orderItems);

        log.debug("주문 생성 검증 완료 - 모든 조건 만족!");
    }

   
    private void validateCustomerInfo(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("올바른 고객 정보가 필요해요!");
        }
    }

    private void validatePopupInfo(UUID popupId) {
        if (popupId == null) {
            throw new IllegalArgumentException("팝업 정보가 필요해요!");
        }
    }


    private void validateOrderItems(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("주문할 상품을 최소 1개는 선택해주세요!");
        }

        // 각 항목별로 자세히 확인
        for (OrderItem item : orderItems) {
            validateSingleOrderItem(item);
        }

        // 🚨 핵심 비즈니스 룰 검증 추가
        validateOrderItemsBusinessRules(orderItems);
    }


    private void validateSingleOrderItem(OrderItem item) {
        // 수량 확인
        if (item.getQty() == null || item.getQty() <= 0) {
            throw new IllegalArgumentException("수량은 1개 이상이어야 해요!");
        }

        // 단가 확인
        if (item.getUnitPrice() == null || item.getUnitPrice() <= 0) {
            throw new IllegalArgumentException("상품 가격이 올바르지 않아요!");
        }
    }

    private void validateOrderItemsBusinessRules(List<OrderItem> orderItems) {
        log.debug("주문 항목 비즈니스 룰 검증 시작");

        boolean hasGoods = orderItems.stream()
                .anyMatch(item -> ItemType.GOODS.equals(item.getOrderItemType()));

        boolean hasReservation = orderItems.stream()
                .anyMatch(item -> ItemType.RESERVATION.equals(item.getOrderItemType()));


        if (!hasGoods && !hasReservation) {
            throw new IllegalArgumentException(
                "예약 또는 굿즈 중 최소 하나는 선택해야 합니다."
            );
        }

    
        if (hasReservation && !hasGoods) {
            log.debug("비즈니스 룰 검증 통과: 스케줄 예약만");
        } else if (!hasReservation && hasGoods) {
            log.debug("비즈니스 룰 검증 통과: 굿즈만");
        } else if (hasReservation && hasGoods) {
            log.debug("비즈니스 룰 검증 통과: 스케줄 + 굿즈 복합형");
        }

        log.debug("주문 항목 비즈니스 룰 검증 완료");
    }

 
    public Order createOrder(Long customerId, UUID popupId,
                           ItemType orderType, List<OrderItem> orderItems) {

        log.info("새 주문 만들기 시작 - 고객: {}, 타입: {}", customerId, orderType);

  
        validateOrderCreation(customerId, popupId, orderItems);

     
        LocalDateTime cancelableUntil = calculateCancelableUntil(orderType);
        int totalAmount = calculateTotalAmount(orderItems);

     
        Order order = Order.builder()
                .orderNo(Order.generateOrderNo())        // 주문 번호 자동 생성
                .customerId(customerId)
                .popupId(popupId)
                .orderType(orderType)
                .status(OrderStatus.REQUESTED)           // 처음엔 항상 "요청됨" 상태
                .totalAmount(totalAmount)
                .cancelableUntil(cancelableUntil)
                .build();

    
        order.addOrderItems(new ArrayList<>(orderItems));

        log.info("새 주문 만들기 완료 - 주문번호: {}, 금액: {}원", order.getOrderNo(), totalAmount);
        return order;
    }


    public LocalDateTime calculateCancelableUntil(ItemType orderType) {
        LocalDateTime now = LocalDateTime.now();

        return switch (orderType) {
            case RESERVATION -> now.plusMinutes(RESERVATION_CANCEL_MINUTES);
            case GOODS -> now.plusMinutes(PURCHASE_CANCEL_MINUTES);
            case MIXED -> now.plusMinutes(PURCHASE_CANCEL_MINUTES); // 혼합형은 구매형과 동일한 정책
        };
    }

    public int calculateTotalAmount(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return 0;
        }

     
        return orderItems.stream()
                .mapToInt(item -> item.getUnitPrice() * item.getQty())
                .sum();
    }

    public boolean canChangeStatus(OrderStatus currentStatus, OrderStatus newStatus) {
        // 같은 상태로는 변경할 필요가 없어요
        if (currentStatus == newStatus) {
            return false;
        }

        // 허용된 전환 목록에서 확인
        EnumSet<OrderStatus> allowedStatuses = STATUS_TRANSITIONS.get(currentStatus);
        boolean canChange = allowedStatuses != null && allowedStatuses.contains(newStatus);

        log.debug("상태 변경 가능성 확인: {} → {} = {}", currentStatus, newStatus, canChange);
        return canChange;
    }

 
    public boolean canCancelOrder(Order order) {
        // 시간 조건 확인
        boolean withinTimeLimit = order.isCancelable();

        // 상태 전환 조건 확인
        boolean statusAllowsCancellation = canChangeStatus(order.getStatus(), OrderStatus.CANCELLED);

        boolean canCancel = withinTimeLimit && statusAllowsCancellation;

        log.debug("주문 취소 가능성 확인 - 주문번호: {}, 시간조건: {}, 상태조건: {}, 결과: {}",
                order.getOrderNo(), withinTimeLimit, statusAllowsCancellation, canCancel);

        return canCancel;
    }

 
    public boolean canCancelPaidOrder(Order order) {
        return order.getStatus() == OrderStatus.PAID && order.isCancelable();
    }

    // ================ 유틸리티 메서드들 ================

   
    public boolean isOrderCompleted(Order order) {
        return order.getStatus() == OrderStatus.COMPLETED;
    }

  
    public boolean isOrderInProgress(Order order) {
        OrderStatus status = order.getStatus();
        return status != OrderStatus.CANCELLED
                && status != OrderStatus.REJECTED
                && status != OrderStatus.COMPLETED;
    }

    public boolean isPaymentRelatedStatus(OrderStatus status) {
        return status == OrderStatus.PAYMENT_PENDING || status == OrderStatus.PAID;
    }

}
