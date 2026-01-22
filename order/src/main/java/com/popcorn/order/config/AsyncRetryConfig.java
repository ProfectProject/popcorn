package com.popcorn.order.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 재시도 설정
 *
 * 재시도 로직을 별도 스레드풀에서 실행하여 메인 스레드의 블로킹을 방지합니다.
 * 대량의 재시도 작업이 시스템 성능에 미치는 영향을 최소화합니다.
 */
@Configuration
@EnableAsync
public class AsyncRetryConfig {

    /**
     * 비동기 재시도 전용 Executor
     *
     * 재시도 작업의 특성을 고려한 스레드풀 설정:
     * - 코어 풀 크기: 5 (기본 유지 스레드)
     * - 최대 풀 크기: 20 (최대 동시 재시도)
     * - 큐 용량: 100 (대기 작업 버퍼)
     * - Keep Alive: 60초 (유휴 스레드 유지 시간)
     */
    @Bean("asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본 스레드 수
        executor.setCorePoolSize(5);

        // 최대 스레드 수
        executor.setMaxPoolSize(20);

        // 작업 큐 용량
        executor.setQueueCapacity(100);

        // 유휴 스레드 유지 시간 (초)
        executor.setKeepAliveSeconds(60);

        // 스레드 이름 접두사
        executor.setThreadNamePrefix("retry-");

        // 스레드풀 종료 시 작업 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);

        // 큐가 가득 찬 경우 정책 (호출자 스레드에서 실행)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }

}