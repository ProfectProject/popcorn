package com.popcorn.common.versioning;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * API 버전 관리 서비스
 *
 * 기능:
 * - API 버전 등록 및 관리
 * - 요청으로부터 버전 추출
 * - 버전 호환성 검증
 * - 버전 라이프사이클 관리
 */
@Service
public class ApiVersionManager {

    private static final Logger log = LoggerFactory.getLogger(ApiVersionManager.class);

    private static final String DEFAULT_VERSION = "v1";
    private static final String VERSION_HEADER = "API-Version";
    private static final String VERSION_PARAM = "version";

    // 등록된 API 버전들
    private final Map<String, ApiVersionInfo> registeredVersions = new ConcurrentHashMap<>();

    public ApiVersionManager() {
        initializeDefaultVersions();
    }

    /**
     * 기본 버전들 초기화
     */
    private void initializeDefaultVersions() {
        // v1 - 안정 버전
        registerVersion(ApiVersionInfo.builder()
                .version("v1")
                .name("Order API v1")
                .description("주문 API 첫 번째 안정 버전")
                .status(ApiVersionInfo.ApiVersionStatus.STABLE)
                .releaseDate(LocalDateTime.now().minusMonths(6))
                .isDefault(true)
                .supportedOperations(new String[]{"orders", "status", "items"})
                .build());

        log.info("🔄 API 버전 초기화 완료 - 등록된 버전: {}", registeredVersions.keySet());
    }

    /**
     * 새 API 버전 등록
     */
    public void registerVersion(ApiVersionInfo versionInfo) {
        registeredVersions.put(versionInfo.getVersion(), versionInfo);
        log.info("📝 API 버전 등록: {} - {}", versionInfo.getVersion(), versionInfo.getName());
    }

    /**
     * HTTP 요청으로부터 API 버전 추출
     *
     * 우선순위:
     * 1. Accept 헤더의 version 파라미터
     * 2. API-Version 헤더
     * 3. URL 경로의 버전 세그먼트
     * 4. 쿼리 파라미터 version
     * 5. 기본 버전
     */
    public String extractVersionFromRequest(HttpServletRequest request) {
        // 1. Accept 헤더에서 버전 추출
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("version=")) {
            String version = extractVersionFromAcceptHeader(acceptHeader);
            if (version != null) {
                log.debug("🔍 Accept 헤더에서 버전 추출: {}", version);
                return version;
            }
        }

        // 2. API-Version 헤더
        String versionHeader = request.getHeader(VERSION_HEADER);
        if (versionHeader != null && !versionHeader.trim().isEmpty()) {
            String normalizedVersion = normalizeVersion(versionHeader);
            log.debug("🔍 API-Version 헤더에서 버전 추출: {}", normalizedVersion);
            return normalizedVersion;
        }

        // 3. URL 경로에서 버전 추출
        String pathVersion = extractVersionFromPath(request.getRequestURI());
        if (pathVersion != null) {
            log.debug("🔍 URL 경로에서 버전 추출: {}", pathVersion);
            return pathVersion;
        }

        // 4. 쿼리 파라미터
        String paramVersion = request.getParameter(VERSION_PARAM);
        if (paramVersion != null && !paramVersion.trim().isEmpty()) {
            String normalizedVersion = normalizeVersion(paramVersion);
            log.debug("🔍 쿼리 파라미터에서 버전 추출: {}", normalizedVersion);
            return normalizedVersion;
        }

        // 5. 기본 버전
        log.debug("🔍 기본 버전 사용: {}", DEFAULT_VERSION);
        return DEFAULT_VERSION;
    }

    /**
     * 버전 정보 조회
     */
    public Optional<ApiVersionInfo> getVersionInfo(String version) {
        return Optional.ofNullable(registeredVersions.get(normalizeVersion(version)));
    }

    /**
     * 모든 등록된 버전 조회
     */
    public List<ApiVersionInfo> getAllVersions() {
        return registeredVersions.values()
                .stream()
                .sorted(Comparator.comparing(ApiVersionInfo::getReleaseDate).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 사용 가능한 버전들만 조회
     */
    public List<ApiVersionInfo> getAvailableVersions() {
        return registeredVersions.values()
                .stream()
                .filter(ApiVersionInfo::isAvailable)
                .sorted(Comparator.comparing(ApiVersionInfo::getReleaseDate).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 버전 호환성 확인
     */
    public boolean isVersionSupported(String version, String[] supportedVersions) {
        String normalizedVersion = normalizeVersion(version);
        return Arrays.asList(supportedVersions).contains(normalizedVersion);
    }

    /**
     * 최신 안정 버전 조회
     */
    public String getLatestStableVersion() {
        return registeredVersions.values()
                .stream()
                .filter(v -> v.getStatus() == ApiVersionInfo.ApiVersionStatus.STABLE)
                .max(Comparator.comparing(ApiVersionInfo::getReleaseDate))
                .map(ApiVersionInfo::getVersion)
                .orElse(DEFAULT_VERSION);
    }

    /**
     * 기본 버전 조회
     */
    public String getDefaultVersion() {
        return registeredVersions.values()
                .stream()
                .filter(ApiVersionInfo::isDefault)
                .findFirst()
                .map(ApiVersionInfo::getVersion)
                .orElse(DEFAULT_VERSION);
    }

    /**
     * 버전 상태 업데이트
     */
    public void updateVersionStatus(String version, ApiVersionInfo.ApiVersionStatus status) {
        ApiVersionInfo versionInfo = registeredVersions.get(version);
        if (versionInfo != null) {
            registeredVersions.put(version, versionInfo.withStatus(status));
            log.info("📝 버전 상태 업데이트: {} → {}", version, status.getDisplayName());
        }
    }

    /**
     * 버전 deprecated 처리
     */
    public void deprecateVersion(String version, LocalDateTime deprecatedDate) {
        ApiVersionInfo versionInfo = registeredVersions.get(version);
        if (versionInfo != null) {
            registeredVersions.put(version, versionInfo.withDeprecatedDate(deprecatedDate));
            log.warn("⚠️ 버전 Deprecated 처리: {} - 날짜: {}", version, deprecatedDate);
        }
    }

    /**
     * 버전 호환성 보고서 생성
     */
    public VersionCompatibilityReport generateCompatibilityReport() {
        return VersionCompatibilityReport.builder()
                .totalVersions(registeredVersions.size())
                .stableVersions(countVersionsByStatus(ApiVersionInfo.ApiVersionStatus.STABLE))
                .betaVersions(countVersionsByStatus(ApiVersionInfo.ApiVersionStatus.BETA))
                .deprecatedVersions(countVersionsByStatus(ApiVersionInfo.ApiVersionStatus.DEPRECATED))
                .sunsetVersions(countVersionsByStatus(ApiVersionInfo.ApiVersionStatus.SUNSET))
                .defaultVersion(getDefaultVersion())
                .latestStableVersion(getLatestStableVersion())
                .versionDetails(getAllVersions())
                .build();
    }

    // ================ 내부 헬퍼 메서드들 ================

    private String extractVersionFromAcceptHeader(String acceptHeader) {
        // application/json;version=v2 형태에서 version 추출
        String[] parts = acceptHeader.split(";");
        for (String part : parts) {
            if (part.trim().startsWith("version=")) {
                return normalizeVersion(part.trim().substring(8));
            }
        }
        return null;
    }

    private String extractVersionFromPath(String path) {
        // /api/v1/orders 또는 /api/v2/orders 형태에서 버전 추출
        String[] segments = path.split("/");
        for (String segment : segments) {
            if (segment.matches("v\\d+")) {
                return segment;
            }
        }
        return null;
    }

    private String normalizeVersion(String version) {
        if (version == null) return DEFAULT_VERSION;

        String trimmed = version.trim();
        if (trimmed.isEmpty()) return DEFAULT_VERSION;

        // v1, V1, 1 모두 v1으로 정규화
        if (trimmed.matches("\\d+")) {
            return "v" + trimmed;
        }

        return trimmed.toLowerCase();
    }

    private long countVersionsByStatus(ApiVersionInfo.ApiVersionStatus status) {
        return registeredVersions.values()
                .stream()
                .filter(v -> v.getStatus() == status)
                .count();
    }

    // ================ 내부 클래스 ================

    /**
     * 버전 호환성 보고서
     */
    @lombok.Builder
    @lombok.Getter
    public static class VersionCompatibilityReport {
        private final int totalVersions;
        private final long stableVersions;
        private final long betaVersions;
        private final long deprecatedVersions;
        private final long sunsetVersions;
        private final String defaultVersion;
        private final String latestStableVersion;
        private final List<ApiVersionInfo> versionDetails;

        public String generateSummary() {
            return String.format("""
                    🔄 API 버전 호환성 보고서
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    📊 버전 현황
                       - 총 버전: %d개
                       - 안정 버전: %d개
                       - 베타 버전: %d개
                       - Deprecated: %d개
                       - 종료됨: %d개

                    🎯 주요 정보
                       - 기본 버전: %s
                       - 최신 안정 버전: %s

                    📋 버전 목록
                    %s
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    """,
                    totalVersions, stableVersions, betaVersions,
                    deprecatedVersions, sunsetVersions,
                    defaultVersion, latestStableVersion,
                    generateVersionList()
            );
        }

        private String generateVersionList() {
            return versionDetails.stream()
                    .map(v -> String.format("       - %s (%s): %s",
                            v.getVersion(), v.getStatus().getDisplayName(), v.getName()))
                    .collect(Collectors.joining("\n"));
        }
    }
}
