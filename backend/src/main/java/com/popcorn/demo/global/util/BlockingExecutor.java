package com.popcorn.demo.global.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.springframework.core.task.AsyncTaskExecutor;

public class BlockingExecutor {
	private final AsyncTaskExecutor taskExecutor;

	public BlockingExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 사용 예시:
	 * - 블로킹 연산을 전용 스레드 풀에서 실행합니다.
	 * - 호출 스레드는 결과를 기다리므로, 실제로는 "동기 실행"처럼 동작합니다.
	 * - DB/JDBC 같이 오래 걸리는 작업을 메인 요청 스레드에서 분리하고 싶을 때 사용합니다.
	 *
	 * 예:
	 * blockingExecutor.execute(() -> repository.findById(id).orElse(null));
	 */
	public <T> T execute(Supplier<T> supplier) {
		try {
			Future<T> future = taskExecutor.submit(supplier::get);
			return future.get();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Blocking task interrupted", ex);
		} catch (ExecutionException ex) {
			throw new IllegalStateException("Blocking task failed", ex.getCause());
		}
	}

	/**
	 * 사용 예시:
	 * - 반환 값이 없는 블로킹 작업을 안전하게 실행합니다.
	 * - 내부에서 Future.get()을 사용하므로 예외는 그대로 상위로 전파됩니다.
	 *
	 * 예:
	 * blockingExecutor.run(() -> jdbcTemplate.execute("SELECT 1"));
	 */
	public void run(Runnable runnable) {
		try {
			Future<?> future = taskExecutor.submit(runnable);
			future.get();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Blocking task interrupted", ex);
		} catch (ExecutionException ex) {
			throw new IllegalStateException("Blocking task failed", ex.getCause());
		}
	}
}
