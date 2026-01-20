package com.popcorn.common.versioning;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

/**
 * API 버전 정보
 *
 * API 버전의 상세 정보와 상태를 관리
 */
@Getter
@Builder
public class ApiVersionInfo {

    private final String version;               // 버전 번호 (v1, v2, etc.)
    private final String name;                  // 버전 이름
    private final String description;           // 버전 설명
    private final ApiVersionStatus status;      // 버전 상태
    private final LocalDateTime releaseDate;    // 릴리즈 날짜
    private final LocalDateTime deprecatedDate; // deprecated 날짜
    private final LocalDateTime sunsetDate;     // 서비스 종료 예정일
    private final boolean isDefault;            // 기본 버전 여부
    private final String[] supportedOperations; // 지원하는 작업들

    /**
     * API 버전 상태
     */
    @Getter
    public enum ApiVersionStatus {
        BETA("베타"),
        STABLE("안정"),
        DEPRECATED("Deprecated"),
        SUNSET("서비스 종료");

        private final String displayName;

        ApiVersionStatus(String displayName) {
            this.displayName = displayName;
        }
    }

    /**
     * 현재 버전이 사용 가능한지 확인
     */
    public boolean isAvailable() {
        return status != ApiVersionStatus.SUNSET;
    }

    /**
     * 현재 버전이 deprecated인지 확인
     */
    public boolean isDeprecated() {
        return status == ApiVersionStatus.DEPRECATED;
    }

    /**
     * 버전 상태 업데이트
     */
    public ApiVersionInfo withStatus(ApiVersionStatus newStatus) {
        return ApiVersionInfo.builder()
                .version(this.version)
                .name(this.name)
                .description(this.description)
                .status(newStatus)
                .releaseDate(this.releaseDate)
                .deprecatedDate(this.deprecatedDate)
                .sunsetDate(this.sunsetDate)
                .isDefault(this.isDefault)
                .supportedOperations(this.supportedOperations)
                .build();
    }

    /**
     * deprecated 날짜 설정
     */
    public ApiVersionInfo withDeprecatedDate(LocalDateTime deprecatedDate) {
        return ApiVersionInfo.builder()
                .version(this.version)
                .name(this.name)
                .description(this.description)
                .status(ApiVersionStatus.DEPRECATED)
                .releaseDate(this.releaseDate)
                .deprecatedDate(deprecatedDate)
                .sunsetDate(this.sunsetDate)
                .isDefault(this.isDefault)
                .supportedOperations(this.supportedOperations)
                .build();
    }
}
