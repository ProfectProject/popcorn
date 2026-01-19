package com.popcorn.demo.domain.payment.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.popcorn.demo.domain.payment.service.PaymentCancelFailureService;

import lombok.RequiredArgsConstructor;

/**
 * 결제 취소 실패 큐 재시도 스케줄러
 * 주기적으로 실패한 결제 취소를 재시도
 */
@Component
@RequiredArgsConstructor
public class PaymentCancelRetryScheduler {

	private static final Logger log = LoggerFactory.getLogger(PaymentCancelRetryScheduler.class);

	private final PaymentCancelFailureService paymentCancelFailureService;

	/**
	 * 매분마다 재시도 가능한 실패 큐 처리
	 */
	@Scheduled(cron = "0 * * * * *") // 매분 0초에 실행
	public void processRetryQueue() {
		try {
			if (!paymentCancelFailureService.hasRetriableQueues()) {
				return;
			}
			log.debug("🔄 결제 취소 재시도 큐 처리 시작");
			paymentCancelFailureService.processRetriableQueues();
		} catch (Exception ex) {
			log.error("❌ 결제 취소 재시도 큐 처리 중 오류", ex);
		}
	}

	/**
	 * 매시간마다 오래된 실패 큐 모니터링
	 */
	@Scheduled(cron = "0 0 * * * *") // 매시간 0분 0초에 실행
	public void monitorStuckQueues() {
		try {
			var stuckQueues = paymentCancelFailureService.getStuckQueues();
			if (!stuckQueues.isEmpty()) {
				log.warn("⚠️ 오래된 실패 큐 감지: {}건 - 수동 확인 필요", stuckQueues.size());
				// 여기서 관리자 알림을 보낼 수 있습니다
			}
		} catch (Exception ex) {
			log.error("❌ 실패 큐 모니터링 중 오류", ex);
		}
	}
}
