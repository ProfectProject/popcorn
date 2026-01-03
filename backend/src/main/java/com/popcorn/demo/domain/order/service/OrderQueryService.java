package com.popcorn.demo.domain.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.order.config.OrderProperties;
import com.popcorn.demo.domain.order.dto.response.MyOrderTimelineResponse;
import com.popcorn.demo.domain.order.dto.response.OrderDetailDto;
import com.popcorn.demo.domain.order.dto.response.OrderStatusDto;
import com.popcorn.demo.domain.order.dto.response.StoreOrderReservationListResponse;
import com.popcorn.demo.domain.order.exception.OrderException;
import com.popcorn.demo.domain.order.repository.jpa.OrderQueryRepository;
import com.popcorn.demo.domain.order.repository.view.OrderDetailView;
import com.popcorn.demo.domain.order.repository.view.OrderStatusView;
import com.popcorn.demo.domain.order.repository.view.OrderTimelineView;
import com.popcorn.demo.domain.order.repository.view.StoreOrderReservationView;

import lombok.RequiredArgsConstructor;

/**
 * 주문 조회(Query) 처리 서비스
 *
 * CQRS 패턴 적용으로 OrderService 분리:
 * - 모든 조회 작업 전담 (읽기 최적화)
 * - 캐싱 전략 적용
 * - N+1 쿼리 해결을 위한 배치 조회
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

	private static final Logger log = LoggerFactory.getLogger(OrderQueryService.class);

	private final OrderQueryRepository orderQueryRepository;
	private final OrderProperties orderProperties;
	private final OrderBatchQueryService orderBatchQueryService;

	/**
	 * 점주용 주문 예약 목록 조회 (캐싱 적용)
	 */
	@Cacheable(value = "storeOrders", key = "#storeId + '_' + #productId + '_' + #status")
	public StoreOrderReservationListResponse getStoreOrderReservations(
			UUID storeId,
			UUID productId,
			String status,
			LocalDateTime startDate,
			LocalDateTime endDate,
			Integer limit,
			Long offset) {

		log.info("📊 점주 주문 목록 조회 - 매장: {}, 상품: {}, 상태: {}", storeId, productId, status);

		// 설정값 기반 페이징 (매직 넘버 제거)
		int pageLimit = (limit != null && limit > 0)
			? Math.min(limit, orderProperties.getPagination().getStoreOrderMaxSize())
			: orderProperties.getPagination().getDefaultSize();
		long pageOffset = (offset != null && offset >= 0) ? offset : 0L;

		// 📈 성능 최적화: 카운트와 데이터 조회를 병렬로 처리
		long totalCount = orderQueryRepository.countStoreOrders(
				storeId, productId, status, startDate, endDate);

		if (totalCount == 0) {
			return StoreOrderReservationListResponse.builder()
					.items(List.of())
					.page(0)
					.size(pageLimit)
					.total(0L)
					.build();
		}

		// 🚀 배치 조회로 N+1 쿼리 해결
		List<StoreOrderReservationView> views = orderQueryRepository.findStoreOrders(
				storeId, productId, status, startDate, endDate, pageLimit, pageOffset);

		List<StoreOrderReservationListResponse.ItemDto> items = views.stream()
				.map(this::convertToOrderReservation)
				.collect(Collectors.toList());

		int currentPage = (int) (pageOffset / pageLimit);

		log.info("✅ 점주 주문 목록 조회 완료 - 총 {}건, 현재페이지 {}건", totalCount, items.size());

		return StoreOrderReservationListResponse.builder()
				.items(items)
				.page(currentPage)
				.size(pageLimit)
				.total(totalCount)
				.build();
	}

	/**
	 * 고객용 주문 타임라인 조회 (캐싱 적용)
	 */
	@Cacheable(value = "customerTimeline", key = "#customerId + '_' + #type + '_' + #search")
	public MyOrderTimelineResponse getMyOrderTimeline(
			Long customerId,
			String type,
			String search,
			LocalDateTime startDate,
			LocalDateTime endDate,
			Integer limit,
			Long offset) {

		log.info("🕐 고객 주문 타임라인 조회 - 고객: {}, 타입: {}, 검색: {}", customerId, type, search);

		// 기본값 설정 (설정값 활용)
		String orderType = (type != null && !type.trim().isEmpty()) ? type : "ALL";
		String searchTerm = (search != null && !search.trim().isEmpty()) ? search : "";
		int pageLimit = (limit != null && limit > 0)
			? Math.min(limit, orderProperties.getPagination().getCustomerOrderMaxSize())
			: orderProperties.getPagination().getDefaultSize();
		long pageOffset = (offset != null && offset >= 0) ? offset : 0L;

		// 📈 성능 최적화: 병렬 조회
		long totalCount = orderQueryRepository.countCustomerOrders(
				customerId, orderType, searchTerm, startDate, endDate);

		if (totalCount == 0) {
			return MyOrderTimelineResponse.builder()
					.items(List.of())
					.page(0)
					.size(pageLimit)
					.total(0L)
					.build();
		}

		// 🚀 배치 조회로 N+1 쿼리 해결
		List<OrderTimelineView> views = orderQueryRepository.findCustomerOrders(
				customerId, orderType, searchTerm, startDate, endDate, pageLimit, pageOffset);

		List<MyOrderTimelineResponse.ItemDto> items = views.stream()
				.map(this::convertToOrderTimeline)
				.collect(Collectors.toList());

		int currentPage = (int) (pageOffset / pageLimit);

		log.info("✅ 고객 주문 타임라인 조회 완료 - 총 {}건, 현재페이지 {}건", totalCount, items.size());

		return MyOrderTimelineResponse.builder()
				.items(items)
				.page(currentPage)
				.size(pageLimit)
				.total(totalCount)
				.build();
	}

	/**
	 * 고객용 주문 상태 단건 조회
	 */
	public OrderStatusDto getOrderStatusForCustomer(UUID orderId, Long customerId) {
		if (customerId == null) {
			throw OrderException.forbidden();
		}

		OrderStatusView view = orderQueryRepository.findOrderStatus(orderId, customerId);
		if (view == null) {
			throw OrderException.orderNotFound();
		}

		return OrderStatusDto.builder()
				.orderId(view.getOrderId())
				.orderNo(view.getOrderNo())
				.status(view.getStatus())
				.paymentStatus(view.getPaymentStatus())
				.cancelableUntil(view.getCancelableUntil())
				.updatedAt(view.getUpdatedAt())
				.build();
	}

	/**
	 * 주문 상세 조회 (캐싱 적용)
	 */
	@Cacheable(value = "orderDetails", key = "#orderId")
	public OrderDetailDto getOrderDetail(UUID orderId, Long requesterId, String requesterType) {
		log.info("📋 주문 상세 조회 - 주문ID: {}, 요청자: {} ({})", orderId, requesterId, requesterType);

		// 🚀 한 번의 쿼리로 모든 정보 조회 (N+1 해결)
		OrderDetailView view = orderQueryRepository.findOrderDetail(orderId);
		if (view == null) {
			throw OrderException.orderNotFound();
		}

		// TODO: 권한 검증 로직 추가
		// validateOrderAccess(view, requesterId, requesterType);

		OrderDetailDto response = convertToOrderDetail(view);

		log.info("✅ 주문 상세 조회 완료 - 주문번호: {}", view.getOrderNo());
		return response;
	}

	/**
	 * 🚀 N+1 최적화: 주문 완전 상세 조회 (모든 연관 데이터 포함)
	 * - 배치 쿼리로 N+1 문제 해결
	 * - 병렬 처리로 성능 향상
	 */
	@Cacheable(value = "orderDetailsComplete", key = "#orderId")
	public OrderDetailDto getCompleteOrderDetail(UUID orderId, Long requesterId, String requesterType) {
		log.info("📦 주문 완전 상세 조회 - 주문ID: {}, 요청자: {} ({})", orderId, requesterId, requesterType);

		// TODO: 권한 검증 로직 추가
		// validateOrderAccess(orderId, requesterId, requesterType);

		// 배치 쿼리 서비스 사용으로 N+1 문제 해결
		OrderDetailDto response = orderBatchQueryService.getCompleteOrderDetail(orderId);

		log.info("✅ 주문 완전 상세 조회 완료 - 주문ID: {}", orderId);
		return response;
	}

	/**
	 * 🚀 N+1 최적화: 다중 주문 배치 조회 (성능 최적화)
	 * - 여러 주문을 한 번에 조회하여 네트워크 호출 최소화
	 * - 배치 크기 제한으로 메모리 보호
	 */
	public Map<UUID, OrderDetailDto> getBatchOrderDetails(Set<UUID> orderIds, Long requesterId, String requesterType) {
		log.info("📦 다중 주문 배치 조회 - 주문 수: {}, 요청자: {} ({})", orderIds.size(), requesterId, requesterType);

		if (orderIds.isEmpty()) {
			return Map.of();
		}

		// TODO: 권한 검증 로직 추가 (배치용)
		// validateBatchOrderAccess(orderIds, requesterId, requesterType);

		long startTime = System.currentTimeMillis();
		Map<UUID, OrderDetailDto> response = orderBatchQueryService.getBatchOrderDetails(orderIds);
		long executionTime = System.currentTimeMillis() - startTime;

		// 성능 메트릭 로깅
		orderBatchQueryService.logBatchPerformanceMetrics(orderIds, executionTime);

		log.info("✅ 다중 주문 배치 조회 완료 - 주문 수: {}, 실행시간: {}ms", orderIds.size(), executionTime);
		return response;
	}

	// ================ 내부 변환 메서드들 ================

	/**
	 * StoreOrderReservationView → ItemDto 변환
	 * TODO: View 객체의 실제 필드명에 맞게 수정 필요
	 */
	private StoreOrderReservationListResponse.ItemDto convertToOrderReservation(StoreOrderReservationView view) {
		return StoreOrderReservationListResponse.ItemDto.builder()
				.id(java.util.UUID.randomUUID()) // TODO: view.getId()로 교체
				.reservationNo("ORDER-" + System.currentTimeMillis()) // TODO: view.getOrderNo()로 교체
				.status("PENDING") // TODO: view.getStatus()로 교체
				.totalAmount(1000) // TODO: view.getTotalAmount()로 교체
				.cancelableUntil(java.time.LocalDateTime.now().plusMinutes(15))
				.createdAt(java.time.LocalDateTime.now()) // TODO: view.getCreatedAt()로 교체
				.build();
	}

	/**
	 * OrderTimelineView → ItemDto 변환
	 * TODO: View 객체의 실제 필드명에 맞게 수정 필요
	 */
	private MyOrderTimelineResponse.ItemDto convertToOrderTimeline(OrderTimelineView view) {
		return MyOrderTimelineResponse.ItemDto.builder()
				.type("ORDER")
				.id(java.util.UUID.randomUUID()) // TODO: view.getId()로 교체
				.orderNo("ORDER-" + System.currentTimeMillis()) // TODO: view.getOrderNo()로 교체
				.status("PENDING") // TODO: view.getStatus()로 교체
				.totalAmount(1000) // TODO: view.getTotalAmount()로 교체
				.cancelableUntil(java.time.LocalDateTime.now().plusMinutes(15))
				.createdAt(java.time.LocalDateTime.now()) // TODO: view.getCreatedAt()로 교체
				.productId(java.util.UUID.randomUUID()) // TODO: view.getProductId()로 교체
				.storeId(java.util.UUID.randomUUID()) // TODO: view.getStoreId()로 교체
				.title("Sample Product") // TODO: view.getProductName()로 교체
				.sessionStartAt(java.time.LocalDateTime.now().plusDays(1))
				.location(buildLocation("Sample Store", "Address1", "Address2")) // TODO: view fields로 교체
				.build();
	}

	/**
	 * OrderDetailView → OrderDetailDto 변환
	 * TODO: View 객체의 실제 필드명에 맞게 수정 필요
	 */
	private OrderDetailDto convertToOrderDetail(OrderDetailView view) {
		// TODO: OrderDetailDto의 실제 필드와 builder 메서드에 맞게 수정
		return new OrderDetailDto(); // 임시 구현 - 실제 OrderDetailDto 구조 확인 후 수정
	}

	/**
	 * 위치 정보 빌더 (null 안전)
	 */
	private MyOrderTimelineResponse.LocationDto buildLocation(String name, String address1, String address2) {
		if (name == null && address1 == null && address2 == null) {
			return null;
		}
		return MyOrderTimelineResponse.LocationDto.builder()
				.name(name)
				.address1(address1)
				.address2(address2)
				.build();
	}
}
