package com.popcorn.demo.common.aop;

import java.lang.reflect.Method;
import java.util.Arrays;

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

import com.popcorn.demo.common.annotation.RetryOnFailure;

/**
 * @RetryOnFailure 어노테이션을 처리하는 AOP 어드바이스
 *
 * 메서드 실행 실패 시 자동으로 재시도를 수행합니다.
 */
@Aspect
@Component
@Order(5) // 중간 우선순위
public class RetryAspect {

    private static final Logger log = LoggerFactory.getLogger(RetryAspect.class);

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * @RetryOnFailure 어노테이션이 적용된 메서드를 인터셉트합니다.
     */
    @Around("@annotation(retryOnFailure)")
    public Object handleRetry(ProceedingJoinPoint joinPoint, RetryOnFailure retryOnFailure) throws Throwable {

        String methodName = joinPoint.getSignature().toShortString();
        int maxAttempts = retryOnFailure.maxAttempts();

        if (maxAttempts <= 0) {
            log.warn("최대 시도 횟수가 0 이하입니다. 직접 실행: {}", methodName);
            return joinPoint.proceed();
        }

        long backoffMillis = retryOnFailure.backoffMillis();
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (retryOnFailure.logRetryAttempts() && attempt > 1) {
                    log.info("재시도 중: method={}, attempt={}/{}", methodName, attempt, maxAttempts);
                }

                Object result = joinPoint.proceed();

                if (attempt > 1 && retryOnFailure.logRetryAttempts()) {
                    log.info("재시도 성공: method={}, attempt={}/{}", methodName, attempt, maxAttempts);
                }

                return result;

            } catch (Exception e) {
                lastException = e;

                // 마지막 시도인 경우 예외 던지기
                if (attempt >= maxAttempts) {
                    if (retryOnFailure.logRetryAttempts()) {
                        log.error("모든 재시도 실패: method={}, attempts={}", methodName, maxAttempts, e);
                    }
                    break;
                }

                // 재시도 가능한 예외인지 확인
                if (!shouldRetry(e, retryOnFailure, joinPoint)) {
                    if (retryOnFailure.logRetryAttempts()) {
                        log.warn("재시도 불가능한 예외: method={}, exception={}",
                                methodName, e.getClass().getSimpleName());
                    }
                    break;
                }

                // 백오프 대기
                if (backoffMillis > 0) {
                    try {
                        Thread.sleep(backoffMillis);

                        // 지수 백오프 적용
                        if (retryOnFailure.exponentialBackoff()) {
                            backoffMillis = Math.min(
                                (long) (backoffMillis * retryOnFailure.multiplier()),
                                retryOnFailure.maxBackoffMillis()
                            );
                        }

                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("재시도 대기 중 인터럽트됨", ie);
                    }
                }

                if (retryOnFailure.logRetryAttempts()) {
                    log.warn("재시도 예정: method={}, attempt={}/{}, exception={}, nextBackoff={}ms",
                            methodName, attempt, maxAttempts,
                            e.getClass().getSimpleName(), backoffMillis);
                }
            }
        }

        // 폴백 메서드가 있는 경우 실행
        if (StringUtils.hasText(retryOnFailure.fallbackMethod())) {
            try {
                return executeFallbackMethod(joinPoint, retryOnFailure.fallbackMethod(), lastException);
            } catch (Exception e) {
                log.error("폴백 메서드 실행 실패: method={}, fallback={}",
                         methodName, retryOnFailure.fallbackMethod(), e);
            }
        }

        throw lastException;
    }

    /**
     * 재시도 가능한 예외인지 확인합니다.
     */
    private boolean shouldRetry(Exception exception, RetryOnFailure retryOnFailure, ProceedingJoinPoint joinPoint) {

        // noRetryOn 예외 타입 체크 (재시도 금지)
        if (retryOnFailure.noRetryOn().length > 0) {
            boolean isNoRetryException = Arrays.stream(retryOnFailure.noRetryOn())
                .anyMatch(exceptionType -> exceptionType.isAssignableFrom(exception.getClass()));

            if (isNoRetryException) {
                return false;
            }
        }

        // retryOn 예외 타입 체크 (재시도 허용)
        if (retryOnFailure.retryOn().length > 0) {
            boolean isRetryException = Arrays.stream(retryOnFailure.retryOn())
                .anyMatch(exceptionType -> exceptionType.isAssignableFrom(exception.getClass()));

            if (!isRetryException) {
                return false;
            }
        }

        // 재시도 조건 SpEL 평가
        if (StringUtils.hasText(retryOnFailure.retryCondition())) {
            try {
                Boolean shouldRetry = evaluateCondition(joinPoint, exception, retryOnFailure.retryCondition());
                if (shouldRetry != null && !shouldRetry) {
                    return false;
                }
            } catch (Exception e) {
                log.error("재시도 조건 평가 실패: {}", retryOnFailure.retryCondition(), e);
            }
        }

        // 재시도 불가 조건 SpEL 평가
        if (StringUtils.hasText(retryOnFailure.noRetryCondition())) {
            try {
                Boolean noRetry = evaluateCondition(joinPoint, exception, retryOnFailure.noRetryCondition());
                if (noRetry != null && noRetry) {
                    return false;
                }
            } catch (Exception e) {
                log.error("재시도 불가 조건 평가 실패: {}", retryOnFailure.noRetryCondition(), e);
            }
        }

        return true;
    }

    /**
     * SpEL 조건을 평가합니다.
     */
    private Boolean evaluateCondition(ProceedingJoinPoint joinPoint, Exception exception, String condition) {
        EvaluationContext context = new StandardEvaluationContext();

        // 메서드 파라미터 추가
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        // 예외 정보 추가
        context.setVariable("exception", exception);
        context.setVariable("exceptionType", exception.getClass());
        context.setVariable("exceptionMessage", exception.getMessage());

        Object result = expressionParser.parseExpression(condition).getValue(context);
        return result instanceof Boolean ? (Boolean) result : null;
    }

    /**
     * 폴백 메서드를 실행합니다.
     */
    private Object executeFallbackMethod(ProceedingJoinPoint joinPoint, String fallbackMethodName, Exception lastException) throws Exception {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        try {
            // 동일한 파라미터 타입으로 폴백 메서드 찾기
            Method fallbackMethod = targetClass.getDeclaredMethod(fallbackMethodName, signature.getParameterTypes());
            fallbackMethod.setAccessible(true);

            return fallbackMethod.invoke(joinPoint.getTarget(), joinPoint.getArgs());

        } catch (NoSuchMethodException e) {
            // 예외 파라미터 포함 폴백 메서드 찾기 (추가적인 Exception 파라미터)
            Class<?>[] paramTypesWithException = new Class<?>[signature.getParameterTypes().length + 1];
            System.arraycopy(signature.getParameterTypes(), 0, paramTypesWithException, 0, signature.getParameterTypes().length);
            paramTypesWithException[paramTypesWithException.length - 1] = Exception.class;

            try {
                Method fallbackMethod = targetClass.getDeclaredMethod(fallbackMethodName, paramTypesWithException);
                fallbackMethod.setAccessible(true);

                Object[] argsWithException = new Object[joinPoint.getArgs().length + 1];
                System.arraycopy(joinPoint.getArgs(), 0, argsWithException, 0, joinPoint.getArgs().length);
                argsWithException[argsWithException.length - 1] = lastException;

                return fallbackMethod.invoke(joinPoint.getTarget(), argsWithException);

            } catch (NoSuchMethodException e2) {
                throw new RuntimeException("폴백 메서드를 찾을 수 없습니다: " + fallbackMethodName, e2);
            }
        }
    }
}