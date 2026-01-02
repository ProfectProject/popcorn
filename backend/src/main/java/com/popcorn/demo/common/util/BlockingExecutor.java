package com.popcorn.demo.common.util;

import java.util.function.Supplier;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

public class BlockingExecutor {
	private final Scheduler scheduler;

	public BlockingExecutor(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	/**
	 * 사용 예시:
	 * - 블로킹 연산을 boundedElastic 스레드 풀에서 실행합니다.
	 *
	 * 예:
	 * blockingExecutor.execute(() -> repository.findById(id).orElse(null));
	 */
	public <T> Mono<T> execute(Supplier<T> supplier) {
		return Mono.fromCallable(supplier::get).subscribeOn(scheduler);
	}

	/**
	 * 사용 예시:
	 * - 반환 값이 없는 블로킹 작업을 안전하게 실행합니다.
	 *
	 * 예:
	 * blockingExecutor.run(() -> jdbcTemplate.execute("SELECT 1"));
	 */
	public Mono<Void> run(Runnable runnable) {
		return Mono.fromRunnable(runnable).subscribeOn(scheduler).then();
	}
}
