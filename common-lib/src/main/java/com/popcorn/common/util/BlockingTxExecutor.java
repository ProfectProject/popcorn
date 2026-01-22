package com.popcorn.common.util;

import java.util.function.Supplier;

import org.springframework.transaction.support.TransactionTemplate;

public class BlockingTxExecutor {
	private final BlockingExecutor blockingExecutor;
	private final TransactionTemplate transactionTemplate;

	public BlockingTxExecutor(BlockingExecutor blockingExecutor, TransactionTemplate transactionTemplate) {
		this.blockingExecutor = blockingExecutor;
		this.transactionTemplate = transactionTemplate;
	}

	/**
	 * 사용 예시:
	 * - JPA 같은 블로킹 호출을 전용 스레드 풀과 트랜잭션 안에서 실행합니다.
	 * - 저장/수정 같은 쓰기 작업도 동일한 방식으로 실행합니다.
	 * - 트랜잭션 경계는 TransactionTemplate이 관리합니다.
	 *
	 * 예:
	 * blockingTxExecutor.execute(() -> jpaRepository.findById(id).orElse(null));
	 */
	public <T> T execute(Supplier<T> supplier) {
		return blockingExecutor.execute(() -> transactionTemplate.execute(status -> supplier.get()));
	}

	public void run(Runnable runnable) {
		// 트랜잭션을 열고, 예외 발생 시 자동으로 롤백됩니다.
		blockingExecutor.run(() -> transactionTemplate.executeWithoutResult(status -> runnable.run()));
	}
}