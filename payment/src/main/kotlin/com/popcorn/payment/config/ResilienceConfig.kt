package com.popcorn.payment.config

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.Duration
import java.util.concurrent.TimeoutException

/**
 * Resilience4j 설정 (코루틴 호환)
 *
 * 🛡️ 장애 격리 패턴:
 * 1. Circuit Breaker: 장애 서비스 차단
 * 2. Retry: 일시적 실패 재시도
 * 3. Time Limiter: 응답 시간 제한
 *
 * 🔄 코루틴 통합:
 * - suspend 함수와 호환되는 확장 함수 제공
 * - 비동기 처리에 최적화된 타임아웃 관리
 * - 코루틴 컨텍스트 안전성 보장
 */
@Configuration
class ResilienceConfig {

    private val log = LoggerFactory.getLogger(ResilienceConfig::class.java)

    /**
     * 토스페이먼츠 API용 Circuit Breaker 설정
     *
     * 🎯 설정 전략:
     * - 실패율 50% 이상 시 Circuit Open
     * - 최소 호출 수 10회 이후 실패율 계산
     * - Open 상태 30초 유지 후 Half-Open으로 전환
     * - Half-Open에서 5회 연속 성공 시 Close
     */
    @Bean
    fun tossPaymentCircuitBreaker(): CircuitBreaker {
        val config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f)                    // 실패율 50% 임계값
            .slowCallRateThreshold(50.0f)                   // 느린 호출 50% 임계값
            .slowCallDurationThreshold(Duration.ofSeconds(3)) // 3초 이상이면 느린 호출
            .minimumNumberOfCalls(10)                       // 최소 호출 수
            .slidingWindowSize(20)                          // 슬라이딩 윈도우 크기
            .waitDurationInOpenState(Duration.ofSeconds(30)) // Open 상태 대기 시간
            .permittedNumberOfCallsInHalfOpenState(5)       // Half-Open 상태에서 허용 호출 수
            .automaticTransitionFromOpenToHalfOpenEnabled(true) // 자동 전환 활성화
            .recordExceptions(
                // 실패로 기록할 예외들
                WebClientResponseException::class.java,
                SocketTimeoutException::class.java,
                ConnectException::class.java,
                TimeoutException::class.java
            )
            .ignoreExceptions(
                // 무시할 예외들 (Circuit Breaker 통계에서 제외)
                IllegalArgumentException::class.java
            )
            .build()

        return CircuitBreaker.of("tossPaymentApi", config).also { circuitBreaker ->
            // 상태 변화 이벤트 리스너 등록
            circuitBreaker.eventPublisher
                .onStateTransition { event ->
                    log.info("🔄 Circuit Breaker 상태 변경: {} -> {}",
                        event.stateTransition.fromState,
                        event.stateTransition.toState)
                }
                .onCallNotPermitted { _ ->
                    log.warn("🚨 Circuit Breaker OPEN - 호출 차단됨")
                }
                .onFailureRateExceeded { event ->
                    log.error("📈 실패율 임계값 초과: {}%", event.failureRate)
                }
        }
    }

    /**
     * 토스페이먼츠 API용 Retry 설정
     *
     * 🔄 재시도 전략:
     * - 최대 3회 재시도
     * - 지수 백오프: 1초 → 2초 → 4초
     * - HTTP 5xx, 네트워크 오류만 재시도
     */
    @Bean
    fun tossPaymentRetry(): Retry {
        val config = RetryConfig.custom<Any>()
            .maxAttempts(3)                                 // 최대 재시도 횟수
            .intervalFunction { attemptNumber ->
                // 지수 백오프: 1초, 2초, 4초 (밀리초로 반환)
                (1 shl (attemptNumber - 1)) * 1000L
            }
            .retryOnException { exception ->
                // 재시도할 예외 조건
                when (exception) {
                    is WebClientResponseException -> {
                        // HTTP 5xx 에러만 재시도
                        exception.statusCode.is5xxServerError
                    }
                    is SocketTimeoutException,
                    is ConnectException,
                    is TimeoutException -> true
                    else -> false
                }
            }
            .build()

        return Retry.of("tossPaymentApi", config).also { retry ->
            // 재시도 이벤트 리스너
            retry.eventPublisher
                .onRetry { event ->
                    log.warn("🔄 재시도 중: attempt={}/{}, exception={}",
                        event.numberOfRetryAttempts,
                        retry.retryConfig.maxAttempts,
                        event.lastThrowable?.message)
                }
                .onError { event ->
                    log.error("❌ 재시도 최종 실패: attempts={}, exception={}",
                        event.numberOfRetryAttempts,
                        event.lastThrowable?.message)
                }
        }
    }

    /**
     * 토스페이먼츠 API용 Time Limiter 설정
     *
     * ⏱️ 타임아웃 전략:
     * - 최대 5초 응답 시간
     * - 타임아웃 시 즉시 취소
     */
    @Bean
    fun tossPaymentTimeLimiter(): TimeLimiter {
        val config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(15))        // 15초 타임아웃
            .cancelRunningFuture(true)                      // 실행 중인 Future 취소
            .build()

        return TimeLimiter.of("tossPaymentApi", config)
    }
}

/**
 * 코루틴용 Resilience4j 확장 함수들
 */

/**
 * 코루틴에서 Circuit Breaker 실행
 */
suspend fun <T> CircuitBreaker.executeSuspend(block: suspend () -> T): T {
    return this.executeSupplier {
        kotlinx.coroutines.runBlocking { block() }
    }
}

/**
 * 코루틴에서 Retry 실행
 */
suspend fun <T> Retry.executeSuspend(block: suspend () -> T): T {
    return this.executeSupplier {
        kotlinx.coroutines.runBlocking { block() }
    }
}

/**
 * 코루틴에서 타임아웃과 함께 실행
 */
suspend fun <T> executeWithTimeout(
    timeLimiter: TimeLimiter,
    block: suspend () -> T
): T {
    return withTimeout(timeLimiter.timeLimiterConfig.timeoutDuration.toMillis()) {
        block()
    }
}

/**
 * Circuit Breaker + Retry + TimeLimiter를 조합한 실행
 *
 * 💡 사용 예시:
 * ```kotlin
 * val result = executeResilient(
 *     circuitBreaker = tossCircuitBreaker,
 *     retry = tossRetry,
 *     timeLimiter = tossTimeLimiter
 * ) {
 *     tossClient.confirm(request)
 * }
 * ```
 */
suspend fun <T> executeResilient(
    circuitBreaker: CircuitBreaker,
    retry: Retry,
    timeLimiter: TimeLimiter,
    block: suspend () -> T
): T {
    return circuitBreaker.executeSuspend {
        retry.executeSuspend {
            executeWithTimeout(timeLimiter) {
                block()
            }
        }
    }
}

/**
 * Resilience 패턴 통계 수집을 위한 헬퍼 클래스
 */
class ResilienceMetrics(
    private val circuitBreaker: CircuitBreaker,
    private val retry: Retry
) {

    fun getCircuitBreakerMetrics(): CircuitBreakerMetrics {
        val metrics = circuitBreaker.metrics
        return CircuitBreakerMetrics(
            state = circuitBreaker.state.toString(),
            failureRate = metrics.failureRate,
            slowCallRate = metrics.slowCallRate,
            numberOfSuccessfulCalls = metrics.numberOfSuccessfulCalls.toLong(),
            numberOfFailedCalls = metrics.numberOfFailedCalls.toLong(),
            numberOfSlowCalls = metrics.numberOfSlowCalls.toLong(),
            numberOfNotPermittedCalls = metrics.numberOfNotPermittedCalls
        )
    }

    fun getRetryMetrics(): RetryMetrics {
        val metrics = retry.metrics
        return RetryMetrics(
            numberOfSuccessfulCallsWithoutRetryAttempt = metrics.numberOfSuccessfulCallsWithoutRetryAttempt,
            numberOfSuccessfulCallsWithRetryAttempt = metrics.numberOfSuccessfulCallsWithRetryAttempt,
            numberOfFailedCallsWithoutRetryAttempt = metrics.numberOfFailedCallsWithoutRetryAttempt,
            numberOfFailedCallsWithRetryAttempt = metrics.numberOfFailedCallsWithRetryAttempt
        )
    }
}

/**
 * Circuit Breaker 메트릭스 정보
 */
data class CircuitBreakerMetrics(
    val state: String,
    val failureRate: Float,
    val slowCallRate: Float,
    val numberOfSuccessfulCalls: Long,
    val numberOfFailedCalls: Long,
    val numberOfSlowCalls: Long,
    val numberOfNotPermittedCalls: Long
)

/**
 * Retry 메트릭스 정보
 */
data class RetryMetrics(
    val numberOfSuccessfulCallsWithoutRetryAttempt: Long,
    val numberOfSuccessfulCallsWithRetryAttempt: Long,
    val numberOfFailedCallsWithoutRetryAttempt: Long,
    val numberOfFailedCallsWithRetryAttempt: Long
)
