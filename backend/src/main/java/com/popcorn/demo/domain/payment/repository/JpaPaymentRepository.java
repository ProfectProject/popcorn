package com.popcorn.demo.domain.payment.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.popcorn.demo.domain.payment.entity.Payment;

public interface JpaPaymentRepository extends JpaRepository<Payment, UUID> {

	boolean existsByOrderId(UUID orderId);

	Optional<Payment> findByOrderId(UUID orderId);

	boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);

	List<Payment> findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID orderId);

	/**
	 * paymentKey로 기존 결제 검색 (rawPayload JSON에서 검색)
	 */
	@Query("""
		SELECT p FROM Payment p
		WHERE p.rawPayload LIKE CONCAT('%"paymentKey":"', :paymentKey, '"%')
		  AND p.deletedAt IS NULL
		""")
	List<Payment> findByPaymentKeyInRawPayload(String paymentKey);
}
