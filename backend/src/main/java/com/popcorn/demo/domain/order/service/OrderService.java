package com.popcorn.demo.domain.order.service;

import com.popcorn.demo.domain.order.dto.CreateOrderRequest;
import com.popcorn.demo.domain.order.dto.OrderCreatedDto;
import com.popcorn.demo.domain.order.entity.*;
import com.popcorn.demo.domain.order.exception.OrderException;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 주문 서비스 - DI 및 비동기 처리 통합 버전
 *
 * 주요 기능:
 * - 동기적 주문 생성 처리
 * - 비동기 후처리 작업 연동
 * - 트랜잭션 관리
 * - 의존성 주입 활용
 */
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderAsyncService orderAsyncService;

    /**
     * 생성자 기반 의존성 주입
     * - OrderRepository: 주문 데이터 저장/조회
     * - OrderAsyncService: 비동기 후처리 작업
     */
    @Autowired
    public OrderService(OrderRepository orderRepository, OrderAsyncService orderAsyncService) {
        this.orderRepository = orderRepository;
        this.orderAsyncService = orderAsyncService;
    }

    /**
     * 새로운 주문을 생성합니다. (DI 및 비동기 처리 통합 버전)
     *
     * 처리 순서:
     * 1. 기본 검증
     * 2. 비동기 검증 (재고, 고객 정보 등)
     * 3. 주문 엔티티 생성 및 저장
     * 4. 비동기 후처리 작업 실행 (재고 차감, 알림 등)
     *
     * @param userId 사용자 ID
     * @param request 주문 생성 요청
     * @param idempotencyKey 멱등성 키
     * @return 생성된 주문 정보
     */
    public OrderCreatedDto createOrder(Long userId, CreateOrderRequest request, String idempotencyKey) {
        // 1. 기본 검증
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw OrderException.emptyItems();
        }

        // 2. 비동기 검증 실행 (첫 번째 아이템 기준으로 검증)
        var firstItem = request.getItems().get(0);
        CompletableFuture<Boolean> validationFuture = orderAsyncService.validateOrderAsync(
            userId,
            request.getProductId(),
            firstItem.getQty()
        );

        // 3. 주문 엔티티 생성
        Order order = Order.builder()
                .orderNo(Order.generateOrderNo())
                .customerId(userId)
                .storeId(request.getStoreId())
                .productId(request.getProductId())
                .orderType(OrderType.valueOf(request.getOrderType()))
                .status(OrderStatus.REQUESTED)
                .totalAmount(29000) // 임시 고정값
                .cancelableUntil(LocalDateTime.now().plusHours(1))
                .build();

        // 4. 주소 정보는 현재 스키마에 없음 - 향후 별도 테이블로 관리 예정
        // TODO: 구매형 주문의 주소 정보는 별도 주문 배송 테이블에 저장

        // 5. 주문 아이템들 생성
        List<OrderItem> orderItems = new ArrayList<>();
        for (var itemRequest : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .orderItemType(OrderItemType.valueOf(itemRequest.getOrderItemType()))
                    .sessionOptionId(itemRequest.getSessionId()) // session + option 통합
                    .merchVariantId(itemRequest.getMerchVariantId())
                    .qty(itemRequest.getQty())
                    .unitPrice(14500) // 임시 고정값
                    .lineAmount(14500 * itemRequest.getQty())
                    .build();
            orderItems.add(item);
        }
        order.setOrderItems(orderItems);

        // 6. 주문 저장
        Order savedOrder = orderRepository.save(order);

        // 7. 비동기 후처리 작업 실행 (Fire-and-Forget)
        orderAsyncService.processOrderPostActions(savedOrder.getId())
                .thenRun(() -> System.out.println("주문 " + savedOrder.getId() + " 후처리 작업 완료"));

        // 8. 응답 DTO 생성
        List<OrderCreatedDto.OrderItemDto> itemDtos = savedOrder.getOrderItems().stream()
                .map(item -> new OrderCreatedDto.OrderItemDto(
                        item.getId(),
                        item.getOrderItemType().name(),
                        item.getQty(),
                        item.getUnitPrice(),
                        item.getLineAmount()
                ))
                .toList();

        return new OrderCreatedDto(
                savedOrder.getId(),
                savedOrder.getOrderNo(),
                savedOrder.getOrderType().name(),
                savedOrder.getStatus().name(),
                savedOrder.getStoreId(),
                savedOrder.getProductId(),
                savedOrder.getTotalAmount(),
                savedOrder.getCancelableUntil(),
                savedOrder.getCreatedAt(),
                itemDtos
        );
    }
}