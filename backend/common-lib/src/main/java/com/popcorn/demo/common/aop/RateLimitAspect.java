package com.popcorn.demo.common.aop;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.popcorn.demo.common.annotation.RateLimit;

/**
 * @RateLimit 어노테이션을 처리하는 AOP 어드바이스
 *
 * API 요청 제한을 수행합니다.
 */
@Aspect
@Component
@Order(2) // Idempotent 다음, 다른 처리 전에 실행
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    // 고정 윈도우용 카운터
    private final Map<String, WindowCounter> fixedWindowCounters = new ConcurrentHashMap<>();

    // 슬라이딩 윈도우용 카운터
    private final Map<String, SlidingWindowCounter> slidingWindowCounters = new ConcurrentHashMap<>();

    // 토큰 버킷용 카운터
    private final Map<String, TokenBucket> tokenBuckets = new ConcurrentHashMap<>();

    /**
     * @RateLimit 어노테이션이 적용된 메서드를 인터셉트합니다.
     */
    @Around("@annotation(rateLimit)")
    public Object handleRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {

        // Rate Limit 키 생성
        String rateLimitKey = generateRateLimitKey(joinPoint, rateLimit);

        if (!StringUtils.hasText(rateLimitKey)) {
            log.warn("Rate Limit 키가 비어있음. 직접 실행: {}", joinPoint.getSignature());
            return joinPoint.proceed();
        }

        // Rate Limit 확인 및 처리
        boolean allowed = isRequestAllowed(rateLimitKey, rateLimit);

        if (!allowed) {
            String errorMessage = rateLimit.errorMessage() + " (key: " + rateLimitKey + ")";
            log.warn("Rate Limit 초과: key={}, method={}", rateLimitKey, joinPoint.getSignature());

            // 커스텀 예외 타입으로 예외 발생
            try {
                throw rateLimit.exceptionType()
                    .getConstructor(String.class)
                    .newInstance(errorMessage);
            } catch (Exception e) {
                // 생성자 호출 실패 시 기본 RuntimeException 사용
                throw new RuntimeException(errorMessage);
            }
        }

        log.debug("Rate Limit 통과: key={}, method={}", rateLimitKey, joinPoint.getSignature());
        return joinPoint.proceed();
    }

    /**
     * Rate Limit 키를 생성합니다.
     */
    private String generateRateLimitKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        try {
            String dynamicKey = evaluateSpelExpression(joinPoint, rateLimit.keyExpression());
            return buildFinalKey(rateLimit.keyPrefix(), dynamicKey);
        } catch (Exception e) {
            log.error("Rate Limit 키 생성 실패: {}", rateLimit.keyExpression(), e);
            return null;
        }
    }

    /**
     * SpEL 표현식을 평가합니다.
     */
    private String evaluateSpelExpression(ProceedingJoinPoint joinPoint, String expression) {
        EvaluationContext context = new StandardEvaluationContext();

        // 메서드 파라미터를 컨텍스트에 추가
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            Object arg = args[i];
            // null 값에 대해서도 변수 설정 (SpEL에서 null 처리를 위해)
            context.setVariable(paramNames[i], arg);

            // Authentication이 null인 경우 안전한 기본값 제공
            if ("authentication".equals(paramNames[i]) && arg == null) {
                log.debug("Rate Limit: Authentication이 null입니다. 익명 사용자로 처리");
                context.setVariable("authentication", new SafeAuthenticationWrapper());
            }
        }

        // 추가 유틸리티 등록
        context.setVariable("methodName", signature.getName());
        context.setVariable("className", signature.getDeclaringType().getSimpleName());

        try {
            Object result = expressionParser.parseExpression(expression).getValue(context);
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            log.warn("SpEL 표현식 평가 실패: {} (expression: {})", e.getMessage(), expression);
            // 기본 키로 폴백
            return "anonymous";
        }
    }

    /**
     * 널 Authentication 객체에 대한 안전한 래퍼
     */
    private static class SafeAuthenticationWrapper {
        private final SafePrincipal principal = new SafePrincipal();

        public SafePrincipal getPrincipal() {
            return principal;
        }

        public String getName() {
            return "anonymous";
        }

        public boolean isAuthenticated() {
            return false;
        }
    }

    /**
     * 널 Principal 객체에 대한 안전한 래퍼
     */
    private static class SafePrincipal {
        public String getUserId() {
            return "anonymous";
        }

        public String getUsername() {
            return "anonymous";
        }

        public String getRole() {
            return "anonymous";
        }
    }

    /**
     * 최종 Rate Limit 키를 생성합니다.
     */
    private String buildFinalKey(String prefix, String key) {
        if (StringUtils.hasText(prefix)) {
            return prefix + ":" + key;
        }
        return key;
    }

    /**
     * 요청이 허용되는지 확인합니다.
     */
    private boolean isRequestAllowed(String key, RateLimit rateLimit) {
        switch (rateLimit.algorithm()) {
            case FIXED_WINDOW:
                return checkFixedWindow(key, rateLimit);
            case SLIDING_WINDOW:
                return checkSlidingWindow(key, rateLimit);
            case TOKEN_BUCKET:
                return checkTokenBucket(key, rateLimit);
            default:
                log.warn("지원하지 않는 Rate Limit 알고리즘: {}", rateLimit.algorithm());
                return true;
        }
    }

    /**
     * 고정 윈도우 알고리즘으로 Rate Limit 확인
     */
    private boolean checkFixedWindow(String key, RateLimit rateLimit) {
        long windowSizeMillis = rateLimit.window() * 1000L;
        long currentTime = System.currentTimeMillis();
        long windowStart = (currentTime / windowSizeMillis) * windowSizeMillis;

        WindowCounter counter = fixedWindowCounters.computeIfAbsent(key, k -> new WindowCounter());

        synchronized (counter) {
            // 새로운 윈도우인 경우 카운터 리셋
            if (counter.windowStart != windowStart) {
                counter.windowStart = windowStart;
                counter.count.set(0);
            }

            long currentCount = counter.count.incrementAndGet();
            return currentCount <= rateLimit.requests();
        }
    }

    /**
     * 슬라이딩 윈도우 알고리즘으로 Rate Limit 확인
     */
    private boolean checkSlidingWindow(String key, RateLimit rateLimit) {
        long windowSizeMillis = rateLimit.window() * 1000L;
        long currentTime = System.currentTimeMillis();

        SlidingWindowCounter counter = slidingWindowCounters.computeIfAbsent(key,
            k -> new SlidingWindowCounter(windowSizeMillis));

        synchronized (counter) {
            // 만료된 요청들 정리
            counter.removeExpiredRequests(currentTime);

            // 현재 윈도우 내 요청 수 확인
            if (counter.requestTimes.size() >= rateLimit.requests()) {
                return false;
            }

            // 새 요청 시간 추가
            counter.requestTimes.add(currentTime);
            return true;
        }
    }

    /**
     * 토큰 버킷 알고리즘으로 Rate Limit 확인
     */
    private boolean checkTokenBucket(String key, RateLimit rateLimit) {
        long refillIntervalMillis = rateLimit.window() * 1000L;

        TokenBucket bucket = tokenBuckets.computeIfAbsent(key,
            k -> new TokenBucket(rateLimit.requests(), refillIntervalMillis));

        synchronized (bucket) {
            long currentTime = System.currentTimeMillis();

            // 토큰 보충
            long timePassed = currentTime - bucket.lastRefillTime;
            if (timePassed >= bucket.refillIntervalMillis) {
                long tokensToAdd = (timePassed / bucket.refillIntervalMillis);
                bucket.tokens = Math.min(bucket.capacity, bucket.tokens + tokensToAdd);
                bucket.lastRefillTime = currentTime;
            }

            // 토큰 소모
            if (bucket.tokens > 0) {
                bucket.tokens--;
                return true;
            }

            return false;
        }
    }

    /**
     * 고정 윈도우 카운터
     */
    private static class WindowCounter {
        volatile long windowStart = 0;
        final AtomicLong count = new AtomicLong(0);
    }

    /**
     * 슬라이딩 윈도우 카운터
     */
    private static class SlidingWindowCounter {
        final java.util.List<Long> requestTimes = new java.util.ArrayList<>();
        final long windowSizeMillis;

        SlidingWindowCounter(long windowSizeMillis) {
            this.windowSizeMillis = windowSizeMillis;
        }

        void removeExpiredRequests(long currentTime) {
            requestTimes.removeIf(time -> currentTime - time > windowSizeMillis);
        }
    }

    /**
     * 토큰 버킷
     */
    private static class TokenBucket {
        volatile long tokens;
        volatile long lastRefillTime;
        final long capacity;
        final long refillIntervalMillis;

        TokenBucket(long capacity, long refillIntervalMillis) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
            this.refillIntervalMillis = refillIntervalMillis;
        }
    }

    /**
     * 모든 Rate Limit 캐시를 초기화합니다. (테스트용)
     */
    public void clearAllCaches() {
        fixedWindowCounters.clear();
        slidingWindowCounters.clear();
        tokenBuckets.clear();
        log.debug("🧹 Rate Limit 캐시 초기화 완료");
    }

    /**
     * 특정 키의 Rate Limit 캐시를 초기화합니다. (테스트용)
     */
    public void clearCacheForKey(String key) {
        fixedWindowCounters.remove(key);
        slidingWindowCounters.remove(key);
        tokenBuckets.remove(key);
        log.debug("🧹 Rate Limit 캐시 초기화 완료 - 키: {}", key);
    }
}