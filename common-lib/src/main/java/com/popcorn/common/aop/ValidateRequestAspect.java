package com.popcorn.common.aop;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.popcorn.common.annotation.ValidateRequest;

import lombok.RequiredArgsConstructor;

/**
 * @ValidateRequest 어노테이션을 처리하는 AOP 어드바이스
 *
 * 요청 데이터의 유효성을 검증합니다.
 */
@Aspect
@Component
@Order(4) // CacheResult 다음에 실행
@RequiredArgsConstructor
public class ValidateRequestAspect {

    private static final Logger log = LoggerFactory.getLogger(ValidateRequestAspect.class);

    private final Validator validator;

    /**
     * @ValidateRequest 어노테이션이 적용된 메서드를 인터셉트합니다.
     */
    @Around("@annotation(validateRequest)")
    public Object handleValidateRequest(ProceedingJoinPoint joinPoint, ValidateRequest validateRequest) throws Throwable {

        String methodName = joinPoint.getSignature().toShortString();
        log.debug("요청 검증 시작: {}", methodName);

        // 메서드 파라미터 검증
        validateMethodParameters(joinPoint, validateRequest);

        // 커스텀 검증기 실행
        executeCustomValidators(joinPoint, validateRequest);

        log.debug("요청 검증 통과: {}", methodName);

        return joinPoint.proceed();
    }

    /**
     * 메서드 파라미터를 검증합니다.
     */
    private void validateMethodParameters(ProceedingJoinPoint joinPoint, ValidateRequest validateRequest) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (paramNames == null || args == null) {
            return;
        }

        List<String> includeParams = Arrays.asList(validateRequest.includeParams());
        List<String> excludeParams = Arrays.asList(validateRequest.excludeParams());

        for (int i = 0; i < Math.min(paramNames.length, args.length); i++) {
            String paramName = paramNames[i];
            Object paramValue = args[i];

            // 포함/제외 파라미터 확인
            boolean shouldValidate = shouldValidateParameter(paramName, includeParams, excludeParams);
            if (!shouldValidate) {
                continue;
            }

            // 기본 검증 (null, empty)
            performBasicValidation(paramName, paramValue, validateRequest);

            // Bean Validation 수행
            if (paramValue != null) {
                performBeanValidation(paramName, paramValue, validateRequest);
            }
        }
    }

    /**
     * 파라미터 검증 대상 여부를 확인합니다.
     */
    private boolean shouldValidateParameter(String paramName, List<String> includeParams, List<String> excludeParams) {

        // includeParams가 지정된 경우, 해당 파라미터만 검증
        if (!includeParams.isEmpty()) {
            return includeParams.contains(paramName);
        }

        // excludeParams에 포함된 경우 검증하지 않음
        return !excludeParams.contains(paramName);
    }

    /**
     * 기본 검증 (null, empty)을 수행합니다.
     */
    private void performBasicValidation(String paramName, Object paramValue, ValidateRequest validateRequest) {

        // null 검증
        if (validateRequest.validateNulls() && paramValue == null) {
            throwValidationException(validateRequest,
                String.format("파라미터 '%s'는 null일 수 없습니다.", paramName));
        }

        // empty 검증
        if (validateRequest.validateEmpty() && paramValue != null) {
            if (paramValue instanceof String && ((String) paramValue).trim().isEmpty()) {
                throwValidationException(validateRequest,
                    String.format("파라미터 '%s'는 비어있을 수 없습니다.", paramName));
            }
        }
    }

    /**
     * Bean Validation을 수행합니다.
     */
    private void performBeanValidation(String paramName, Object paramValue, ValidateRequest validateRequest) {

        Class<?>[] groups = validateRequest.groups();
        Set<ConstraintViolation<Object>> violations;

        if (groups.length > 0) {
            violations = validator.validate(paramValue, groups);
        } else {
            violations = validator.validate(paramValue);
        }

        if (!violations.isEmpty()) {
            StringBuilder errorMessages = new StringBuilder();
            errorMessages.append(String.format("파라미터 '%s' 검증 실패: ", paramName));

            for (ConstraintViolation<Object> violation : violations) {
                errorMessages.append(String.format("[%s: %s] ",
                    violation.getPropertyPath(), violation.getMessage()));
            }

            throwValidationException(validateRequest, errorMessages.toString());
        }
    }

    /**
     * 커스텀 검증기를 실행합니다.
     */
    private void executeCustomValidators(ProceedingJoinPoint joinPoint, ValidateRequest validateRequest) {
        Class<?>[] customValidators = validateRequest.customValidator();

        if (customValidators.length == 0) {
            return;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();

        for (Class<?> validatorClass : customValidators) {
            try {
                executeCustomValidator(validatorClass, args, signature, validateRequest);
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throwValidationException(validateRequest,
                    "커스텀 검증기 실행 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 개별 커스텀 검증기를 실행합니다.
     */
    private void executeCustomValidator(Class<?> validatorClass, Object[] args,
                                       MethodSignature signature, ValidateRequest validateRequest) throws Exception {

        // RequestValidator 인터페이스를 구현한 검증기 찾기
        Method validateMethod = findValidateMethod(validatorClass);

        if (validateMethod != null) {
            // 검증기 인스턴스 생성
            Object validatorInstance = createValidatorInstance(validatorClass);

            // 검증 메서드 호출
            Object result = validateMethod.invoke(validatorInstance, (Object) args);

            // boolean 결과인 경우 false면 예외 발생
            if (result instanceof Boolean && !(Boolean) result) {
                throwValidationException(validateRequest,
                    String.format("커스텀 검증 실패: %s", validatorClass.getSimpleName()));
            }

        } else {
            log.warn("커스텀 검증기에서 validate 메서드를 찾을 수 없습니다: {}", validatorClass.getName());
        }
    }

    /**
     * 검증기 클래스에서 validate 메서드를 찾습니다.
     */
    private Method findValidateMethod(Class<?> validatorClass) {
        try {
            // validate(Object[] args) 메서드 찾기
            return validatorClass.getMethod("validate", Object[].class);
        } catch (NoSuchMethodException e) {
            try {
                // validate(Object... args) 메서드 찾기
                Method[] methods = validatorClass.getMethods();
                for (Method method : methods) {
                    if ("validate".equals(method.getName()) && method.getParameterCount() >= 0) {
                        return method;
                    }
                }
            } catch (Exception ex) {
                log.debug("validate 메서드 찾기 실패: {}", validatorClass.getName(), ex);
            }
        }
        return null;
    }

    /**
     * 검증기 인스턴스를 생성합니다.
     */
    private Object createValidatorInstance(Class<?> validatorClass) throws Exception {
        try {
            // 기본 생성자 시도
            Constructor<?> constructor = validatorClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            // 파라미터가 있는 생성자들 중에서 시도
            Constructor<?>[] constructors = validatorClass.getDeclaredConstructors();
            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterCount() == 0) {
                    constructor.setAccessible(true);
                    return constructor.newInstance();
                }
            }
            throw new RuntimeException("검증기 인스턴스 생성 실패: " + validatorClass.getName());
        }
    }

    /**
     * 검증 예외를 발생시킵니다.
     */
    private void throwValidationException(ValidateRequest validateRequest, String message) {
        String finalMessage = StringUtils.hasText(validateRequest.errorMessage())
            ? validateRequest.errorMessage() + " - " + message
            : message;

        try {
            throw validateRequest.exceptionType()
                .getConstructor(String.class)
                .newInstance(finalMessage);
        } catch (Exception e) {
            // 생성자 호출 실패 시 기본 IllegalArgumentException 사용
            throw new IllegalArgumentException(finalMessage);
        }
    }

    /**
     * 요청 검증기 인터페이스
     */
    public interface RequestValidator {
        /**
         * 요청을 검증합니다.
         * @param args 메서드 파라미터들
         * @return 검증 성공 시 true, 실패 시 false 또는 예외 발생
         */
        boolean validate(Object[] args);
    }
}