package com.popcorn.common.aop;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

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

import com.popcorn.common.annotation.CacheResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.RequiredArgsConstructor;

/**
 * @CacheResult 어노테이션을 처리하는 AOP 어드바이스
 *
 * 메서드 실행 결과를 캐싱하여 성능을 향상시킵니다.
 */
@Aspect
@Component
@Order(3) // RateLimit 다음에 실행
@RequiredArgsConstructor
public class CacheResultAspect {

    private static final Logger log = LoggerFactory.getLogger(CacheResultAspect.class);

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    // 캐시 이름별로 Caffeine 캐시 인스턴스 관리
    private final Map<String, Cache<String, CacheWrapper>> caches = new ConcurrentHashMap<>();

    /**
     * @CacheResult 어노테이션이 적용된 메서드를 인터셉트합니다.
     */
    @Around("@annotation(cacheResult)")
    public Object handleCacheResult(ProceedingJoinPoint joinPoint, CacheResult cacheResult) throws Throwable {

        // 캐시 키 생성
        String cacheKey = generateCacheKey(joinPoint, cacheResult);

        if (!StringUtils.hasText(cacheKey)) {
            log.debug("캐시 키가 비어있음. 직접 실행: {}", joinPoint.getSignature());
            return joinPoint.proceed();
        }

        // 캐시 조건 확인
        if (!shouldCache(joinPoint, null, cacheResult)) {
            log.debug("캐시 조건 불만족. 직접 실행: key={}", cacheKey);
            return joinPoint.proceed();
        }

        // 캐시 인스턴스 가져오기
        Cache<String, CacheWrapper> cache = getOrCreateCache(cacheResult);

        // 캐시에서 조회
        CacheWrapper cachedWrapper = cache.getIfPresent(cacheKey);
        if (cachedWrapper != null && !cachedWrapper.isExpired()) {
            Object cachedResult = cachedWrapper.value();

            // null 캐싱이 비활성화되어 있고 결과가 null인 경우 캐시 미사용
            if (!cacheResult.cacheNull() && cachedResult == null) {
                log.debug("null 캐싱 비활성화로 캐시 미사용: key={}", cacheKey);
                return joinPoint.proceed();
            }

            log.debug("캐시 히트: key={}, method={}", cacheKey, joinPoint.getSignature());
            return cachedResult;
        }

        // 캐시 미스 - 메서드 실행
        log.debug("캐시 미스: key={}, method={}", cacheKey, joinPoint.getSignature());

        Object result;
        try {
            result = joinPoint.proceed();

            // unless 조건 확인 (결과가 있는 경우)
            if (!shouldCacheResult(joinPoint, result, cacheResult)) {
                log.debug("캐시 제외 조건 만족. 캐싱하지 않음: key={}", cacheKey);
                return result;
            }

            // 결과 캐싱
            if (cacheResult.cacheNull() || result != null) {
                CacheWrapper wrapper = new CacheWrapper(result,
                    cacheResult.ttlSeconds() > 0 ?
                        System.currentTimeMillis() + (cacheResult.ttlSeconds() * 1000L) : -1);

                cache.put(cacheKey, wrapper);
                log.debug("캐시 저장: key={}, method={}", cacheKey, joinPoint.getSignature());
            }

        } catch (Exception e) {
            log.debug("메서드 실행 실패로 캐싱 안 함: key={}, exception={}",
                     cacheKey, e.getClass().getSimpleName());
            throw e;
        }

        return result;
    }

    /**
     * 캐시 키를 생성합니다.
     */
    private String generateCacheKey(ProceedingJoinPoint joinPoint, CacheResult cacheResult) {

        // SpEL 표현식이 있는 경우 우선 사용
        if (StringUtils.hasText(cacheResult.keyExpression())) {
            try {
                String dynamicKey = evaluateSpelExpression(joinPoint, null, cacheResult.keyExpression());
                return buildFinalKey(cacheResult.keyPrefix(), dynamicKey);
            } catch (Exception e) {
                log.error("캐시 키 SpEL 표현식 평가 실패: {}", cacheResult.keyExpression(), e);
            }
        }

        // 정적 키가 있는 경우 사용
        if (StringUtils.hasText(cacheResult.staticKey())) {
            return buildFinalKey(cacheResult.keyPrefix(), cacheResult.staticKey());
        }

        // 둘 다 없는 경우 메서드 시그니처와 파라미터 기반 키 생성
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(signature.getMethod().getName());

        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            keyBuilder.append("(");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) keyBuilder.append(",");
                keyBuilder.append(args[i] != null ? args[i].toString() : "null");
            }
            keyBuilder.append(")");
        }

        return buildFinalKey(cacheResult.keyPrefix(), keyBuilder.toString());
    }

    /**
     * SpEL 표현식을 평가합니다.
     */
    private String evaluateSpelExpression(ProceedingJoinPoint joinPoint, Object result, String expression) {
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
                log.debug("Cache: Authentication이 null입니다. 익명 사용자로 처리");
                context.setVariable("authentication", new SafeAuthenticationWrapper());
            }
        }

        // 결과값 추가 (unless 조건에서 사용)
        if (result != null) {
            context.setVariable("result", result);
        }

        try {
            Object expressionResult = expressionParser.parseExpression(expression).getValue(context);
            return expressionResult != null ? expressionResult.toString() : "";
        } catch (Exception e) {
            log.warn("캐시 SpEL 표현식 평가 실패: {} (expression: {})", e.getMessage(), expression);
            // 기본 키로 폴백
            return "anonymous";
        }
    }

    /**
     * 최종 캐시 키를 생성합니다.
     */
    private String buildFinalKey(String prefix, String key) {
        if (StringUtils.hasText(prefix)) {
            return prefix + ":" + key;
        }
        return key;
    }

    /**
     * 캐시 조건을 확인합니다.
     */
    private boolean shouldCache(ProceedingJoinPoint joinPoint, Object result, CacheResult cacheResult) {
        if (!StringUtils.hasText(cacheResult.condition())) {
            return true;
        }

        try {
            Boolean condition = evaluateConditionExpression(joinPoint, result, cacheResult.condition());
            return condition == null || condition;
        } catch (Exception e) {
            log.error("캐시 조건 평가 실패: {}", cacheResult.condition(), e);
            return true;
        }
    }

    /**
     * 캐시 결과 저장 여부를 확인합니다 (unless 조건).
     */
    private boolean shouldCacheResult(ProceedingJoinPoint joinPoint, Object result, CacheResult cacheResult) {
        if (!StringUtils.hasText(cacheResult.unless())) {
            return true;
        }

        try {
            Boolean unless = evaluateConditionExpression(joinPoint, result, cacheResult.unless());
            return unless == null || !unless;
        } catch (Exception e) {
            log.error("캐시 제외 조건 평가 실패: {}", cacheResult.unless(), e);
            return true;
        }
    }

    /**
     * 조건 표현식을 평가합니다.
     */
    private Boolean evaluateConditionExpression(ProceedingJoinPoint joinPoint, Object result, String expression) {
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
                log.debug("Cache Condition: Authentication이 null입니다. 익명 사용자로 처리");
                context.setVariable("authentication", new SafeAuthenticationWrapper());
            }
        }

        // 결과값 추가
        if (result != null) {
            context.setVariable("result", result);
        }

        try {
            Object expressionResult = expressionParser.parseExpression(expression).getValue(context);
            return expressionResult instanceof Boolean ? (Boolean) expressionResult : null;
        } catch (Exception e) {
            log.warn("캐시 조건 표현식 평가 실패: {} (expression: {})", e.getMessage(), expression);
            return null; // 기본적으로 조건을 만족하지 않는 것으로 처리
        }
    }

    /**
     * 캐시 인스턴스를 가져오거나 생성합니다.
     */
    private Cache<String, CacheWrapper> getOrCreateCache(CacheResult cacheResult) {
        return caches.computeIfAbsent(cacheResult.cacheName(), cacheName -> {
            Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();

            // 기본 설정
            cacheBuilder.maximumSize(10000); // 기본 최대 크기

            // TTL이 설정된 경우 expireAfterWrite 적용
            if (cacheResult.ttlSeconds() > 0) {
                cacheBuilder.expireAfterWrite(Duration.ofSeconds(cacheResult.ttlSeconds()));
            }

            log.info("캐시 생성: name={}, ttl={}초", cacheName, cacheResult.ttlSeconds());
            return cacheBuilder.build();
        });
    }

    /**
     * 캐시 값을 감싸는 래퍼 클래스
     *
     * @param expireTime -1이면 만료시간 없음
     */
        private record CacheWrapper(Object value, long expireTime) {

        boolean isExpired() {
                return expireTime > 0 && System.currentTimeMillis() > expireTime;
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
}