package com.popcorn.order.dto.idempotency;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 멱등성 키 검증 결과 DTO
 *
 * 멱등성 키의 유효성 검증 결과와 처리 지침을 담습니다.
 * API 요청 처리 시 중복 여부를 판단하고 적절한 응답을 결정하는데 사용됩니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKeyValidationResponse {

    /** 검증 결과 - 키가 유효한지 여부 */
    @Schema(description = "키 유효성", example = "true")
    private boolean valid;

    /** 중복 요청 여부 - 이전에 처리된 요청인지 확인 */
    @Schema(description = "중복 요청 여부", example = "false")
    private boolean duplicateRequest;

    /** 멱등성 키 */
    @Schema(description = "검증된 멱등성 키", example = "idem-12345678-1234-1234-1234-123456789abc")
    private String idempotencyKey;

    /** 요청 고유 식별자 */
    @Schema(description = "요청 ID", example = "req_20240315_001")
    private String requestId;

    /** 요청 사용자 ID */
    @Schema(description = "사용자 ID", example = "12345")
    private Long userId;

    /** 검증 수행 시간 */
    @Schema(description = "검증 시간")
    private LocalDateTime validatedAt;

    /** 검증 결과 코드 */
    @Schema(description = "검증 결과 코드", example = "VALID_NEW_REQUEST")
    private String resultCode;

    /** 검증 결과 메시지 */
    @Schema(description = "검증 결과 메시지", example = "유효한 새 요청입니다")
    private String message;

    /** 이전 요청 처리 결과 (중복 요청인 경우) */
    @Schema(description = "이전 요청 결과 데이터")
    private Object previousResult;

    /** 이전 요청 처리 시간 (중복 요청인 경우) */
    @Schema(description = "이전 요청 처리 시간")
    private LocalDateTime previousRequestTime;

    /** 키 생성 시간 */
    @Schema(description = "키 생성 시간")
    private LocalDateTime keyCreatedAt;

    /** 키 만료 시간 */
    @Schema(description = "키 만료 시간")
    private LocalDateTime keyExpiresAt;

    /** 키 사용 횟수 (이번 요청 포함) */
    @Schema(description = "키 사용 횟수", example = "1")
    private Integer keyUsageCount;

    /** 키 최대 사용 가능 횟수 */
    @Schema(description = "키 최대 사용 횟수", example = "1")
    private Integer keyMaxUsage;

    /** 클라이언트 IP 주소 */
    @Schema(description = "클라이언트 IP", example = "192.168.1.100")
    private String clientIp;

    /** 키 관련 추가 메타데이터 */
    @Schema(description = "추가 메타데이터")
    private Map<String, Object> metadata;

    /** 경고 메시지 목록 */
    @Schema(description = "경고 메시지")
    private java.util.List<String> warnings;

    // ========================= 편의 메서드 =========================

    /**
     * 새로운 요청인지 확인
     * @return 새 요청이면 true
     */
    public boolean isNewRequest() {
        return valid && !duplicateRequest;
    }

    /**
     * 처리 가능한 요청인지 확인
     * @return 처리 가능하면 true
     */
    public boolean canProceed() {
        return valid && !duplicateRequest;
    }

    /**
     * 이전 결과를 반환해야 하는지 확인
     * @return 이전 결과 반환이 필요하면 true
     */
    public boolean shouldReturnPreviousResult() {
        return valid && duplicateRequest && previousResult != null;
    }

    /**
     * 키가 곧 만료되는지 확인 (10분 이내)
     * @return 곧 만료되면 true
     */
    public boolean isKeyExpiringSoon() {
        if (keyExpiresAt == null) return false;
        LocalDateTime tenMinutesLater = LocalDateTime.now().plusMinutes(10);
        return keyExpiresAt.isBefore(tenMinutesLater);
    }

    /**
     * 키 사용률 계산
     * @return 사용률 (0.0 ~ 1.0)
     */
    public double getKeyUsageRatio() {
        if (keyMaxUsage == null || keyMaxUsage == 0) return 0.0;
        if (keyUsageCount == null) return 0.0;
        return Math.min(1.0, (double) keyUsageCount / keyMaxUsage);
    }

    /**
     * 남은 키 사용 가능 횟수
     * @return 남은 횟수
     */
    public int getRemainingUsage() {
        if (keyMaxUsage == null || keyUsageCount == null) return 0;
        return Math.max(0, keyMaxUsage - keyUsageCount);
    }

    /**
     * 중복 요청 대기 시간 계산 (초 단위)
     * @return 이전 요청 후 경과 시간 (초)
     */
    public long getDuplicateRequestDelaySeconds() {
        if (!duplicateRequest || previousRequestTime == null) return 0;
        return java.time.Duration.between(previousRequestTime, LocalDateTime.now()).getSeconds();
    }

    /**
     * 경고가 있는지 확인
     * @return 경고가 있으면 true
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    /**
     * 검증 결과 요약
     * @return 검증 결과 요약 문자열
     */
    public String getSummary() {
        if (!valid) {
            return "유효하지 않은 키: " + message;
        }
        if (duplicateRequest) {
            return "중복 요청 (이전 처리: " + previousRequestTime + ")";
        }
        return "새로운 유효한 요청";
    }

    // ========================= 정적 팩토리 메서드 =========================

    /**
     * 유효한 새 요청 결과 생성
     */
    public static IdempotencyKeyValidationResponse validNewRequest(
            String idempotencyKey, String requestId, Long userId) {
        return IdempotencyKeyValidationResponse.builder()
                .valid(true)
                .duplicateRequest(false)
                .idempotencyKey(idempotencyKey)
                .requestId(requestId)
                .userId(userId)
                .validatedAt(LocalDateTime.now())
                .resultCode("VALID_NEW_REQUEST")
                .message("유효한 새 요청입니다")
                .keyUsageCount(1)
                .build();
    }

    /**
     * 중복 요청 결과 생성
     */
    public static IdempotencyKeyValidationResponse duplicateRequest(
            String idempotencyKey, String requestId, Long userId,
            Object previousResult, LocalDateTime previousTime) {
        return IdempotencyKeyValidationResponse.builder()
                .valid(true)
                .duplicateRequest(true)
                .idempotencyKey(idempotencyKey)
                .requestId(requestId)
                .userId(userId)
                .validatedAt(LocalDateTime.now())
                .resultCode("DUPLICATE_REQUEST")
                .message("중복 요청 - 이전 결과를 반환합니다")
                .previousResult(previousResult)
                .previousRequestTime(previousTime)
                .build();
    }

    /**
     * 유효하지 않은 키 결과 생성
     */
    public static IdempotencyKeyValidationResponse invalidKey(
            String idempotencyKey, String requestId, String reason) {
        return IdempotencyKeyValidationResponse.builder()
                .valid(false)
                .duplicateRequest(false)
                .idempotencyKey(idempotencyKey)
                .requestId(requestId)
                .validatedAt(LocalDateTime.now())
                .resultCode("INVALID_KEY")
                .message("유효하지 않은 키: " + reason)
                .build();
    }

}