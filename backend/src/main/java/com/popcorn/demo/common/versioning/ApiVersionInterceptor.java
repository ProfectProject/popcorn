package com.popcorn.demo.common.versioning;

import java.lang.reflect.Method;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * API 버전 검증 인터셉터
 *
 * 모든 API 요청에서 버전 정보를 검증하고 적절한 처리를 수행
 * - 요청 버전 추출 및 검증
 * - 지원되지 않는 버전에 대한 오류 처리
 * - Deprecated 버전에 대한 경고 헤더 추가
 * - 버전별 접근 로그
 */
@Component
@RequiredArgsConstructor
public class ApiVersionInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiVersionInterceptor.class);

    private final ApiVersionManager versionManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                            Object handler) throws Exception {

        // Handler 메서드가 아닌 경우 패스
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 요청에서 API 버전 추출
        String requestedVersion = versionManager.extractVersionFromRequest(request);
        request.setAttribute("api.version", requestedVersion);

        // 메서드/클래스의 @ApiVersion 어노테이션 확인
        ApiVersionConfig versionConfig = extractVersionConfig(handlerMethod);
        if (versionConfig == null) {
            // 버전 어노테이션이 없는 경우 기본 버전으로 처리
            log.debug("🔧 버전 어노테이션 없음 - 기본 버전 적용: {}", requestedVersion);
            return true;
        }

        // 버전 호환성 검증
        VersionValidationResult validationResult = validateVersion(
            requestedVersion, versionConfig, request.getRequestURI());

        if (!validationResult.isValid()) {
            sendVersionError(response, validationResult);
            return false;
        }

        // Deprecated 버전 경고 헤더 추가
        if (validationResult.isDeprecated()) {
            addDeprecationWarning(response, requestedVersion, versionConfig.getDeprecatedInVersions());
        }

        // 버전 정보를 응답 헤더에 추가
        response.setHeader("API-Version-Used", requestedVersion);

        // 접근 로그 기록
        log.info("🔄 API 버전 처리 - URI: {}, 요청버전: {}, 지원버전: {}",
                request.getRequestURI(), requestedVersion, String.join(",", versionConfig.getSupportedVersions()));

        return true;
    }

    /**
     * 핸들러 메서드에서 @ApiVersion 설정 추출
     */
    private ApiVersionConfig extractVersionConfig(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        Class<?> controllerClass = handlerMethod.getBeanType();

        // 메서드 레벨 어노테이션 우선 확인
        ApiVersion methodAnnotation = method.getAnnotation(ApiVersion.class);
        if (methodAnnotation != null) {
            return ApiVersionConfig.fromAnnotation(methodAnnotation);
        }

        // 클래스 레벨 어노테이션 확인
        ApiVersion classAnnotation = controllerClass.getAnnotation(ApiVersion.class);
        if (classAnnotation != null) {
            return ApiVersionConfig.fromAnnotation(classAnnotation);
        }

        return null;
    }

    /**
     * 버전 검증 수행
     */
    private VersionValidationResult validateVersion(String requestedVersion,
                                                   ApiVersionConfig config,
                                                   String requestUri) {

        // 1. 지원 버전 확인
        boolean isSupported = versionManager.isVersionSupported(requestedVersion, config.getSupportedVersions());
        if (!isSupported) {
            return VersionValidationResult.unsupported(requestedVersion, config.getSupportedVersions());
        }

        // 2. 제거된 버전 확인
        if (config.isRemovedVersion(requestedVersion)) {
            return VersionValidationResult.removed(requestedVersion, config.getRemovedInVersions());
        }

        // 3. 등록된 버전 정보 확인
        Optional<ApiVersionInfo> versionInfo = versionManager.getVersionInfo(requestedVersion);
        if (versionInfo.isPresent() && !versionInfo.get().isAvailable()) {
            return VersionValidationResult.unavailable(requestedVersion, versionInfo.get().getStatus());
        }

        // 4. Deprecated 확인
        boolean isDeprecated = config.isDeprecatedVersion(requestedVersion) ||
                              versionInfo.map(ApiVersionInfo::isDeprecated).orElse(false);

        return VersionValidationResult.valid(requestedVersion, isDeprecated);
    }

    /**
     * 버전 오류 응답 전송
     */
    private void sendVersionError(HttpServletResponse response, VersionValidationResult result) throws Exception {
        response.setStatus(result.getHttpStatus());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("API-Version-Error", result.getErrorType());

        String errorJson = String.format("""
            {
                "error": "API_VERSION_ERROR",
                "code": "%s",
                "message": "%s",
                "requestedVersion": "%s",
                "supportedVersions": [%s],
                "timestamp": "%s"
            }""",
            result.getErrorCode(),
            result.getErrorMessage(),
            result.getRequestedVersion(),
            result.formatSupportedVersions(),
            java.time.LocalDateTime.now()
        );

        response.getWriter().write(errorJson);

        log.warn("⚠️ API 버전 오류 - {}: {}", result.getErrorType(), result.getErrorMessage());
    }

    /**
     * Deprecated 경고 헤더 추가
     */
    private void addDeprecationWarning(HttpServletResponse response, String version, String deprecatedIn) {
        response.setHeader("API-Deprecation-Warning",
            String.format("Version %s is deprecated since %s. Please migrate to a newer version.",
                         version, deprecatedIn));
        response.setHeader("Sunset", "Tue, 31 Dec 2024 23:59:59 GMT"); // 예시 종료일

        log.warn("⚠️ Deprecated API 사용 - 버전: {}, 폐기예정: {}", version, deprecatedIn);
    }

    // ================ 내부 클래스들 ================

    /**
     * API 버전 설정
     */
    private static class ApiVersionConfig {
        private final String[] supportedVersions;
        private final String deprecatedInVersions;
        private final String removedInVersions;

        public ApiVersionConfig(String[] supportedVersions, String deprecatedInVersions, String removedInVersions) {
            this.supportedVersions = supportedVersions;
            this.deprecatedInVersions = deprecatedInVersions;
            this.removedInVersions = removedInVersions;
        }

        public static ApiVersionConfig fromAnnotation(ApiVersion annotation) {
            return new ApiVersionConfig(
                annotation.value(),
                annotation.deprecatedInVersions(),
                annotation.removedInVersions()
            );
        }

        public String[] getSupportedVersions() { return supportedVersions; }
        public String getDeprecatedInVersions() { return deprecatedInVersions; }
        public String getRemovedInVersions() { return removedInVersions; }

        public boolean isDeprecatedVersion(String version) {
            return !deprecatedInVersions.isEmpty() &&
                   compareVersions(version, deprecatedInVersions) >= 0;
        }

        public boolean isRemovedVersion(String version) {
            return !removedInVersions.isEmpty() &&
                   compareVersions(version, removedInVersions) >= 0;
        }

        private int compareVersions(String v1, String v2) {
            // 간단한 버전 비교 (v1, v2 형태)
            String n1 = v1.replaceAll("v", "");
            String n2 = v2.replaceAll("v", "");

            try {
                return Integer.compare(Integer.parseInt(n1), Integer.parseInt(n2));
            } catch (NumberFormatException e) {
                return v1.compareTo(v2);
            }
        }
    }

    /**
     * 버전 검증 결과
     */
    private static class VersionValidationResult {
        private final boolean valid;
        private final boolean deprecated;
        private final String requestedVersion;
        private final String errorType;
        private final String errorCode;
        private final String errorMessage;
        private final String[] supportedVersions;
        private final int httpStatus;

        private VersionValidationResult(boolean valid, boolean deprecated, String requestedVersion,
                                       String errorType, String errorCode, String errorMessage,
                                       String[] supportedVersions, int httpStatus) {
            this.valid = valid;
            this.deprecated = deprecated;
            this.requestedVersion = requestedVersion;
            this.errorType = errorType;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.supportedVersions = supportedVersions;
            this.httpStatus = httpStatus;
        }

        public static VersionValidationResult valid(String version, boolean deprecated) {
            return new VersionValidationResult(true, deprecated, version, null, null, null, null, 200);
        }

        public static VersionValidationResult unsupported(String version, String[] supported) {
            return new VersionValidationResult(false, false, version,
                "UNSUPPORTED_VERSION", "VERSION_NOT_SUPPORTED",
                String.format("API version '%s' is not supported. Supported versions: %s",
                             version, String.join(", ", supported)),
                supported, 400);
        }

        public static VersionValidationResult removed(String version, String removedIn) {
            return new VersionValidationResult(false, false, version,
                "REMOVED_VERSION", "VERSION_REMOVED",
                String.format("API version '%s' has been removed since version %s", version, removedIn),
                null, 410);
        }

        public static VersionValidationResult unavailable(String version, ApiVersionInfo.ApiVersionStatus status) {
            return new VersionValidationResult(false, false, version,
                "UNAVAILABLE_VERSION", "VERSION_UNAVAILABLE",
                String.format("API version '%s' is currently unavailable (status: %s)", version, status.getDisplayName()),
                null, 503);
        }

        // Getters
        public boolean isValid() { return valid; }
        public boolean isDeprecated() { return deprecated; }
        public String getRequestedVersion() { return requestedVersion; }
        public String getErrorType() { return errorType; }
        public String getErrorCode() { return errorCode; }
        public String getErrorMessage() { return errorMessage; }
        public int getHttpStatus() { return httpStatus; }

        public String formatSupportedVersions() {
            if (supportedVersions == null) return "";
            return String.join("\", \"", supportedVersions);
        }
    }
}