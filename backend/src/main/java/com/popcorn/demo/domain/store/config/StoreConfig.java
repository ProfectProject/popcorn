package com.popcorn.demo.domain.store.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@ComponentScan(basePackages = {
		"com.popcorn.demo.domain.store.service",
		"com.popcorn.demo.domain.store.repository",
		"com.popcorn.demo.domain.store.controller"
})
public class StoreConfig {

	@Bean(name = "storeTaskExecutor")
	public Executor storeTaskExecutor() {
		return createExecutor(5, 10, 100, "StoreAsync-", 60);
	}

	@Bean(name = "storeValidationTaskExecutor")
	public Executor storeValidationTaskExecutor() {
		return createExecutor(2, 5, 50, "StoreValidation-", 30);
	}

	private ThreadPoolTaskExecutor createExecutor(int corePoolSize, int maxPoolSize, 
												  int queueCapacity, String threadNamePrefix, 
												  int awaitTerminationSeconds) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix(threadNamePrefix);
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
		executor.initialize();
		return executor;
	}

}