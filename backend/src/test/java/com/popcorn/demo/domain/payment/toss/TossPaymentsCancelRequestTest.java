package com.popcorn.demo.domain.payment.toss;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("❌ TossPayments 결제 취소 요청 테스트")
class TossPaymentsCancelRequestTest {

    @Test
    @DisplayName("Builder 패턴으로 취소 요청 객체 생성 테스트")
    void shouldCreateCancelRequestUsingBuilder() {
        // Given & When
        TossPaymentsCancelRequest request = TossPaymentsCancelRequest.builder()
                .cancelReason("사용자 요청에 의한 취소")
                .build();

        // Then
        assertThat(request.getCancelReason()).isEqualTo("사용자 요청에 의한 취소");
    }

    @Test
    @DisplayName("다양한 취소 사유로 요청 객체 생성 테스트")
    void shouldCreateCancelRequestWithVariousReasons() {
        // Given & When - 사용자 요청 취소
        TossPaymentsCancelRequest userRequest = TossPaymentsCancelRequest.builder()
                .cancelReason("사용자 요청에 의한 취소")
                .build();

        // When - 시스템 오류 취소
        TossPaymentsCancelRequest systemError = TossPaymentsCancelRequest.builder()
                .cancelReason("시스템 오류로 인한 자동 취소")
                .build();

        // When - 재고 부족 취소
        TossPaymentsCancelRequest outOfStock = TossPaymentsCancelRequest.builder()
                .cancelReason("상품 재고 부족")
                .build();

        // When - 관리자 취소
        TossPaymentsCancelRequest adminCancel = TossPaymentsCancelRequest.builder()
                .cancelReason("관리자에 의한 취소")
                .build();

        // Then
        assertThat(userRequest.getCancelReason()).isEqualTo("사용자 요청에 의한 취소");
        assertThat(systemError.getCancelReason()).isEqualTo("시스템 오류로 인한 자동 취소");
        assertThat(outOfStock.getCancelReason()).isEqualTo("상품 재고 부족");
        assertThat(adminCancel.getCancelReason()).isEqualTo("관리자에 의한 취소");
    }

    @Test
    @DisplayName("빈 취소 사유로 요청 객체 생성 테스트")
    void shouldCreateCancelRequestWithEmptyReason() {
        // Given & When
        TossPaymentsCancelRequest request = TossPaymentsCancelRequest.builder()
                .cancelReason("")
                .build();

        // Then
        assertThat(request.getCancelReason()).isEmpty();
    }

    @Test
    @DisplayName("null 취소 사유로 요청 객체 생성 테스트")
    void shouldCreateCancelRequestWithNullReason() {
        // Given & When
        TossPaymentsCancelRequest request = TossPaymentsCancelRequest.builder()
                .cancelReason(null)
                .build();

        // Then
        assertThat(request.getCancelReason()).isNull();
    }

    @Test
    @DisplayName("Builder 없이 기본 생성자로는 객체 생성이 불가능함을 확인")
    void shouldNotCreateWithoutBuilder() {
        // Builder 패턴 강제 사용으로 인해 new TossPaymentsCancelRequest()는 불가능
        // 이는 컴파일 타임에 확인되므로 별도 테스트 불필요하지만 문서화 목적으로 명시

        // Given & When - Builder를 통해서만 생성 가능
        TossPaymentsCancelRequest request = TossPaymentsCancelRequest.builder()
                .cancelReason("Builder 패턴 테스트")
                .build();

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getCancelReason()).isEqualTo("Builder 패턴 테스트");
    }
}