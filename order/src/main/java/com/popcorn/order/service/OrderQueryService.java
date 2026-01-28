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
import com.popcorn.order.entity.ItemType;
import com.popcorn.order.entity.OrderStatus;
import com.popcorn.order.entity.OrderStatusHistory;
import com.popcorn.order.repository.OrderRepository;
import com.popcorn.order.repository.OrderStatusHistoryRepository;
import com.popcorn.order.dto.store.PopupInfoResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // 모든 메서드가 읽기 전용임을 명시
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderDomainService orderDomainService;
    private final OrderPopupLookupService orderPopupLookupService;

   
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

 
    public Optional<OrderDetailResponse> findOrderByOrderNo(String orderNo) {
        log.debug("주문 조회 - 주문번호: {}", orderNo);

        Optional<Order> orderOpt = orderRepository.findByOrderNo(orderNo);
        if (orderOpt.isEmpty()) {
            log.warn("주문을 찾을 수 없음 - 주문번호: {}", orderNo);
            return Optional.empty();
        }

        return findOrderById(orderOpt.get().getId());
    }

    public Page<OrderSummaryResponse> findOrdersByUserId(Long userId, Pageable pageable) {
        log.debug("사용자 주문 목록 조회 - 사용자: {}, 페이지: {}", userId, pageable.getPageNumber());

        Page<Order> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(userId, pageable);

        return orders.map(order -> {
            // 각 주문에 대해 간단한 요약 정보만 조회
            return OrderSummaryResponse.fromOrder(order);
        });
    }

 
    public Page<OrderSummaryResponse> findOrdersByPopupId(UUID popupId, Pageable pageable) {
        log.debug("팝업 주문 목록 조회 - 팝업: {}", popupId);

        Page<Order> orders = orderRepository.findByPopupIdOrderByCreatedAtDesc(popupId, pageable);
        return orders.map(OrderSummaryResponse::fromOrder);
    }

   
    public Page<OrderSummaryResponse> findCancellableOrdersByUserId(Long userId, Pageable pageable) {
        log.debug("취소 가능한 주문 조회 - 사용자: {}", userId);

        LocalDateTime now = LocalDateTime.now();
        Page<Order> orders = orderRepository.findCancellableOrdersByCustomerId(userId, now, pageable);

        return orders.map(OrderSummaryResponse::fromOrder);
    }

  
    public long countOrdersByUserId(Long userId) {
        log.debug("사용자 총 주문 수 조회 - 사용자: {}", userId);
        return orderRepository.countByCustomerId(userId);
    }

    // ================ 비즈니스 로직 조회 ================

  
    public boolean isOrderCancellable(UUID orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return false;
        }

        Order order = orderOpt.get();
        return orderDomainService.canCancelOrder(order);
    }

    public Optional<OrderStatus> getOrderStatus(UUID orderId) {
        return orderRepository.findById(orderId)
                .map(Order::getStatus);
    }

   
    public List<OrderStatusHistory> getOrderStatusHistory(UUID orderId) {
        log.debug("주문 상태 이력 조회 - 주문: {}", orderId);
        return orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId);
    }

  
    public Page<OrderSummaryResponse> findOrdersNeedingAttention(int hours, Pageable pageable) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        log.debug("처리 필요 주문 조회 - {} 시간 전부터", hours);

        Page<Order> orders = orderRepository.findRequestedOrdersOlderThan(cutoffTime, pageable);
        return orders.map(OrderSummaryResponse::fromOrder);
    }

  
    public boolean existsById(UUID orderId) {
        return orderRepository.existsById(orderId);
    }

 
    public boolean existsByOrderNo(String orderNo) {
        return orderRepository.existsByOrderNo(orderNo);
    }

   
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

 
    public OrderListResponse findOrdersByCategory(OrderListQuery query) {
        log.info("카테고리별 주문 목록 조회 - 카테고리: {}, 페이지: {}, 사이즈: {}",
                query.getOrderType(), query.getValidatedPage(), query.getValidatedSize());

        
        int page = query.getValidatedPage();
        int size = query.getValidatedSize();

        try {
          
            ItemType orderType = null;
            if (query.getOrderType() != null) {
                try {
                    orderType = ItemType.valueOf(query.getOrderType());
                } catch (IllegalArgumentException e) {
                    log.warn("잘못된 주문 타입: {}", query.getOrderType());
                    return OrderListResponse.from(List.of(), page, size, 0L);
                }
            }

          
            Pageable pageable = PageRequest.of(page - 1, size);

            
            Page<Order> orderPage = orderRepository.findOrdersByCategoryWithConditions(
                    orderType,
                    query.getStatus(),
                    query.getUserId(),
                    query.getFrom(),
                    query.getTo(),
                    pageable
            );

           
            long total = query.getValidatedWithTotal() ? orderPage.getTotalElements() : -1L;

           
            OrderListResponse response = OrderListResponse.from(
                    orderPage.getContent(), page, size, total);

            log.info("카테고리 주문 목록 조회 완료 - 카테고리: {}, 조회된 주문: {}개, 전체: {}",
                    query.getOrderType(), orderPage.getContent().size(), total);

            return response;

        } catch (Exception e) {
            log.error("카테고리 주문 목록 조회 실패 - 카테고리: {}, 에러: {}", query.getOrderType(), e.getMessage(), e);
           
            return OrderListResponse.from(List.of(), page, size, 0L);
        }
    }


    public OrderListResponse findOrdersWithQuery(OrderListQuery query) {
        log.info("통합 주문 목록 조회 - 팝업: {}, 타입: {}, 상태: {}, 사용자: {}, 페이지: {}",
                query.getPopupId(), query.getOrderType(), query.getStatus(),
                query.getUserId(), query.getValidatedPage());

        int page = query.getValidatedPage();
        int size = query.getValidatedSize();

        try {
            // ItemType 검증
            ItemType orderType = null;
            if (query.getOrderType() != null) {
                try {
                    orderType = ItemType.valueOf(query.getOrderType());
                } catch (IllegalArgumentException e) {
                    log.warn("잘못된 주문 타입: {}", query.getOrderType());
                    return OrderListResponse.from(List.of(), page, size, 0L);
                }
            }

         
            Pageable pageable = PageRequest.of(page - 1, size);

            
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

  
    @Cacheable(value = "my-orders",
               key = "#customerId + ':' + (#orderType ?: 'ALL') + ':' + (#status ?: 'ALL') + ':' + (#offset ?: 0) + ':' + (#limit ?: 20)",
               condition = "#from == null and #to == null") // 기간 필터가 없을 때만 캐시
    public com.popcorn.order.dto.response.MyOrderTimelineResponse getMyOrderTimeline(
            Long customerId,
            String orderType,
            String status,
            LocalDateTime from,
            LocalDateTime to,
            Integer limit,
            Long offset) {

        log.info("🕐 고객 주문 타임라인 조회 - 고객: {}, 타입: {}, 상태: {}", customerId, orderType, status);

        try {
            // 1. Pageable 생성 (offset/limit을 페이지로 변환)
            int page = offset != null ? (int) (offset / limit) : 0;
            Pageable pageable = PageRequest.of(page, limit != null ? limit : 20);

            // 2. 사용자별 주문 조회 (기존 메소드 활용)
            Page<Order> orderPage = orderRepository.findOrdersWithAllConditions(
                null, // popupId
                orderType != null ? ItemType.valueOf(orderType) : null,
                status,
                customerId,
                null, // storeId
                from,
                to,
                pageable
            );

            // 3. 응답 DTO로 변환
            List<com.popcorn.order.dto.response.MyOrderTimelineResponse.ItemDto> items = orderPage.getContent().stream()
                .map(this::convertToMyOrderTimelineItem)
                .toList();

            return com.popcorn.order.dto.response.MyOrderTimelineResponse.builder()
                .items(items)
                .page(page + 1) // 사용자에게는 1부터 시작하는 페이지 번호 반환
                .size(limit != null ? limit : 20)
                .total(orderPage.getTotalElements())
                .build();

        } catch (Exception e) {
            log.error("내 주문 타임라인 조회 실패 - 고객: {}, 에러: {}", customerId, e.getMessage(), e);
            return com.popcorn.order.dto.response.MyOrderTimelineResponse.builder()
                .items(List.of())
                .page(1)
                .size(limit != null ? limit : 20)
                .total(0L)
                .build();
        }
    }

   
    public com.popcorn.order.dto.response.StoreOrderReservationListResponse getStoreOrderReservations(
            UUID storeId,
            UUID popupId,
            UUID scheduleId,
            String orderType,
            String status,
            LocalDateTime from,
            LocalDateTime to,
            Integer limit,
            Long offset) {

        log.info("🏪 매장 주문 현황 조회 - 매장: {}, 팝업: {}, 타입: {}", storeId, popupId, orderType);

        try {
            // 1. Pageable 생성
            int page = offset != null ? (int) (offset / limit) : 0;
            Pageable pageable = PageRequest.of(page, limit != null ? limit : 20);

            // 2. 매장별 주문 조회
            Page<Order> orderPage;
            if (popupId != null) {
                // 특정 팝업의 주문만 조회
                orderPage = orderRepository.findOrdersByPopupIdWithConditions(
                    popupId,
                    status,  // String 타입으로 전달
                    orderType,  // String 타입으로 전달
                    pageable
                );
            } else {
               
                log.warn("매장 전체 주문 조회는 현재 구현되지 않음 - storeId: {}", storeId);
                orderPage = Page.empty(pageable);
            }

            // 3. 응답 DTO로 변환
            List<com.popcorn.order.dto.response.StoreOrderReservationListResponse.ItemDto> items = orderPage.getContent().stream()
                .map(this::convertToStoreOrderItem)
                .toList();

            return com.popcorn.order.dto.response.StoreOrderReservationListResponse.builder()
                .items(items)
                .page(page + 1)
                .size(limit != null ? limit : 20)
                .total(orderPage.getTotalElements())
                .build();

        } catch (Exception e) {
            log.error("매장 주문 현황 조회 실패 - 매장: {}, 에러: {}", storeId, e.getMessage(), e);
            return com.popcorn.order.dto.response.StoreOrderReservationListResponse.builder()
                .items(List.of())
                .page(1)
                .size(limit != null ? limit : 20)
                .total(0L)
                .build();
        }
    }

   
    private com.popcorn.order.dto.response.MyOrderTimelineResponse.ItemDto convertToMyOrderTimelineItem(Order order) {
        // 1. Store 서비스에서 팝업 정보 조회 (매장 정보 포함)
        PopupInfoResponse popupInfo = orderPopupLookupService.getPopupInfo(order.getPopupId())
                .orElseGet(() -> PopupInfoResponse.builder()
                        .popupId(order.getPopupId())
                        .title("팝업 정보를 불러올 수 없습니다")
                        .description("")
                        .storeId(null)
                        .storeInfo(PopupInfoResponse.StoreInfo.builder()
                                .name("매장 정보 없음")
                                .address1("")
                                .address2("")
                                .phoneNumber("")
                                .build())
                        .status("UNKNOWN")
                        .build());

        return com.popcorn.order.dto.response.MyOrderTimelineResponse.ItemDto.builder()
            .type(order.getOrderType().name())
            .id(order.getId())
            .orderNo(order.getOrderNo())
            .status(order.getStatus().name())
            .totalAmount(order.getTotalAmount())
            .cancelableUntil(order.getCancelableUntil())
            .createdAt(order.getCreatedAt())
            .popupId(order.getPopupId())
            .storeId(popupInfo.getStoreId())  // Store 서비스에서 조회한 실제 매장 ID
            .title(popupInfo.getSafeTitle()) // Store 서비스에서 조회한 실제 팝업 제목
            .sessionStartAt(null) // TODO: 방문 예정 시각은 현재 Order 엔티티에 없음. 별도 테이블에서 조회 필요
            .location(popupInfo.getLocationDto()) // Store 서비스에서 조회한 실제 매장 정보
            .build();
    }


    private com.popcorn.order.dto.response.StoreOrderReservationListResponse.ItemDto convertToStoreOrderItem(Order order) {
        return com.popcorn.order.dto.response.StoreOrderReservationListResponse.ItemDto.builder()
            .id(order.getId())
            .reservationNo(order.getOrderNo()) // 주문 번호를 예약 번호로 사용
            .status(order.getStatus().name())
            .totalAmount(order.getTotalAmount())
            .cancelableUntil(order.getCancelableUntil())
            .createdAt(order.getCreatedAt())
            .build();
    }


    public List<OrderListResponse.OrderItemDto> findOrdersByPopup(UUID popupId, String status, int page, int size) {
        log.info("팝업별 주문 목록 조회 - popupId: {}, status: {}, page: {}, size: {}",
                popupId, status, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Order> orderPage;

            if (status != null && !status.trim().isEmpty()) {
                // 상태 필터가 있는 경우
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                orderPage = orderRepository.findByPopupIdAndStatusOrderByCreatedAtDesc(
                        popupId, orderStatus, pageable);
            } else {
                // 전체 주문 조회
                orderPage = orderRepository.findByPopupIdOrderByCreatedAtDesc(popupId, pageable);
            }

            return orderPage.getContent().stream()
                    .map(OrderListResponse.OrderItemDto::fromEntity)
                    .toList();

        } catch (IllegalArgumentException e) {
            log.error("잘못된 주문 상태: {}", status, e);
            throw new IllegalArgumentException("유효하지 않은 주문 상태입니다: " + status);
        } catch (Exception e) {
            log.error("팝업별 주문 목록 조회 실패 - popupId: {}", popupId, e);
            throw new RuntimeException("주문 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
}
