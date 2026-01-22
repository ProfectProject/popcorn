package com.popcorn.order.service;

import java.time.LocalDateTime;
import java.util.UUID;

import com.popcorn.order.dto.payment.PaymentUrlResponse;
import com.popcorn.order.dto.payment.SecurePaymentToken;

/**
 * 결제 URL 보안 서비스 인터페이스
 *
 * JWT를 사용한 안전한 결제 URL 생성과 검증을 담당합니다.
 * 결제 URL이 변조되지 않고 유효한 기간 내에만 사용되도록 보장합니다.
 *
 * JWT 토큰에 포함되는 정보:
 * - 주문 ID와 주문 번호
 * - 사용자 ID와 결제 금액
 * - 토큰 생성/만료 시간
 * - 결제 방식과 추가 메타데이터
 * - 변조 방지를 위한 디지털 서명
 */
public interface PaymentUrlService {

    /**
     * 안전한 결제 URL 생성
     *
     * 주문 생성 직후 호출되어 JWT 토큰이 포함된 결제 URL을 생성합니다.
     * 생성된 URL은 일정 시간 후 만료되며, 변조 시 검증에 실패합니다.
     *
     * @param orderId 주문 ID
     * @param orderNo 주문 번호
     * @param userId 사용자 ID
     * @param amount 결제 금액
     * @param paymentMethod 결제 방식
     * @param expiryMinutes 만료 시간 (분 단위, 기본 30분)
     * @return JWT 토큰이 포함된 결제 URL 정보
     */
    PaymentUrlResponse generateSecurePaymentUrl(UUID orderId, String orderNo, Long userId,
                                               Long amount, String paymentMethod, int expiryMinutes);

    /**
     * 결제 토큰 검증
     *
     * 결제 페이지에서 받은 JWT 토큰의 유효성을 검증합니다.
     * 서명 검증, 만료 시간 확인, 토큰 구조 검사를 수행합니다.
     *
     * @param token JWT 결제 토큰
     * @return 검증된 토큰 정보
     * @throws SecurityException 토큰이 유효하지 않은 경우
     */
    SecurePaymentToken validatePaymentToken(String token) throws SecurityException;

    /**
     * 결제 토큰 갱신
     *
     * 만료가 임박한 토큰을 새로운 만료 시간으로 갱신합니다.
     * 사용자가 결제 진행 중일 때 토큰이 만료되는 것을 방지합니다.
     *
     * @param oldToken 기존 토큰
     * @param additionalMinutes 추가 유효 시간 (분 단위)
     * @return 갱신된 결제 URL 정보
     */
    PaymentUrlResponse refreshPaymentToken(String oldToken, int additionalMinutes);

    /**
     * 결제 토큰 무효화
     *
     * 결제 완료 후나 주문 취소 시 토큰을 무효화합니다.
     * 재사용을 방지하고 보안을 강화합니다.
     *
     * @param token 무효화할 토큰
     * @param reason 무효화 사유
     */
    void invalidatePaymentToken(String token, String reason);

    /**
     * 결제 URL 통계 조회
     *
     * 토큰 생성/검증/만료 통계를 관리자가 확인할 수 있습니다.
     *
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @return 통계 정보
     */
    java.util.Map<String, Object> getPaymentUrlStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 만료된 토큰 정리
     *
     * 주기적으로 만료된 토큰들을 정리하여 저장공간을 절약합니다.
     *
     * @return 정리된 토큰 개수
     */
    int cleanupExpiredTokens();

}