package com.popcorn.order.dto.idempotency;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 멱등성 키 응답 DTO
 *
 * 멱등성 키의 생성, 조회, 상태 변경 등에 사용되는 응답 데이터입니다.
 * 키의 전체 생명주기 정보를 포함하여 디버깅과 모니터링을 지원합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKeyResponse {

    /** 멱등성 키 고유 식별자 */
    @Schema(description = "멱등성 키", example = "idem-12345678-1234-1234-1234-123456789abc")
    private String key;

    /** 키 생성 시간 */
    @Schema(description = "생성 시간", example = "2024-03-15T10:30:00")
    private LocalDateTime createdAt;

    /** 키 만료 시간 */
    @Schema(description = "만료 시간", example = "2024-03-16T10:30:00")
    private LocalDateTime expiresAt;

    /** 키 상태 */
    @Schema(
        description = "키 상태",
        allowableValues = {"GENERATED", "USED", "EXPIRED", "FAILED"},
        example = "USED"
    )
    private String status;

    /** 키를 생성한 사용자 ID */
    @Schema(description = "생성 사용자 ID", example = "123")
    private Long userId;

    /** 키가 사용된 시간 */
    @Schema(description = "사용 시간", example = "2024-03-15T10:32:15")
    private LocalDateTime usedAt;

    /** 연결된 주문 ID (키가 사용된 경우) */
    @Schema(description = "연결된 주문 ID", example = "12345678-1234-1234-1234-123456789abc")
    private UUID orderId;

    /** 연결된 주문 번호 (키가 사용된 경우) */
    @Schema(description = "연결된 주문 번호", example = "ORD-20240315-001")
    private String orderNo;

    /** 원본 요청 데이터의 해시값 */
    @Schema(description = "요청 데이터 해시", example = "a1b2c3d4e5f6...")
    private String requestHash;

    /** 저장된 응답 데이터 (중복 요청시 반환용) */
    @Schema(description = "저장된 응답 데이터")
    private Map<String, Object> storedResponse;

    /** 키 사용 횟수 (중복 요청 추적) */
    @Schema(description = "사용 횟수", example = "3")
    private Integer usageCount;

    /** 마지막 접근 시간 */
    @Schema(description = "마지막 접근 시간", example = "2024-03-15T10:35:22")
    private LocalDateTime lastAccessedAt;

    /** 클라이언트 IP 주소 */
    @Schema(description = "클라이언트 IP", example = "192.168.1.100")
    private String clientIp;

    /** 클라이언트 User-Agent */
    @Schema(description = "클라이언트 User-Agent", example = "Mozilla/5.0...")
    private String userAgent;

    /** 중복 요청이 차단된 시간들 */
    @Schema(description = "차단된 시간 목록")
    private java.util.List<LocalDateTime> blockedAtTimes;

    /** 오류 메시지 (실패한 경우) */
    @Schema(description = "오류 메시지", example = "네트워크 타임아웃")
    private String errorMessage;

    // ========================= 편의 메서드 =========================

    /**
     * 키가 현재 유효한지 확인
     * @return 만료되지 않았고 사용 가능하면 true
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return expiresAt != null && now.isBefore(expiresAt) &&
               ("GENERATED".equals(status) || "USED".equals(status));
    }

    /**
     * 키가 만료되었는지 확인
     * @return 만료되었으면 true
     */
    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        return expiresAt != null && now.isAfter(expiresAt);
    }

    /**
     * 키가 사용되었는지 확인
     * @return 사용되었으면 true
     */
    public boolean isUsed() {
        return "USED".equals(status) && usedAt != null;
    }

    /**
     * 키 사용까지 걸린 시간 계산 (분 단위)
     * @return 사용 시간 (분), 사용되지 않았으면 null
     */
    public Long getUsageTimeInMinutes() {
        if (createdAt == null || usedAt == null) {
            return null;
        }
        return java.time.Duration.between(createdAt, usedAt).toMinutes();
    }

    /**
     * 중복 요청이 있었는지 확인
     * @return 중복 요청이 차단되었으면 true
     */
    public boolean hasDuplicateRequests() {
        return usageCount != null && usageCount > 1;
    }

    /**
     * 상태별 색상 클래스 반환 (UI 표시용)
     * @return CSS 색상 클래스명
     */
    public String getStatusColorClass() {
        return switch (status) {
            case "USED" -> "success";
            case "GENERATED" -> "info";
            case "EXPIRED" -> "warning";
            case "FAILED" -> "danger";
            default -> "secondary";
        };
    }

    /**
     * 상태별 한글 설명 반환
     * @return 상태 한글 설명
     */
    public String getStatusDescription() {
        return switch (status) {
            case "GENERATED" -> "생성됨";
            case "USED" -> "사용됨";
            case "EXPIRED" -> "만료됨";
            case "FAILED" -> "실패";
            default -> status;
        };
    }

    /**
     * 키가 곧 만료되는지 확인 (1시간 이내)
     * @return 1시간 이내 만료되면 true
     */
    public boolean isExpiringSoon() {
        if (expiresAt == null) return false;
        LocalDateTime oneHourLater = LocalDateTime.now().plusHours(1);
        return expiresAt.isBefore(oneHourLater);
    }

    /**
     * 키 생성 후 경과 시간 계산 (시간 단위)
     * @return 경과 시간 (시간)
     */
    public Long getAgeInHours() {
        if (createdAt == null) return null;
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toHours();
    }

    /**
     * 중복 요청 차단 횟수 반환
     * @return 차단된 중복 요청 수
     */
    public int getBlockedRequestCount() {
        return blockedAtTimes != null ? blockedAtTimes.size() : 0;
    }

}