package com.popcorn.demo.domain.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 주문 도메인 DI 설정
 *
 * 주요 기능:
 * - 주문 관련 컴포넌트 스캔
 * - 비동기 처리를 위한 ThreadPool 설정
 * - 주문별 전용 Bean 설정
 */
@Configuration
@EnableAsync
@ComponentScan(basePackages = {
    "com.popcorn.demo.domain.order.service",
    "com.popcorn.demo.domain.order.repository",
    "com.popcorn.demo.domain.order.controller"
})
public class OrderConfig {

    /**
     * 주문 처리 전용 비동기 실행기
     * - 코어 풀 크기: 5개 스레드
     * - 최대 풀 크기: 10개 스레드
     * - 큐 용량: 100개 작업
     * - 스레드 이름 접두사: "OrderAsync-"
     */
    @Bean(name = "orderTaskExecutor")
    public Executor orderTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("OrderAsync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 주문 검증 전용 비동기 실행기
     * - 빠른 응답을 위한 소규모 풀
     * - 코어 풀 크기: 2개 스레드
     * - 최대 풀 크기: 5개 스레드
     */
    @Bean(name = "orderValidationTaskExecutor")
    public Executor orderValidationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("OrderValidation-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}