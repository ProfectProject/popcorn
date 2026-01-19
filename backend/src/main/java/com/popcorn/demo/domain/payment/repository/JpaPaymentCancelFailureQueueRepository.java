package com.popcorn.demo.domain.payment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.popcorn.demo.domain.payment.entity.PaymentCancelFailureQueue;

@Repository
public interface JpaPaymentCancelFailureQueueRepository extends JpaRepository<PaymentCancelFailureQueue, Long> {

	/**
	 * 재시도 가능한 실패 큐 조회
	 */
	@Query("""
		SELECT q FROM PaymentCancelFailureQueue q
		WHERE q.status = 'PENDING'
		  AND q.attemptCount < q.maxAttempts
		  AND (q.nextRetryAt IS NULL OR q.nextRetryAt <= :now)
		ORDER BY q.createdAt ASC
		""")
	List<PaymentCancelFailureQueue> findRetriableQueues(@Param("now") LocalDateTime now);

	/**
	 * 재시도 가능한 실패 큐 존재 여부 확인
	 */
	@Query("""
		SELECT COUNT(q) FROM PaymentCancelFailureQueue q
		WHERE q.status = 'PENDING'
		  AND q.attemptCount < q.maxAttempts
		  AND (q.nextRetryAt IS NULL OR q.nextRetryAt <= :now)
		""")
	long countRetriableQueues(@Param("now") LocalDateTime now);

	/**
	 * 특정 주문의 대기 중인 큐가 있는지 확인
	 */
	boolean existsByOrderIdAndStatusIn(UUID orderId, List<PaymentCancelFailureQueue.QueueStatus> statuses);

	/**
	 * 특정 결제의 실패 큐 조회
	 */
	List<PaymentCancelFailureQueue> findByPaymentId(UUID paymentId);

	/**
	 * 완료되지 않은 큐들 조회 (모니터링용)
	 */
	@Query("""
		SELECT q FROM PaymentCancelFailureQueue q
		WHERE q.status IN ('PENDING', 'RETRYING')
		  AND q.createdAt < :threshold
		ORDER BY q.createdAt ASC
		""")
	List<PaymentCancelFailureQueue> findStuckQueues(@Param("threshold") LocalDateTime threshold);
}
