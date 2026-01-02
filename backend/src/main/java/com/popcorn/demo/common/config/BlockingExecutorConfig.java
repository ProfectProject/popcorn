package com.popcorn.demo.common.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.popcorn.demo.common.util.BlockingExecutor;
import com.popcorn.demo.common.util.BlockingTxExecutor;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
public class BlockingExecutorConfig {

	/**
	 * 블로킹 작업(JPA/JDBC 등)을 위한 전용 스케줄러.
	 * WebFlux의 이벤트 루프를 막지 않도록 boundedElastic을 사용합니다.
	 */
	@Bean
	public Scheduler blockingScheduler() {
		return Schedulers.boundedElastic();
	}

	/**
	 * 블로킹 작업을 안전하게 위임하기 위한 실행 유틸.
	 */
	@Bean
	public BlockingExecutor blockingExecutor(Scheduler blockingScheduler) {
		return new BlockingExecutor(blockingScheduler);
	}

	/**
	 * JDBC 트랜잭션 템플릿 (블로킹 트랜잭션 실행용).
	 */
	@Bean
	public TransactionTemplate jdbcTransactionTemplate(
			@Qualifier("jdbcTransactionManager") PlatformTransactionManager transactionManager) {
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
