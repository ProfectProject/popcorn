package com.popcorn.demo.global.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
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
@RequiredArgsConstructor
public class AsyncConfig {

	private final OrderProperties orderProperties;

	@Bean(name = "applicationTaskExecutor")
	public Executor applicationTaskExecutor() {
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
}
