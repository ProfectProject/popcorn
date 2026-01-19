package com.popcorn.demo.domain.payment.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.payment.entity.PaymentCancelFailureQueue;
import com.popcorn.demo.domain.payment.event.PaymentCancelFailedEvent;
import com.popcorn.demo.domain.payment.repository.JpaPaymentCancelFailureQueueRepository;
import com.popcorn.demo.domain.payment.toss.TossPaymentsCancelRequest;
import com.popcorn.demo.domain.payment.toss.TossPaymentsCancelResponse;
import com.popcorn.demo.domain.payment.toss.TossPaymentsClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentCancelFailureService {

	private static final Logger log = LoggerFactory.getLogger(PaymentCancelFailureService.class);

	private final JpaPaymentCancelFailureQueueRepository failureQueueRepository;
	private final TossPaymentsClient tossPaymentsClient;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 실패 큐에 결제 취소 실패 사항 저장
	 */
	@Transactional
	public void addToFailureQueue(PaymentCancelFailedEvent event) {
		log.info("💾 실패 큐에 결제 취소 실패 저장 - 주문ID: {}, 시도횟수: {}",
				event.getOrderId(), event.getAttemptCount());

		// 이미 처리 중인 큐가 있는지 확인
		boolean alreadyQueued = failureQueueRepository.existsByOrderIdAndStatusIn(
				event.getOrderId(),
				List.of(
					PaymentCancelFailureQueue.QueueStatus.PENDING,
					PaymentCancelFailureQueue.QueueStatus.RETRYING
				)
		);

		if (alreadyQueued) {
			log.info("⏭️ 이미 실패 큐에 등록된 주문 - 주문ID: {}", event.getOrderId());
			return;
		}

		PaymentCancelFailureQueue queue = PaymentCancelFailureQueue.builder()
				.orderId(event.getOrderId())
				.paymentId(event.getPaymentId())
				.paymentKey(event.getPaymentKey())
				.cancelReason(event.getCancelReason())
				.failureReason(event.getFailureReason())
				.amount(event.getAmount())
				.attemptCount(event.getAttemptCount())
				.nextRetryAt(LocalDateTime.now().plusMinutes(1)) // 1분 후 첫 재시도
				.build();

		failureQueueRepository.save(queue);
		log.info("✅ 실패 큐 저장 완료 - ID: {}, 다음 재시도: {}",
				queue.getId(), queue.getNextRetryAt());
	}

	/**
	 * 재시도 가능한 실패 큐들을 처리
	 */
	@Async("paymentRetryExecutor")
	@Transactional
	public void processRetriableQueues() {
		List<PaymentCancelFailureQueue> retriableQueues = failureQueueRepository
				.findRetriableQueues(LocalDateTime.now());

		if (retriableQueues.isEmpty()) {
			return;
		}

		log.info("🔄 재시도 대상 결제 취소 실패 큐: {}건", retriableQueues.size());

		for (PaymentCancelFailureQueue queue : retriableQueues) {
			try {
				retryPaymentCancel(queue);
			} catch (Exception ex) {
				log.error("❌ 재시도 처리 중 오류 - QueueID: {}", queue.getId(), ex);
				handleRetryFailure(queue, ex);
			}
		}
	}

	/**
	 * 재시도 가능한 실패 큐 존재 여부 확인 (스케줄러용)
	 */
	@Transactional(readOnly = true)
	public boolean hasRetriableQueues() {
		return failureQueueRepository.countRetriableQueues(LocalDateTime.now()) > 0;
	}

	private void retryPaymentCancel(PaymentCancelFailureQueue queue) {
		log.info("🔁 결제 취소 재시도 시작 - QueueID: {}, 시도횟수: {}/{}",
				queue.getId(), queue.getAttemptCount() + 1, queue.getMaxAttempts());

		queue.setRetrying();
		failureQueueRepository.save(queue);

		try {
			// 토스 결제 취소 재시도
			TossPaymentsCancelResponse response = tossPaymentsClient.cancel(
					queue.getPaymentKey(),
					TossPaymentsCancelRequest.builder()
							.cancelReason(queue.getCancelReason())
							.build()
			);

			// 성공 처리
			queue.markSuccess();
			failureQueueRepository.save(queue);

			log.info("✅ 결제 취소 재시도 성공 - QueueID: {}, 취소금액: {}원",
					queue.getId(), response.getTotalAmount());

		} catch (Exception ex) {
			handleRetryFailure(queue, ex);
		}
	}

	private void handleRetryFailure(PaymentCancelFailureQueue queue, Exception ex) {
		log.warn("⚠️ 결제 취소 재시도 실패 - QueueID: {}, 오류: {}",
				queue.getId(), ex.getMessage());

		queue.setFailureReason(ex.getMessage());
		queue.incrementAttempt();
		failureQueueRepository.save(queue);

		if (queue.getStatus() == PaymentCancelFailureQueue.QueueStatus.FAILED) {
			log.error("❌ 결제 취소 최종 실패 - QueueID: {}, 주문ID: {}, 총 시도횟수: {}",
					queue.getId(), queue.getOrderId(), queue.getAttemptCount());

			// 최종 실패 알림 이벤트 발행 (관리자 알림용)
			publishFinalFailureEvent(queue);
		} else {
			log.info("⏰ 다음 재시도 예정 - QueueID: {}, 다음 시도: {}",
					queue.getId(), queue.getNextRetryAt());
		}
	}

	private void publishFinalFailureEvent(PaymentCancelFailureQueue queue) {
		// 관리자 알림, 슬랙 메시지 등을 위한 최종 실패 이벤트
		// 이 이벤트는 별도 핸들러에서 처리하여 알림을 보냄
		log.warn("🚨 결제 취소 최종 실패 알림 필요 - 주문ID: {}, 금액: {}원",
				queue.getOrderId(), queue.getAmount());
	}

	/**
	 * 특정 주문의 실패 큐 상태 조회
	 */
	public List<PaymentCancelFailureQueue> getFailureQueuesForOrder(UUID orderId) {
		return failureQueueRepository.findByPaymentId(orderId);
	}

	/**
	 * 오래된 실패 큐들 조회 (모니터링용)
	 */
	public List<PaymentCancelFailureQueue> getStuckQueues() {
		LocalDateTime threshold = LocalDateTime.now().minusHours(24); // 24시간 이상 된 것
		return failureQueueRepository.findStuckQueues(threshold);
	}
}
