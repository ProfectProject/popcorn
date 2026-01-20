package com.popcorn.common.aop;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

import com.popcorn.common.annotation.AuditLog;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * @AuditLog 어노테이션을 처리하는 AOP 어드바이스
 *
 * 메서드 실행에 대한 감사 로그를 기록합니다.
 */
@Aspect
@Component
@Order(20) // 낮은 우선순위 (다른 처리 후 실행)
@RequiredArgsConstructor
public class AuditLogAspect {

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

    private final ObjectMapper objectMapper;
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * @AuditLog 어노테이션이 적용된 메서드를 인터셉트합니다.
     */
    @Around("@annotation(auditLog)")
    public Object handleAuditLog(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {

        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();

        // 감사 로그 컨텍스트 생성
        AuditContext context = createAuditContext(joinPoint, auditLog);

        Exception executionException = null;
        Object result = null;

        try {
            result = joinPoint.proceed();
            context.setSuccess(true);
            context.setResult(result);

            return result;

        } catch (Exception e) {
            executionException = e;
            context.setSuccess(false);
            context.setException(e);
            throw e;

        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            context.setExecutionTime(executionTime);

            // 감사 로그 기록 조건 확인
            if (shouldLogAudit(auditLog, context)) {
                logAuditEvent(context, auditLog);
            }
        }
    }

    /**
     * 감사 로그 컨텍스트를 생성합니다.
     */
    private AuditContext createAuditContext(ProceedingJoinPoint joinPoint, AuditLog auditLog) {
        AuditContext context = new AuditContext();

        context.setAction(auditLog.action());
        context.setResource(auditLog.resource());
        context.setDescription(auditLog.description());
        context.setMethodName(joinPoint.getSignature().toShortString());
        context.setTimestamp(System.currentTimeMillis());

        // 사용자 ID 추출
        String userId = extractUserId(joinPoint, auditLog);
        context.setUserId(userId);

        // 리소스 ID 추출
        String resourceId = extractResourceId(joinPoint, auditLog);
        context.setResourceId(resourceId);

        // 요청 데이터 수집
        if (auditLog.includeRequestData()) {
            Map<String, Object> requestData = collectRequestData(joinPoint, auditLog);
            context.setRequestData(requestData);
        }

        return context;
    }

    /**
     * 사용자 ID를 추출합니다.
     */
    private String extractUserId(ProceedingJoinPoint joinPoint, AuditLog auditLog) {
        if (!StringUtils.hasText(auditLog.userIdExpression())) {
            return "unknown";
        }

        try {
            Object userId = evaluateSpelExpression(joinPoint, null, auditLog.userIdExpression());
            return userId != null ? userId.toString() : "unknown";
        } catch (Exception e) {
            auditLogger.warn("사용자 ID 추출 실패: {}", auditLog.userIdExpression(), e);
            return "unknown";
        }
    }

    /**
     * 리소스 ID를 추출합니다.
     */
    private String extractResourceId(ProceedingJoinPoint joinPoint, AuditLog auditLog) {

        // 정적 리소스 ID 우선
        if (StringUtils.hasText(auditLog.staticResourceId())) {
            return auditLog.staticResourceId();
        }

        // SpEL 표현식 평가
        if (StringUtils.hasText(auditLog.resourceIdExpression())) {
            try {
                Object resourceId = evaluateSpelExpression(joinPoint, null, auditLog.resourceIdExpression());
                return resourceId != null ? resourceId.toString() : "unknown";
            } catch (Exception e) {
                auditLogger.warn("리소스 ID 추출 실패: {}", auditLog.resourceIdExpression(), e);
            }
        }

        return "unknown";
    }

    /**
     * 요청 데이터를 수집합니다.
     */
    private Map<String, Object> collectRequestData(ProceedingJoinPoint joinPoint, AuditLog auditLog) {
        Map<String, Object> requestData = new HashMap<>();

        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            List<String> excludeParams = Arrays.asList(auditLog.excludeParams());

            for (int i = 0; i < paramNames.length; i++) {
                String paramName = paramNames[i];

                if (excludeParams.contains(paramName)) {
                    requestData.put(paramName, "***EXCLUDED***");
                } else if (!auditLog.includeSensitiveData() && isSensitiveParam(paramName)) {
                    requestData.put(paramName, "***SENSITIVE***");
                } else {
                    requestData.put(paramName, args[i]);
                }
            }

        } catch (Exception e) {
            auditLogger.warn("요청 데이터 수집 실패", e);
            requestData.put("error", "데이터 수집 실패: " + e.getMessage());
        }

        return requestData;
    }

    /**
     * 감사 로그 기록 여부를 결정합니다.
     */
    private boolean shouldLogAudit(AuditLog auditLog, AuditContext context) {
        // 성공 시에만 로깅하는 설정이고 실패한 경우
        return !auditLog.onSuccessOnly() || context.isSuccess();
    }

    /**
     * 감사 로그를 기록합니다.
     */
    private void logAuditEvent(AuditContext context, AuditLog auditLog) {
        try {
            Map<String, Object> auditEvent = new HashMap<>();

            auditEvent.put("timestamp", context.getTimestamp());
            auditEvent.put("userId", context.getUserId());
            auditEvent.put("action", context.getAction());
            auditEvent.put("resource", context.getResource());
            auditEvent.put("resourceId", context.getResourceId());
            auditEvent.put("method", context.getMethodName());
            auditEvent.put("success", context.isSuccess());
            auditEvent.put("executionTime", context.getExecutionTime());

            if (StringUtils.hasText(context.getDescription())) {
                auditEvent.put("description", context.getDescription());
            }

            if (context.getRequestData() != null) {
                auditEvent.put("requestData", context.getRequestData());
            }

            if (auditLog.includeResponseData() && context.getResult() != null) {
                try {
                    auditEvent.put("responseData", context.getResult());
                } catch (Exception e) {
                    auditEvent.put("responseData", "***SERIALIZATION_ERROR***");
                }
            }

            if (!context.isSuccess() && context.getException() != null) {
                auditEvent.put("exception", context.getException().getClass().getSimpleName());
                auditEvent.put("exceptionMessage", context.getException().getMessage());
            }

            String auditMessage = objectMapper.writeValueAsString(auditEvent);

            // 레벨에 따른 로깅
            logWithLevel(auditLog.level(), auditMessage);

        } catch (Exception e) {
            auditLogger.error("감사 로그 기록 실패: action={}, resource={}, resourceId={}",
                             context.getAction(), context.getResource(), context.getResourceId(), e);
        }
    }

    /**
     * SpEL 표현식을 평가합니다.
     */
    private Object evaluateSpelExpression(ProceedingJoinPoint joinPoint, Object result, String expression) {
        EvaluationContext context = new StandardEvaluationContext();

        // 메서드 파라미터 추가
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        // 결과 추가
        if (result != null) {
            context.setVariable("result", result);
        }

        return expressionParser.parseExpression(expression).getValue(context);
    }

    /**
     * 민감한 파라미터인지 확인합니다.
     */
    private boolean isSensitiveParam(String paramName) {
        String lowerCaseName = paramName.toLowerCase();
        return lowerCaseName.contains("password") ||
               lowerCaseName.contains("token") ||
               lowerCaseName.contains("secret") ||
               lowerCaseName.contains("key") ||
               lowerCaseName.contains("credential");
    }

    /**
     * 지정된 레벨로 로깅합니다.
     */
    private void logWithLevel(AuditLog.Level level, String message) {
        switch (level) {
            case DEBUG:
                if (auditLogger.isDebugEnabled()) auditLogger.debug(message);
                break;
            case INFO:
                auditLogger.info(message);
                break;
            case WARN:
                auditLogger.warn(message);
                break;
            case ERROR:
                auditLogger.error(message);
                break;
        }
    }

    /**
     * 감사 로그 컨텍스트 클래스
     */
    private static class AuditContext {
        private String action;
        private String resource;
        private String resourceId;
        private String description;
        private String methodName;
        private String userId;
        private long timestamp;
        private long executionTime;
        private boolean success;
        private Map<String, Object> requestData;
        private Object result;
        private Exception exception;

        // Getters and Setters
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }

        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public long getExecutionTime() { return executionTime; }
        public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public Map<String, Object> getRequestData() { return requestData; }
        public void setRequestData(Map<String, Object> requestData) { this.requestData = requestData; }

        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; }

        public Exception getException() { return exception; }
        public void setException(Exception exception) { this.exception = exception; }
    }
}