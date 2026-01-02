package com.popcorn.demo.global.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 처리용 스레드 풀 설정
 *
 * - @Async 메서드를 별도 스레드 풀에서 실행합니다.
 * - 주문 후처리 알림처럼 응답과 분리된 작업에 사용합니다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

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
}
