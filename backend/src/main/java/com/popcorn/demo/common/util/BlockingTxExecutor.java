package com.popcorn.demo.common.util;

import java.util.function.Supplier;

import org.springframework.transaction.support.TransactionTemplate;

import reactor.core.publisher.Mono;

public class BlockingTxExecutor {
	private final BlockingExecutor blockingExecutor;
	private final TransactionTemplate transactionTemplate;

	public BlockingTxExecutor(BlockingExecutor blockingExecutor, TransactionTemplate transactionTemplate) {
		this.blockingExecutor = blockingExecutor;
		this.transactionTemplate = transactionTemplate;
	}

	/**
	 * 사용 예시:
	 * - JPA 같은 블로킹 호출을 WebFlux에서 안전하게 실행할 때 사용합니다.
	 * - 트랜잭션 내부에서 실행되며, boundedElastic 스레드 풀로 위임됩니다.
	 *
	 * 예:
	 * blockingTxExecutor.execute(() -> jpaRepository.findById(id).orElse(null));
	 */
	public <T> Mono<T> execute(Supplier<T> supplier) {
		return blockingExecutor.execute(() -> transactionTemplate.execute(status -> supplier.get()));
	}

	public Mono<Void> run(Runnable runnable) {
		return blockingExecutor.run(() -> transactionTemplate.executeWithoutResult(status -> runnable.run()));
	}
}
