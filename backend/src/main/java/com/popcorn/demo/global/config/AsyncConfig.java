package com.popcorn.demo.global.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.popcorn.demo.domain.order.config.OrderProperties;

import lombok.RequiredArgsConstructor;

/**
 * 비동기 처리용 스레드 풀 설정
 *
 * - @Async 메서드를 별도 스레드 풀에서 실행합니다.
 * - 주문 후처리 알림처럼 응답과 분리된 작업에 사용합니다.
 * - 도메인별 전용 스레드 풀 제공
 */
@Configuration
@EnableAsync
@EnableScheduling
@RequiredArgsConstructor
public class AsyncConfig {

	private final OrderProperties orderProperties;

	@Bean(name = "customTaskExecutor")
	public Executor customTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(200);
		executor.setThreadNamePrefix("app-async-");
		executor.initialize();
		return executor;
	}

	/**
	 * 주문 도메인 전용 비동기 실행자
	 * - 이벤트 발행 및 백그라운드 작업 처리
	 * - 설정값 기반 스레드풀 크기 조정
	 */
	@Bean(name = "orderAsyncExecutor")
	public Executor orderAsyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		// 병렬 검증이 활성화된 경우 더 많은 스레드 사용
		if (orderProperties.getAsync().isParallelValidation()) {
			executor.setCorePoolSize(6);
			executor.setMaxPoolSize(12);
			executor.setQueueCapacity(300);
		} else {
			executor.setCorePoolSize(3);
			executor.setMaxPoolSize(6);
			executor.setQueueCapacity(150);
		}

		executor.setThreadNamePrefix("order-async-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(30); // 우아한 종료
		executor.initialize();
		return executor;
	}

	/**
	 * 결제 관련 비동기 작업 전용 스레드 풀
	 * - 재고 차감, QR 발급, 알림 발송 등 후속 작업 처리
	 * - 결제 성공 이벤트 핸들러에서 사용
	 */
	@Bean(name = "paymentTaskExecutor")
	public Executor paymentTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);          // 기본 스레드 수
		executor.setMaxPoolSize(5);           // 최대 스레드 수
		executor.setQueueCapacity(100);       // 대기 큐 크기
		executor.setKeepAliveSeconds(60);     // 스레드 생존 시간
		executor.setThreadNamePrefix("PaymentAsync-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(30);
		executor.initialize();
		return executor;
	}

	/**
	 * QR 체크인 비동기 처리 전용 스레드 풀
	 * - QR 체크인 요청 및 완료 이벤트 처리
	 * - 체크인 관련 알림, 로그, 외부 연동 등
	 */
	@Bean(name = "qrCheckinExecutor")
	public Executor qrCheckinExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);          // 기본 스레드 수
		executor.setMaxPoolSize(3);           // 최대 스레드 수
		executor.setQueueCapacity(50);        // 대기 큐 크기
		executor.setKeepAliveSeconds(60);     // 스레드 생존 시간
		executor.setThreadNamePrefix("QrCheckin-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(30);
		executor.initialize();
		return executor;
	}

	/**
	 * 결제 취소 재시도 전용 스레드 풀
	 * - 실패한 결제 취소의 재시도 처리
	 * - 스케줄러에서 주기적으로 실행되는 재시도 로직
	 */
	@Bean(name = "paymentRetryExecutor")
	public Executor paymentRetryExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);          // 기본 스레드 수
		executor.setMaxPoolSize(3);           // 최대 스레드 수
		executor.setQueueCapacity(50);        // 대기 큐 크기
		executor.setKeepAliveSeconds(120);    // 스레드 생존 시간 (재시도 간격 고려)
		executor.setThreadNamePrefix("PaymentRetry-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(60);
		executor.initialize();
		return executor;
	}
}
