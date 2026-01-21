package com.popcorn.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.order.dto.query.OrderListQuery;
import com.popcorn.order.dto.response.OrderDetailResponse;
import com.popcorn.order.dto.response.OrderListResponse;
import com.popcorn.order.dto.response.OrderSummaryResponse;
import com.popcorn.order.entity.Order;
import com.popcorn.order.entity.OrderType;
import com.popcorn.order.entity.OrderStatus;
import com.popcorn.order.entity.OrderStatusHistory;
import com.popcorn.order.repository.OrderRepository;
import com.popcorn.order.repository.OrderStatusHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 조회(Query) 서비스 - CQRS 패턴의 조회 쪽 담당
 *
 * CQRS 패턴에서는 명령(Command)과 조회(Query)를 분리해요:
 * - OrderCommandService: 데이터 변경 (생성, 수정, 삭제)
 * - OrderQueryService: 데이터 조회 (읽기 전용)
 *
 * 이렇게 분리하는 이유:
 * - 복잡한 조회 로직과 변경 로직을 분리해서 이해하기 쉬워져요
 * - 조회 성능을 최적화하기 쉬워져요
 * - 각각 독립적으로 확장할 수 있어요
 * - 보안상 조회와 변경 권한을 다르게 관리할 수 있어요
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // 모든 메서드가 읽기 전용임을 명시
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderDomainService orderDomainService;

    // ================ 단일 주문 조회 ================

    /**
     * 주문 ID로 주문 상세 정보 조회
     *
     * @param orderId 조회할 주문 ID
     * @return 주문 상세 정보 (없으면 Optional.empty())
     */
    public Optional<OrderDetailResponse> findOrderById(UUID orderId) {
        log.debug("주문 상세 조회 - ID: {}", orderId);

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            log.warn("주문을 찾을 수 없음 - ID: {}", orderId);
            return Optional.empty();
        }

        Order order = orderOpt.get();
        List<OrderStatusHistory> statusHistories = orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId);

        OrderDetailResponse response = OrderDetailResponse.fromOrder(order, statusHistories);
        log.debug("주문 상세 조회 완료 - 주문번호: {}", order.getOrderNo());

        return Optional.of(response);
    }

    /**
     * 주문 번호로 주문 조회
     *
     * @param orderNo 주문 번호 (O20260120-000001 형태)
     * @return 주문 상세 정보
     */
    public Optional<OrderDetailResponse> findOrderByOrderNo(String orderNo) {
        log.debug("주문 조회 - 주문번호: {}", orderNo);

        Optional<Order> orderOpt = orderRepository.findByOrderNo(orderNo);
        if (orderOpt.isEmpty()) {
            log.warn("주문을 찾을 수 없음 - 주문번호: {}", orderNo);
            return Optional.empty();
        }

        return findOrderById(orderOpt.get().getId());
    }

    // ================ 주문 목록 조회 ================

    /**
     * 사용자의 주문 목록 조회 (페이징)
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 주문 요약 목록
     */
    public Page<OrderSummaryResponse> findOrdersByUserId(Long userId, Pageable pageable) {
        log.debug("사용자 주문 목록 조회 - 사용자: {}, 페이지: {}", userId, pageable.getPageNumber());

        Page<Order> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(userId, pageable);

        return orders.map(order -> {
            // 각 주문에 대해 간단한 요약 정보만 조회
            return OrderSummaryResponse.fromOrder(order);
        });
    }

    /**
     * 팝업별 주문 목록 조회
     *
     * @param popupId 팝업 ID
     * @param pageable 페이징 정보
     * @return 주문 목록
     */
    public Page<OrderSummaryResponse> findOrdersByPopupId(UUID popupId, Pageable pageable) {
        log.debug("팝업 주문 목록 조회 - 팝업: {}", popupId);

        Page<Order> orders = orderRepository.findByPopupIdOrderByCreatedAtDesc(popupId, pageable);
        return orders.map(OrderSummaryResponse::fromOrder);
    }

    // ================ 조건부 조회 ================

    /**
     * 취소 가능한 주문들 조회
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 취소 가능한 주문 목록
     */
    public Page<OrderSummaryResponse> findCancellableOrdersByUserId(Long userId, Pageable pageable) {
        log.debug("취소 가능한 주문 조회 - 사용자: {}", userId);

        LocalDateTime now = LocalDateTime.now();
        Page<Order> orders = orderRepository.findCancellableOrdersByCustomerId(userId, now, pageable);

        return orders.map(OrderSummaryResponse::fromOrder);
    }

    // ================ 통계 및 집계 ================

    /**
     * 사용자의 총 주문 수 조회
     *
     * @param userId 사용자 ID
     * @return 총 주문 수
     */
    public long countOrdersByUserId(Long userId) {
        log.debug("사용자 총 주문 수 조회 - 사용자: {}", userId);
        return orderRepository.countByCustomerId(userId);
    }

    // ================ 비즈니스 로직 조회 ================

    /**
     * 주문이 취소 가능한지 확인
     *
     * @param orderId 주문 ID
     * @return 취소 가능 여부
     */
    public boolean isOrderCancellable(UUID orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return false;
        }

        Order order = orderOpt.get();
        return orderDomainService.canCancelOrder(order);
    }

    /**
     * 주문의 현재 상태 조회
     *
     * @param orderId 주문 ID
     * @return 현재 주문 상태 (주문이 없으면 Optional.empty())
     */
    public Optional<OrderStatus> getOrderStatus(UUID orderId) {
        return orderRepository.findById(orderId)
                .map(Order::getStatus);
    }

    /**
     * 주문 상태 변경 이력 조회
     *
     * @param orderId 주문 ID
     * @return 상태 변경 이력 목록
     */
    public List<OrderStatusHistory> getOrderStatusHistory(UUID orderId) {
        log.debug("주문 상태 이력 조회 - 주문: {}", orderId);
        return orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId);
    }

    // ================ 관리자용 조회 ================

    /**
     * 처리가 필요한 주문들 조회 (관리자용)
     *
     * 예: 오랫동안 REQUESTED 상태인 주문들
     *
     * @param hours 체크할 시간 (몇 시간 전부터)
     * @param pageable 페이징 정보
     * @return 처리 필요한 주문 목록
     */
    public Page<OrderSummaryResponse> findOrdersNeedingAttention(int hours, Pageable pageable) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        log.debug("처리 필요 주문 조회 - {} 시간 전부터", hours);

        Page<Order> orders = orderRepository.findRequestedOrdersOlderThan(cutoffTime, pageable);
        return orders.map(OrderSummaryResponse::fromOrder);
    }

    /**
     * 주문이 존재하는지 확인
     *
     * @param orderId 주문 ID
     * @return 존재 여부
     */
    public boolean existsById(UUID orderId) {
        return orderRepository.existsById(orderId);
    }

    /**
     * 주문 번호가 존재하는지 확인
     *
     * @param orderNo 주문 번호
     * @return 존재 여부
     */
    public boolean existsByOrderNo(String orderNo) {
        return orderRepository.existsByOrderNo(orderNo);
    }

    // ================ 새로운 Store 서비스 방식 조회 (Pagination) ================

    /**
     * 팝업별 주문 목록 조회 (Store 서비스 방식)
     *
     * @param query 조회 조건
     * @return Store 서비스 방식의 주문 목록 응답
     */
    public OrderListResponse findOrdersByPopupId(OrderListQuery query) {
        log.info("팝업별 주문 목록 조회 - 팝업ID: {}, 페이지: {}, 사이즈: {}",
                query.getPopupId(), query.getValidatedPage(), query.getValidatedSize());

        // 1. 페이지네이션 파라미터 검증
        int page = query.getValidatedPage();
        int size = query.getValidatedSize();

        try {
            // 2. Pageable 생성 (Spring Page는 0부터 시작하므로 -1)
            Pageable pageable = PageRequest.of(page - 1, size);

            // 3. 조건에 맞는 주문 목록 조회
            Page<Order> orderPage = orderRepository.findOrdersByPopupIdWithConditions(
                    query.getPopupId(),
                    query.getStatus(),
                    query.getOrderType(),
                    pageable
            );

            // 4. 전체 개수 (withTotal=false인 경우 -1 반환)
            long total = query.getValidatedWithTotal() ? orderPage.getTotalElements() : -1L;

            // 5. Store 서비스 방식으로 응답 생성
            OrderListResponse response = OrderListResponse.from(
                    orderPage.getContent(), page, size, total);

            log.info("팝업 주문 목록 조회 완료 - 팝업ID: {}, 조회된 주문: {}개, 전체: {}",
                    query.getPopupId(), orderPage.getContent().size(), total);

            return response;

        } catch (Exception e) {
            log.error("팝업 주문 목록 조회 실패 - 팝업ID: {}, 에러: {}", query.getPopupId(), e.getMessage(), e);
            // 에러 시 빈 응답 반환
            return OrderListResponse.from(List.of(), page, size, 0L);
        }
    }

    /**
     * 카테고리별 주문 목록 조회 (Store 서비스 방식)
     *
     * @param query 조회 조건
     * @return Store 서비스 방식의 주문 목록 응답
     */
    public OrderListResponse findOrdersByCategory(OrderListQuery query) {
        log.info("카테고리별 주문 목록 조회 - 카테고리: {}, 페이지: {}, 사이즈: {}",
                query.getOrderType(), query.getValidatedPage(), query.getValidatedSize());

        // 1. 페이지네이션 파라미터 검증
        int page = query.getValidatedPage();
        int size = query.getValidatedSize();

        try {
            // 2. OrderType 검증
            OrderType orderType = null;
            if (query.getOrderType() != null) {
                try {
                    orderType = OrderType.valueOf(query.getOrderType());
                } catch (IllegalArgumentException e) {
                    log.warn("잘못된 주문 타입: {}", query.getOrderType());
                    return OrderListResponse.from(List.of(), page, size, 0L);
                }
            }

            // 3. Pageable 생성
            Pageable pageable = PageRequest.of(page - 1, size);

            // 4. 조건에 맞는 주문 목록 조회
            Page<Order> orderPage = orderRepository.findOrdersByCategoryWithConditions(
                    orderType,
                    query.getStatus(),
                    query.getUserId(),
                    query.getFrom(),
                    query.getTo(),
                    pageable
            );

            // 5. 전체 개수 (withTotal=false인 경우 -1 반환)
            long total = query.getValidatedWithTotal() ? orderPage.getTotalElements() : -1L;

            // 6. Store 서비스 방식으로 응답 생성
            OrderListResponse response = OrderListResponse.from(
                    orderPage.getContent(), page, size, total);

            log.info("카테고리 주문 목록 조회 완료 - 카테고리: {}, 조회된 주문: {}개, 전체: {}",
                    query.getOrderType(), orderPage.getContent().size(), total);

            return response;

        } catch (Exception e) {
            log.error("카테고리 주문 목록 조회 실패 - 카테고리: {}, 에러: {}", query.getOrderType(), e.getMessage(), e);
            // 에러 시 빈 응답 반환
            return OrderListResponse.from(List.of(), page, size, 0L);
        }
    }

    /**
     * 통합 주문 목록 조회 (Store 서비스 방식)
     * 모든 검색 조건을 지원하는 범용 메서드
     *
     * @param query 조회 조건
     * @return Store 서비스 방식의 주문 목록 응답
     */
    public OrderListResponse findOrdersWithQuery(OrderListQuery query) {
        log.info("통합 주문 목록 조회 - 팝업: {}, 타입: {}, 상태: {}, 사용자: {}, 페이지: {}",
                query.getPopupId(), query.getOrderType(), query.getStatus(),
                query.getUserId(), query.getValidatedPage());

        int page = query.getValidatedPage();
        int size = query.getValidatedSize();

        try {
            // OrderType 검증
            OrderType orderType = null;
            if (query.getOrderType() != null) {
                try {
                    orderType = OrderType.valueOf(query.getOrderType());
                } catch (IllegalArgumentException e) {
                    log.warn("잘못된 주문 타입: {}", query.getOrderType());
                    return OrderListResponse.from(List.of(), page, size, 0L);
                }
            }

            // Pageable 생성
            Pageable pageable = PageRequest.of(page - 1, size);

            // 통합 조회 (모든 조건 지원)
            Page<Order> orderPage = orderRepository.findOrdersWithAllConditions(
                    query.getPopupId(),
                    orderType,
                    query.getStatus(),
                    query.getUserId(),
                    query.getStoreId(),
                    query.getFrom(),
                    query.getTo(),
                    pageable
            );

            // 전체 개수
            long total = query.getValidatedWithTotal() ? orderPage.getTotalElements() : -1L;

            return OrderListResponse.from(orderPage.getContent(), page, size, total);

        } catch (Exception e) {
            log.error("통합 주문 목록 조회 실패 - 에러: {}", e.getMessage(), e);
            return OrderListResponse.from(List.of(), page, size, 0L);
        }
    }
}
