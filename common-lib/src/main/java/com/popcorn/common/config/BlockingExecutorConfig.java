package com.popcorn.common.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.popcorn.common.util.BlockingExecutor;
import com.popcorn.common.util.BlockingTxExecutor;

@Configuration
public class BlockingExecutorConfig {

	/**
	 * 블로킹 작업(JPA/JDBC 등)을 위한 전용 스레드 풀.
	 * - MVC 환경에서 느린 I/O 작업을 분리하고 싶을 때 사용합니다.
	 * - 사이즈는 서비스 트래픽 특성에 맞게 조정하세요.
	 */
	@Bean
	public AsyncTaskExecutor blockingTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("blocking-");
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(16);
		executor.setQueueCapacity(200);
		executor.initialize();
		return executor;
	}

	/**
	 * 블로킹 작업을 안전하게 위임하기 위한 실행 유틸.
	 */
	@Bean
	public BlockingExecutor blockingExecutor(@Qualifier("blockingTaskExecutor") AsyncTaskExecutor blockingTaskExecutor) {
		return new BlockingExecutor(blockingTaskExecutor);
	}

	/**
	 * JDBC 트랜잭션 템플릿 (블로킹 트랜잭션 실행용).
	 */
	@Bean
	public TransactionTemplate jdbcTransactionTemplate(
			PlatformTransactionManager transactionManager) {
		return new TransactionTemplate(transactionManager);
	}

	/**
	 * 블로킹 + 트랜잭션 실행 유틸.
	 * 예: blockingTxExecutor.execute(() -> jpaRepository.findById(id).orElse(null));
	 */
	@Bean
	public BlockingTxExecutor blockingTxExecutor(
			BlockingExecutor blockingExecutor,
			TransactionTemplate jdbcTransactionTemplate) {
		return new BlockingTxExecutor(blockingExecutor, jdbcTransactionTemplate);
	}
}