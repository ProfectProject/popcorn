package com.popcorn.demo.domain.order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderStatusHistory;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.repository.jpa.JpaOrderRepository;
import com.popcorn.demo.domain.order.repository.jpa.JpaOrderStatusHistoryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA를 사용한 주문 도메인 시드 데이터 관리
 *
 * 장점:
 * - IntelliJ JPA 디자이너에서 엔티티 관계 시각화 가능
 * - 타입 안전성 보장 (컴파일 타임 체크)
 * - JPA Cascade, 연관관계 매핑 등 활용 가능
 * - 객체지향적인 도메인 로직 활용
 */
@Slf4j
// @Component
@RequiredArgsConstructor
@Profile("disabled")
public class OrderJpaDataSeeder implements ApplicationRunner {

	private final JpaOrderRepository orderRepository;
	private final JpaOrderStatusHistoryRepository statusHistoryRepository;

	@PersistenceContext
	private EntityManager entityManager;

	// 상수 정의 (기존 LocalSeedRunner와 호환성 유지)
	private static final UUID STORE_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID PRODUCT_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID SESSION_OPTION_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000301");
	private static final UUID MERCH_VARIANT_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000401");

	// 테스트 주문 ID 상수들
	private static final UUID ORDER_ID_1501 = UUID.fromString("00000000-0000-0000-0000-000000001501");
	private static final UUID ORDER_ID_1502 = UUID.fromString("00000000-0000-0000-0000-000000001502");
	private static final UUID ORDER_ID_1503 = UUID.fromString("00000000-0000-0000-0000-000000001503");

	@Override
	@Transactional("jdbcTransactionManager")
	public void run(ApplicationArguments args) {
		log.info("🌱 JPA 시드 데이터 생성 시작...");

		// 기존 시드 데이터가 있는지 확인 (중복 방지)
		if (orderRepository.count() > 0) {
			log.info("🌱 기존 시드 데이터 존재, JPA 시드 생성 건너뜀");
			return;
		}

		createTestOrdersWithJpa();

		log.info("🌱 JPA 시드 데이터 생성 완료");
	}

	/**
	 * JPA를 사용한 테스트 주문 데이터 생성
	 * IntelliJ JPA 디자이너에서 엔티티 관계를 확인할 수 있음
	 */
	private void createTestOrdersWithJpa() {
		// 1. 기본 테스트 주문들 (기존)
		Order reservationOrder = createReservationOrder();
		Order purchaseOrder = createPurchaseOrder();
		Order cancelledOrder = createCancelledOrder();

		// 2. 취소 가능한 주문들 (REQUESTED 상태) - Swagger 테스트용
		List<Order> cancellableOrders = createCancellableTestOrders();

		// 3. 취소 불가능한 주문들 (다양한 상태) - 비즈니스 로직 테스트용
		List<Order> nonCancellableOrders = createNonCancellableTestOrders();

		// 4. 모든 주문 수집
		List<Order> allOrders = new ArrayList<>();
		allOrders.add(reservationOrder);
		allOrders.add(purchaseOrder);
		allOrders.add(cancelledOrder);
		allOrders.addAll(cancellableOrders);
		allOrders.addAll(nonCancellableOrders);

		// 5. JPA 저장 (Cascade 활용)
		orderRepository.saveAll(allOrders);

		// 6. 주문 상태 이력 생성 (감사 추적용)
		List<OrderStatusHistory> statusHistories = createOrderStatusHistories();
		statusHistoryRepository.saveAll(statusHistories);

		// 7. EntityManager flush로 즉시 DB 반영
		entityManager.flush();

		log.info("🌱 JPA로 {} 개의 테스트 주문 생성됨 (기본: 3개, 취소가능: {}개, 취소불가: {}개, 상태이력: {}개)",
				allOrders.size(), cancellableOrders.size(), nonCancellableOrders.size(), statusHistories.size());
	}

	/**
	 * 예약형 주문 생성 (JPA 엔티티 메서드 활용)
	 */
	private Order createReservationOrder() {
		Order order = Order.builder()
				.orderNo(Order.generateOrderNo()) // 도메인 엔티티의 비즈니스 로직 활용
				.customerId(1001L)
				.storeId(STORE_ID_1)
				.popupId(PRODUCT_ID_1)
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.REQUESTED)
				.totalAmount(20000)
				.build();

		// 주문 아이템 생성 (연관관계 활용)
		OrderItem reservationItem = OrderItem.builder()
				.orderItemType(OrderItemType.RESERVATION)
				.sessionOptionId(SESSION_OPTION_ID_1)
				.qty(2)
				.unitPrice(10000)
				.lineAmount(20000)
				.build();

		// JPA 엔티티의 편의 메서드 활용
		order.addOrderItem(reservationItem);

		return order;
	}

	/**
	 * 구매형 주문 생성 (JPA 엔티티 메서드 활용)
	 */
	private Order createPurchaseOrder() {
		Order order = Order.builder()
				.orderNo(Order.generateOrderNo())
				.customerId(1002L)
				.storeId(STORE_ID_1)
				.popupId(PRODUCT_ID_1)
				.orderType(OrderType.PURCHASE)
				.status(OrderStatus.RESERVED)
				.totalAmount(35000)
				.build();

		// 굿즈 아이템 생성
		OrderItem merchItem = OrderItem.builder()
				.orderItemType(OrderItemType.GOODS)
				.goodsVariantId(MERCH_VARIANT_ID_1)
				.qty(1)
				.unitPrice(35000)
				.lineAmount(35000)
				.build();

		order.addOrderItem(merchItem);

		return order;
	}

	/**
	 * 취소된 주문 생성 (상태 변화 시뮬레이션)
	 */
	private Order createCancelledOrder() {
		Order order = Order.builder()
				.orderNo(Order.generateOrderNo())
				.customerId(1003L)
				.storeId(STORE_ID_1)
				.popupId(PRODUCT_ID_1)
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.CANCELLED) // 취소 상태
				.totalAmount(15000)
				.build();

		OrderItem cancelledItem = OrderItem.builder()
				.orderItemType(OrderItemType.RESERVATION)
				.sessionOptionId(SESSION_OPTION_ID_1)
				.qty(1)
				.unitPrice(15000)
				.lineAmount(15000)
				.build();

		order.addOrderItem(cancelledItem);

		return order;
	}

	/**
	 * 취소 가능한 테스트 주문들 생성 (REQUESTED 상태)
	 * V9__seed_cancel_test_data.sql의 1401~1405 주문 JPA 버전
	 */
	private List<Order> createCancellableTestOrders() {
		List<Order> orders = new ArrayList<>();

		// 1401: 예약형 주문 (취소 가능)
		Order order1401 = Order.builder()
				.id(UUID.fromString("00000000-0000-0000-0000-000000001401"))
				.orderNo("O20260103-140001")
				.customerId(1001L)
				.storeId(STORE_ID_1)
				.popupId(PRODUCT_ID_1)
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.REQUESTED)
				.totalAmount(10000)
				.build();
		OrderItem item1401 = OrderItem.builder()
				.orderItemType(OrderItemType.RESERVATION)
				.sessionOptionId(SESSION_OPTION_ID_1)
				.qty(2)
				.unitPrice(5000)
				.lineAmount(10000)
				.build();
		order1401.addOrderItem(item1401);
		orders.add(order1401);

		// 1402: 구매형 주문 (취소 가능)
		Order order1402 = Order.builder()
				.id(UUID.fromString("00000000-0000-0000-0000-000000001402"))
				.orderNo("O20260103-140002")
				.customerId(1001L)
				.storeId(STORE_ID_1)
				.popupId(PRODUCT_ID_1)
				.orderType(OrderType.PURCHASE)
				.status(OrderStatus.REQUESTED)
				.totalAmount(25000)
				.build();
		OrderItem item1402 = OrderItem.builder()
				.orderItemType(OrderItemType.GOODS)
				.goodsVariantId(MERCH_VARIANT_ID_1)
				.qty(1)
				.unitPrice(25000)
				.lineAmount(25000)
				.build();
		order1402.addOrderItem(item1402);
		orders.add(order1402);

		// 1403: 예약형 주문 (취소 가능)
		Order order1403 = Order.builder()
				.id(UUID.fromString("00000000-0000-0000-0000-000000001403"))
				.orderNo("O20260103-140003")
				.customerId(1001L)
				.storeId(STORE_ID_1)
				.popupId(PRODUCT_ID_1)
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.REQUESTED)
				.totalAmount(15000)
				.build();
		OrderItem item1403 = OrderItem.builder()
				.orderItemType(OrderItemType.RESERVATION)
				.sessionOptionId(SESSION_OPTION_ID_1)
				.qty(3)
				.unitPrice(5000)
				.lineAmount(15000)
				.build();
		order1403.addOrderItem(item1403);
		orders.add(order1403);

		// 1404: 구매형 주문 (취소 가능)
		Order order1404 = Order.builder()
				.id(UUID.fromString("00000000-0000-0000-0000-000000001404"))
				.orderNo("O20260103-140004")
				.customerId(1002L)
				.storeId(STORE_ID_1)
				.popupId(PRODUCT_ID_1)
				.orderType(OrderType.PURCHASE)
				.status(OrderStatus.REQUESTED)
				.totalAmount(18000)
				.build();
		OrderItem item1404 = OrderItem.builder()
				.orderItemType(OrderItemType.GOODS)
				.goodsVariantId(MERCH_VARIANT_ID_1)
				.qty(2)
				.unitPrice(9000)
				.lineAmount(18000)
				.build();
		order1404.addOrderItem(item1404);
		orders.add(order1404);

		// 1405: 예약형 주문 (취소 가능)
		Order order1405 = Order.builder()
				.id(UUID.fromString("00000000-0000-0000-0000-000000001405"))
				.orderNo("O20260103-140005")
				.customerId(1003L)
				.storeId(STORE_ID_1)
				.popupId(PRODUCT_ID_1)
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.REQUESTED)
				.totalAmount(22000)
				.build();
		OrderItem item1405 = OrderItem.builder()
				.orderItemType(OrderItemType.RESERVATION)
				.sessionOptionId(SESSION_OPTION_ID_1)
				.qty(4)
				.unitPrice(5500)
				.lineAmount(22000)
				.build();
		order1405.addOrderItem(item1405);
		orders.add(order1405);

		return orders;
	}

	/**
	 * 취소 불가능한 테스트 주문들 생성 (다양한 상태)
	 * V9__seed_cancel_test_data.sql의 1501~1503 주문 JPA 버전
	 */
	private List<Order> createNonCancellableTestOrders() {
		List<Order> orders = new ArrayList<>();

		// 1501: 이미 취소된 주문 (CANCELLED 상태)
		Order cancelledOrder = Order.builder()
				.id(ORDER_ID_1501)
				.orderNo("O20260103-150001")
				.customerId(1001L)
				.storeId(STORE_ID_1)
				.popupId(PRODUCT_ID_1)
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.CANCELLED)
				.totalAmount(15000)
				.build();
		OrderItem cancelledItem = OrderItem.builder()
				.orderItemType(OrderItemType.RESERVATION)
				.sessionOptionId(SESSION_OPTION_ID_1)
				.qty(1)
				.unitPrice(15000)
				.lineAmount(15000)
				.build();
		cancelledOrder.addOrderItem(cancelledItem);
		orders.add(cancelledOrder);

		// 1502: 완료된 주문 (COMPLETED 상태)
		Order completedOrder = Order.builder()
				.id(UUID.fromString("00000000-0000-0000-0000-000000001502"))
				.orderNo("O20260103-150002")
				.customerId(1001L)
				.storeId(STORE_ID_1)
				.popupId(PRODUCT_ID_1)
				.orderType(OrderType.PURCHASE)
				.status(OrderStatus.COMPLETED)
				.totalAmount(20000)
				.build();
		OrderItem completedItem = OrderItem.builder()
				.orderItemType(OrderItemType.GOODS)
				.goodsVariantId(MERCH_VARIANT_ID_1)
				.qty(1)
				.unitPrice(20000)
				.lineAmount(20000)
				.build();
		completedOrder.addOrderItem(completedItem);
		orders.add(completedOrder);

		// 1503: 결제 대기 주문 (PAYMENT_PENDING 상태) - 취소 시간은 남았지만 상태상 취소 불가
		Order preparingOrder = Order.builder()
				.id(UUID.fromString("00000000-0000-0000-0000-000000001503"))
				.orderNo("O20260103-150003")
				.customerId(1001L)
				.storeId(STORE_ID_1)
				.popupId(PRODUCT_ID_1)
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.PAYMENT_PENDING)
				.totalAmount(12000)
				.build();
		OrderItem preparingItem = OrderItem.builder()
				.orderItemType(OrderItemType.RESERVATION)
				.sessionOptionId(SESSION_OPTION_ID_1)
				.qty(1)
				.unitPrice(12000)
				.lineAmount(12000)
				.build();
		preparingOrder.addOrderItem(preparingItem);
		orders.add(preparingOrder);

		return orders;
	}

	/**
	 * 주문 상태 이력 생성 (감사 추적용)
	 * 상태 변경이 발생한 주문들의 이력을 JPA 엔티티로 생성
	 */
	private List<OrderStatusHistory> createOrderStatusHistories() {
		List<OrderStatusHistory> histories = new ArrayList<>();

		// 1501번 주문: REQUESTED → CANCELLED 상태 변경 이력
		// V9__seed_cancel_test_data.sql의 status history 데이터를 JPA로 재현
		OrderStatusHistory cancelHistory = OrderStatusHistory.builder()
				.id(UUID.fromString("00000000-0000-0000-0000-000000004501"))
				.orderId(UUID.fromString("00000000-0000-0000-0000-000000001501"))
				.fromStatus(OrderStatus.REQUESTED)
				.toStatus(OrderStatus.CANCELLED)
				.reason("고객 요청에 의한 취소")
				.changedBy(1001L) // 고객 ID
				.changedAt(LocalDateTime.now().minusMinutes(10))
				.build();
		histories.add(cancelHistory);

		// 1502번 주문: REQUESTED → RESERVED → COMPLETED 상태 변경 이력
		// 실제 운영에서는 여러 단계를 거쳐 완료되므로 이력을 시뮬레이션
		OrderStatusHistory confirmHistory = OrderStatusHistory.builder()
				.orderId(UUID.fromString("00000000-0000-0000-0000-000000001502"))
				.fromStatus(OrderStatus.REQUESTED)
				.toStatus(OrderStatus.RESERVED)
				.reason("운영자 승인")
				.changedBy(2001L) // 운영자 ID
				.changedAt(LocalDateTime.now().minusHours(2).minusMinutes(30))
				.build();
		histories.add(confirmHistory);

		OrderStatusHistory completeHistory = OrderStatusHistory.builder()
				.orderId(UUID.fromString("00000000-0000-0000-0000-000000001502"))
				.fromStatus(OrderStatus.RESERVED)
				.toStatus(OrderStatus.COMPLETED)
				.reason("서비스 완료")
				.changedBy(2001L) // 운영자 ID
				.changedAt(LocalDateTime.now().minusMinutes(10))
				.build();
		histories.add(completeHistory);

		// 1503번 주문: REQUESTED → RESERVED → PAYMENT_PENDING 상태 변경 이력
		OrderStatusHistory prepareConfirmHistory = OrderStatusHistory.builder()
				.orderId(UUID.fromString("00000000-0000-0000-0000-000000001503"))
				.fromStatus(OrderStatus.REQUESTED)
				.toStatus(OrderStatus.RESERVED)
				.reason("운영자 승인")
				.changedBy(2001L) // 운영자 ID
				.changedAt(LocalDateTime.now().minusHours(1).minusMinutes(30))
				.build();
		histories.add(prepareConfirmHistory);

		OrderStatusHistory preparingHistory = OrderStatusHistory.builder()
				.orderId(UUID.fromString("00000000-0000-0000-0000-000000001503"))
				.fromStatus(OrderStatus.RESERVED)
				.toStatus(OrderStatus.PAYMENT_PENDING)
				.reason("주문 준비 시작")
				.changedBy(2001L) // 운영자 ID
				.changedAt(LocalDateTime.now().minusMinutes(30))
				.build();
		histories.add(preparingHistory);

		return histories;
	}
}
