package com.popcorn.demo.domain.order.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.popcorn.demo.domain.order.config.OrderProperties;

class AsyncEventPublisherTest {

	@Test
	@DisplayName("동기 이벤트 발행은 예외를 전파한다")
	void publishEventSync() {
		CountingPublisher publisher = new CountingPublisher();
		AsyncEventPublisher async = new AsyncEventPublisher(publisher, new OrderProperties(), Runnable::run);

		Object event = new Object();
		async.publishEventSync(event);
		publisher.assertPublished(1);

		publisher.throwOnPublish(new RuntimeException("fail"));
		assertThatThrownBy(() -> async.publishEventSync(new Object()))
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	@DisplayName("배치 이벤트 발행은 모든 이벤트를 처리한다")
	void publishEventsAsync() {
		CountingPublisher publisher = new CountingPublisher();
		OrderProperties properties = new OrderProperties();
		properties.getAsync().setEventRetryCount(1);
		Executor executor = Runnable::run;

		AsyncEventPublisher async = new AsyncEventPublisher(publisher, properties, executor);

		Object event1 = new Object();
		Object event2 = new Object();
		async.publishEventsAsync(event1, event2).join();

		publisher.assertPublished(2);
	}

	@Test
	@DisplayName("비동기 실패 핸들러는 예외 유형별 분기를 처리한다")
	void handleAsyncFailureBranches() {
		ApplicationEventPublisher publisher = new CountingPublisher();
		AsyncEventPublisher async = new AsyncEventPublisher(publisher, new OrderProperties(), Runnable::run);

		ReflectionTestUtils.invokeMethod(async, "handleAsyncFailure", new TimeoutException("timeout"));
		ReflectionTestUtils.invokeMethod(async, "handleAsyncFailure",
				new CompletionException(new IllegalStateException("boom")));
		ReflectionTestUtils.invokeMethod(async, "handleAsyncFailure", new IllegalArgumentException("other"));
	}

	private static final class CountingPublisher implements ApplicationEventPublisher {
		private int publishCount;
		private RuntimeException exceptionToThrow;

		@Override
		public void publishEvent(Object event) {
			publishInternal();
		}

		@Override
		public void publishEvent(ApplicationEvent event) {
			publishInternal();
		}

		void throwOnPublish(RuntimeException exception) {
			this.exceptionToThrow = exception;
		}

		void assertPublished(int expected) {
			if (publishCount != expected) {
				throw new AssertionError("Expected " + expected + " publish calls but was " + publishCount);
			}
		}

		private void publishInternal() {
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
			publishCount++;
		}
	}
}
