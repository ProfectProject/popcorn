package com.popcorn.order.dto.payment;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 검증된 보안 결제 토큰 DTO
 *
 * JWT 토큰에서 추출하고 검증된 결제 정보를 담습니다.
 * 결제 프로세스에서 신뢰할 수 있는 데이터로 사용됩니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurePaymentToken {

    /** 토큰 ID (고유 식별자) */
    private String tokenId;

    /** 주문 ID */
    private UUID orderId;

    /** 주문 번호 */
    private String orderNo;

    /** 사용자 ID */
    private Long userId;

    /** 결제 금액 */
    private Long amount;

    /** 결제 방식 */
    private String paymentMethod;

    /** 토큰 발행 시간 */
    private LocalDateTime issuedAt;

    /** 토큰 만료 시간 */
    private LocalDateTime expiresAt;

    /** 토큰 발행자 */
    private String issuer;

    /** 토큰 대상 (audience) */
    private String audience;

    /** 토큰 주제 (subject) */
    private String subject;

    /** JWT ID (JTI) */
    private String jwtId;

    /** 커스텀 클레임 */
    private Map<String, Object> customClaims;

    /** 토큰 상태 (ACTIVE, EXPIRED, REVOKED) */
    private String status;

    /** 토큰 사용 횟수 */
    private Integer usageCount;

    /** 마지막 사용 시간 */
    private LocalDateTime lastUsedAt;

    /** 클라이언트 IP 주소 (토큰 생성 시) */
    private String clientIp;

    /** 사용자 에이전트 정보 */
    private String userAgent;

    // ========================= 검증 메서드 =========================

    /**
     * 토큰이 유효한지 확인
     * @return 유효하면 true
     */
    public boolean isValid() {
        return "ACTIVE".equals(status) && !isExpired() && hasRequiredFields();
    }

    /**
     * 토큰이 만료되었는지 확인
     * @return 만료되었으면 true
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 필수 필드가 모두 있는지 확인
     * @return 필수 필드가 모두 있으면 true
     */
    public boolean hasRequiredFields() {
        return orderId != null && userId != null && amount != null && amount > 0;
    }

    /**
     * 토큰이 일회용인지 확인
     * @return 일회용이면 true
     */
    public boolean isOneTimeUse() {
        Boolean oneTime = (Boolean) getCustomClaimValue("oneTimeUse");
        return Boolean.TRUE.equals(oneTime);
    }

    /**
     * 토큰이 이미 사용되었는지 확인
     * @return 사용되었으면 true
     */
    public boolean isUsed() {
        return usageCount != null && usageCount > 0;
    }

    /**
     * 토큰이 폐기되었는지 확인
     * @return 폐기되었으면 true
     */
    public boolean isRevoked() {
        return "REVOKED".equals(status);
    }

    /**
     * 특정 IP에서만 사용 가능한지 확인
     * @param requestIp 요청 IP
     * @return IP 제한이 있고 일치하지 않으면 false
     */
    public boolean isValidForIp(String requestIp) {
        if (clientIp == null) return true; // IP 제한 없음
        return clientIp.equals(requestIp);
    }

    /**
     * 남은 유효 시간 계산 (초 단위)
     * @return 남은 시간 (초)
     */
    public long getRemainingSeconds() {
        if (isExpired()) return 0;
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
    }

    /**
     * 토큰 수명 계산 (분 단위)
     * @return 토큰의 전체 수명 (분)
     */
    public long getTotalLifetimeMinutes() {
        if (issuedAt == null || expiresAt == null) return 0;
        return java.time.Duration.between(issuedAt, expiresAt).toMinutes();
    }

    // ========================= 커스텀 클레임 접근 =========================

    /**
     * 커스텀 클레임 값 조회
     * @param claimName 클레임 이름
     * @return 클레임 값 (없으면 null)
     */
    public Object getCustomClaimValue(String claimName) {
        return customClaims != null ? customClaims.get(claimName) : null;
    }

    /**
     * 문자열 커스텀 클레임 조회
     * @param claimName 클레임 이름
     * @return 문자열 값 (없거나 타입이 다르면 null)
     */
    public String getStringClaim(String claimName) {
        Object value = getCustomClaimValue(claimName);
        return value instanceof String ? (String) value : null;
    }

    /**
     * 숫자 커스텀 클레임 조회
     * @param claimName 클레임 이름
     * @return 숫자 값 (없거나 타입이 다르면 null)
     */
    public Long getLongClaim(String claimName) {
        Object value = getCustomClaimValue(claimName);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    /**
     * 불린 커스텀 클레임 조회
     * @param claimName 클레임 이름
     * @return 불린 값 (없거나 타입이 다르면 false)
     */
    public boolean getBooleanClaim(String claimName) {
        Object value = getCustomClaimValue(claimName);
        return Boolean.TRUE.equals(value);
    }

    // ========================= 보안 및 감사 =========================

    /**
     * 토큰 사용 기록
     * @param currentIp 현재 사용 IP
     */
    public void recordUsage(String currentIp) {
        this.usageCount = (this.usageCount != null ? this.usageCount : 0) + 1;
        this.lastUsedAt = LocalDateTime.now();

        // IP 변경 감지
        if (clientIp != null && !clientIp.equals(currentIp)) {
            // 보안 로그 기록 필요
        }
    }

    /**
     * 토큰 정보 요약
     * @return 주요 정보를 담은 요약 문자열
     */
    public String getSummary() {
        return String.format("Token[주문=%s, 사용자=%d, 금액=%,d원, 상태=%s, 만료=%s]",
                orderNo, userId, amount, status,
                expiresAt != null ? expiresAt.toString() : "무제한");
    }

    /**
     * 보안 감사용 정보
     * @return 보안 관련 주요 정보
     */
    public Map<String, Object> getSecurityAuditInfo() {
        return Map.of(
                "tokenId", tokenId,
                "orderId", orderId,
                "userId", userId,
                "issuedAt", issuedAt,
                "expiresAt", expiresAt,
                "usageCount", usageCount != null ? usageCount : 0,
                "status", status,
                "clientIp", clientIp != null ? clientIp : "unknown"
        );
    }

    // ========================= 팩토리 메서드 =========================

    /**
     * 만료된 토큰 생성 (테스트용)
     */
    public static SecurePaymentToken createExpiredToken(UUID orderId, Long userId) {
        return SecurePaymentToken.builder()
                .tokenId("EXPIRED-" + UUID.randomUUID().toString().substring(0, 8))
                .orderId(orderId)
                .userId(userId)
                .amount(0L)
                .issuedAt(LocalDateTime.now().minusHours(2))
                .expiresAt(LocalDateTime.now().minusHours(1))
                .status("EXPIRED")
                .build();
    }

    /**
     * 폐기된 토큰 생성 (테스트용)
     */
    public static SecurePaymentToken createRevokedToken(UUID orderId, Long userId) {
        return SecurePaymentToken.builder()
                .tokenId("REVOKED-" + UUID.randomUUID().toString().substring(0, 8))
                .orderId(orderId)
                .userId(userId)
                .amount(0L)
                .issuedAt(LocalDateTime.now().minusMinutes(30))
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .status("REVOKED")
                .build();
    }

}