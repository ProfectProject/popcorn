package com.popcorn.demo.common.aop;

import java.lang.reflect.Method;

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

import com.popcorn.demo.common.annotation.Idempotent;
import com.popcorn.demo.common.cache.IdempotencyService;
import com.popcorn.demo.common.cache.IdempotentOperation;

import lombok.RequiredArgsConstructor;

/**
 * @Idempotent 어노테이션을 처리하는 AOP 어드바이스
 *
 * 멱등성 처리를 위해 IdempotencyService를 사용하여
 * 동일한 요청에 대해 캐시된 결과를 반환하거나 새로 실행합니다.
 */
@Aspect
@Component
@Order(1) // 다른 AOP보다 먼저 실행
@RequiredArgsConstructor
public class IdempotentAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotentAspect.class);

    private final IdempotencyService idempotencyService;
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * @Idempotent 어노테이션이 적용된 메서드를 인터셉트합니다.
     */
    @Around("@annotation(idempotent)")
    public Object handleIdempotent(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {

        // 멱등성 키 생성
        String idempotencyKey = generateIdempotencyKey(joinPoint, idempotent);

        if (!StringUtils.hasText(idempotencyKey)) {
            log.debug("멱등성 키가 비어있음. 직접 실행: {}", joinPoint.getSignature());
            return joinPoint.proceed();
        }

        // 응답 타입 결정
        Class<?> responseType = determineResponseType(joinPoint, idempotent);

        log.debug("멱등성 처리 시작: key={}, method={}", idempotencyKey, joinPoint.getSignature());

        // IdempotencyService를 통한 멱등성 처리
        @SuppressWarnings("unchecked")
        IdempotencyService.IdempotencyResult<Object> result = idempotencyService.processRequest(
            idempotencyKey,
            new IdempotentOperation<Object>() {
                @Override
                public Object execute() throws Exception {
                    try {
                        return joinPoint.proceed();
                    } catch (Throwable e) {
                        if (e instanceof RuntimeException) {
                            throw (RuntimeException) e;
                        }
                        if (e instanceof Exception) {
                            throw (Exception) e;
                        }
                        throw new RuntimeException("Method execution failed", e);
                    }
                }
            },
            (Class<Object>) responseType
        );

        if (result.isFromCache()) {
            log.debug("멱등성 캐시 히트: key={}, originalTime={}",
                     idempotencyKey, result.getOriginalExecutionTime());
        } else {
            log.debug("멱등성 새 실행 완료: key={}", idempotencyKey);
        }

        return result.getResult();
    }

    /**
     * 멱등성 키를 생성합니다.
     */
    private String generateIdempotencyKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {

        // SpEL 표현식이 있는 경우 우선 사용
        if (StringUtils.hasText(idempotent.keyExpression())) {
            try {
                String dynamicKey = evaluateSpelExpression(joinPoint, idempotent.keyExpression());
                return buildFinalKey(idempotent.keyPrefix(), dynamicKey);
            } catch (Exception e) {
                log.error("SpEL 표현식 평가 실패: {}", idempotent.keyExpression(), e);
            }
        }

        // 정적 키가 있는 경우 사용
        if (StringUtils.hasText(idempotent.staticKey())) {
            return buildFinalKey(idempotent.keyPrefix(), idempotent.staticKey());
        }

        // 둘 다 없는 경우 메서드 시그니처 기반 키 생성
        String methodKey = joinPoint.getSignature().toString();
        return buildFinalKey(idempotent.keyPrefix(), methodKey);
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
            context.setVariable(paramNames[i], args[i]);
            context.setVariable("p" + i, args[i]); // #p0, #p1, ... 형태로도 접근 가능
        }

        Object result = expressionParser.parseExpression(expression).getValue(context);
        return result != null ? result.toString() : "";
    }

    /**
     * 최종 멱등성 키를 생성합니다.
     */
    private String buildFinalKey(String prefix, String key) {
        if (StringUtils.hasText(prefix)) {
            return prefix + ":" + key;
        }
        return key;
    }

    /**
     * 응답 타입을 결정합니다.
     */
    private Class<?> determineResponseType(ProceedingJoinPoint joinPoint, Idempotent idempotent) {

        // 어노테이션에 명시적으로 지정된 경우
        if (idempotent.responseType() != Object.class) {
            return idempotent.responseType();
        }

        // 메서드 반환 타입 사용
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> returnType = signature.getReturnType();

        // void 타입인 경우 Object로 처리
        if (returnType == void.class || returnType == Void.class) {
            return Object.class;
        }

        return returnType;
    }
}