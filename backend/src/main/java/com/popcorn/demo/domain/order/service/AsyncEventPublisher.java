package com.popcorn.demo.domain.order.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.popcorn.demo.domain.order.config.OrderProperties;

import lombok.RequiredArgsConstructor;

/**
 * 비동기 이벤트 발행 서비스
 *
 * 성능 최적화를 위한 비동기 이벤트 처리:
 * - 설정값 기반 타임아웃 관리
 * - 재시도 로직 적용
 * - 전용 스레드풀 사용
 * - 실패 로그 및 메트릭 수집
 */
@Service
@RequiredArgsConstructor
public class AsyncEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(AsyncEventPublisher.class);

	private final ApplicationEventPublisher eventPublisher;
	private final OrderProperties orderProperties;
	private final Executor orderAsyncExecutor; // 주문 전용 비동기 Executor

	/**
	 * 비동기 이벤트 발행 (타임아웃 및 재시도 적용)
	 */
	public CompletableFuture<Void> publishEventAsync(Object event) {
		return CompletableFuture
				.runAsync(() -> publishWithRetry(event), orderAsyncExecutor)
				.orTimeout(orderProperties.getAsync().getTaskTimeout().toMillis(), TimeUnit.MILLISECONDS)
				.exceptionally(this::handleAsyncFailure);
	}

	/**
	 * 재시도가 적용된 이벤트 발행 (수동 재시도)
	 */
	private void publishWithRetry(Object event) {
		int maxAttempts = orderProperties.getAsync().getEventRetryCount();
		int attempt = 1;
		long delay = 100;

		while (attempt <= maxAttempts) {
			try {
				eventPublisher.publishEvent(event);
				log.debug("✅ 이벤트 발행 성공 - 타입: {} (시도 {}회)",
					event.getClass().getSimpleName(), attempt);
				return; // 성공시 메서드 종료
			} catch (Exception e) {
				if (attempt == maxAttempts) {
					log.error("❌ 이벤트 발행 최종 실패 - 타입: {}, 총 시도: {}회",
						event.getClass().getSimpleName(), attempt, e);
					throw e; // 최종 실패시 예외 전파
				}

				log.warn("⚠️ 이벤트 발행 실패 (재시도 중) - 타입: {}, 시도: {}회, 오류: {}",
						event.getClass().getSimpleName(), attempt, e.getMessage());

				// 백오프 지연
				try {
					Thread.sleep(delay);
					delay *= 2; // 지연시간 2배 증가
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("재시도 지연 중 인터럽트", ie);
				}

				attempt++;
			}
		}
	}

	/**
	 * 비동기 처리 실패 핸들러
	 */
	private Void handleAsyncFailure(Throwable throwable) {
		if (throwable instanceof TimeoutException) {
			log.error("❌ 이벤트 발행 타임아웃 - 제한시간: {}ms",
					orderProperties.getAsync().getTaskTimeout().toMillis(), throwable);
		} else if (throwable instanceof CompletionException) {
			log.error("❌ 이벤트 발행 완전 실패 - 재시도 횟수 초과", throwable.getCause());
		} else {
			log.error("❌ 이벤트 발행 예상치 못한 실패", throwable);
		}
		return null; // CompletableFuture<Void>를 위한 반환값
	}

	/**
	 * 동기 이벤트 발행 (즉시 처리가 필요한 경우)
	 */
	public void publishEventSync(Object event) {
		try {
			eventPublisher.publishEvent(event);
			log.debug("✅ 동기 이벤트 발행 성공 - 타입: {}", event.getClass().getSimpleName());
		} catch (Exception e) {
			log.error("❌ 동기 이벤트 발행 실패 - 타입: {}", event.getClass().getSimpleName(), e);
			throw e; // 동기 처리에서는 예외를 전파
		}
	}

	/**
	 * 배치 이벤트 비동기 발행
	 */
	public CompletableFuture<Void> publishEventsAsync(Object... events) {
		CompletableFuture<Void>[] futures = new CompletableFuture[events.length];

		for (int i = 0; i < events.length; i++) {
			futures[i] = publishEventAsync(events[i]);
		}

		return CompletableFuture.allOf(futures)
				.whenComplete((result, throwable) -> {
					if (throwable != null) {
						log.error("❌ 배치 이벤트 발행 중 일부 실패 - 총 {}개", events.length, throwable);
					} else {
						log.debug("✅ 배치 이벤트 발행 완료 - 총 {}개", events.length);
					}
				});
	}
}