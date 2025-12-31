package com.popcorn.demo.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.repository.OrderSummaryView;

/**

	* JPA 기반 주문 레포지토리 구현체

	* - Spring Data JPA 활용

	* - 도메인 레이어의 OrderRepository 인터페이스 구현

	* - 인프라스트럭처 레이어 위치

	*/

@Repository

public interface JpaOrderRepository extends JpaRepository<Order, Long> {



	// ========================= 기본 조회 메서드 =========================



	/**

		* 주문 번호로 조회

		* JPA 메서드 네이밍 컨벤션 활용

		*/

	Optional<Order> findByOrderNo(String orderNo);



	/**

		* 주문 요약 조회 (필요 컬럼만)

		*/

	@Query("SELECT o.id AS id, o.orderNo AS orderNo, o.status AS status, "
				+ "o.totalAmount AS totalAmount, o.createdAt AS createdAt "
				+ "FROM Order o WHERE o.id = :orderId")

	Optional<OrderSummaryView> findSummaryById(@Param("orderId") Long orderId);



	// ========================= 비즈니스 조회 메서드 =========================



	/**

		* 고객의 주문 목록 조회 (최신순)

		*/

	List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);



	/**

		* 스토어의 주문 목록 조회

		*/

	List<Order> findByStoreId(Long storeId);



	/**

		* 스토어의 특정 상태 주문 목록 조회

		*/

	List<Order> findByStoreIdAndStatus(Long storeId, OrderStatus status);



	/**

		* 상품의 주문 목록 조회

		*/

	List<Order> findByProductId(Long productId);



	// ========================= 상태별 조회 메서드 =========================



	/**

		* 특정 상태의 주문 목록 조회

		*/

	List<Order> findByStatus(OrderStatus status);



	/**

		* 취소 가능한 주문 목록 조회

		* JPQL 사용 - cancelableUntil이 현재 시간보다 큰 주문들

		*/

	@Query("SELECT o FROM Order o WHERE o.cancelableUntil > :currentTime")

	List<Order> findCancelableOrders(@Param("currentTime") LocalDateTime currentTime);



	/**

		* 취소 기간이 만료된 주문 목록 조회

		*/

	@Query("SELECT o FROM Order o WHERE o.cancelableUntil <= :currentTime")

	List<Order> findExpiredCancelableOrders(@Param("currentTime") LocalDateTime currentTime);



	// ========================= 통계 및 집계 메서드 =========================



	/**

		* 고객의 총 주문 수 조회

		*/

	long countByCustomerId(Long customerId);



	/**

		* 스토어의 총 주문 수 조회

		*/

	long countByStoreId(Long storeId);



	/**

		* 특정 기간 내 주문 수 조회

		*/

	long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);



	/**

		* 고객의 특정 기간 총 주문 금액 조회

		* JPQL 집계 함수 사용

		*/

	@Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o "
				+ "WHERE o.customerId = :customerId "
				+ "AND o.createdAt BETWEEN :startDate AND :endDate")

	long sumTotalAmountByCustomerIdAndCreatedAtBetween(

			@Param("customerId") Long customerId,

			@Param("startDate") LocalDateTime startDate,

			@Param("endDate") LocalDateTime endDate

	);



	// ========================= 중복 방지 메서드 =========================



	/**

		* 멱등성 키로 주문 조회

		* 향후 idempotencyKey 필드 추가 시 활성화

		*/

	// Optional<Order> findByIdempotencyKey(String idempotencyKey);

	Optional<Order> findByIdempotencyKey(String idempotencyKey);



	/**

		* 멱등성 키 존재 여부 확인

		* 향후 idempotencyKey 필드 추가 시 활성화

		*/

	// boolean existsByIdempotencyKey(String idempotencyKey);

	boolean existsByIdempotencyKey(String idempotencyKey);



	// ========================= 페이징 조회 메서드 =========================



	/**

		* 고객의 주문 목록 조회 (페이징)

		* PageRequest.of(page, size) 와 함께 사용

		*/

	@Query(value = "SELECT o FROM Order o WHERE o.customerId = :customerId ORDER BY o.createdAt DESC",

			countQuery = "SELECT COUNT(o) FROM Order o WHERE o.customerId = :customerId")

	List<Order> findByCustomerIdWithPaging(

			@Param("customerId") Long customerId,

			org.springframework.data.domain.Pageable pageable

	);

}

