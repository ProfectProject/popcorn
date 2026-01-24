package com.popcorn.checkIns.controller;

import com.popcorn.checkIns.dto.request.QrVerifyRequest;
import com.popcorn.checkIns.dto.response.QrCodeResponse;
import com.popcorn.checkIns.dto.response.QrVerifyResponse;
import com.popcorn.checkIns.exception.QrException;
import com.popcorn.checkIns.service.QrCodeService;
import com.popcorn.common.dto.BaseResponse;
import com.popcorn.common.security.PassportPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QrController 단위 테스트")
class QrControllerUnitTest {

    @Mock
    private QrCodeService qrCodeService;

    @Mock
    private PassportPrincipal principal;

    @InjectMocks
    private QrController qrController;

    private final UUID orderId = UUID.randomUUID();
    private final String qrCode = "QR_CODE_12345";
    private final UUID checkinId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Mock 초기화는 @ExtendWith(MockitoExtension.class)에서 자동 처리
    }

    @Test
    @DisplayName("QR 발급 성공")
    void issueQr_shouldSuccessfullyIssueQrCode() {
        // Given
        QrCodeResponse expectedResponse = QrCodeResponse.builder()
            .orderId(orderId)
            .qrCode(qrCode)
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .build();

        when(qrCodeService.issue(orderId)).thenReturn(expectedResponse);

        // When
        ResponseEntity<BaseResponse<QrCodeResponse>> response = qrController.issueQr(orderId, principal);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        BaseResponse<QrCodeResponse> body = response.getBody();
        assertNotNull(body.getData());
        assertEquals(orderId, body.getData().getOrderId());
        assertEquals(qrCode, body.getData().getQrCode());
        assertNotNull(body.getData().getExpiresAt());

        verify(qrCodeService, times(1)).issue(orderId);
    }

    @Test
    @DisplayName("QR 발급 실패 - 주문 상태 오류")
    void issueQr_shouldHandleOrderStatusError() {
        // Given
        when(qrCodeService.issue(orderId)).thenThrow(QrException.orderNotReserved());

        // When & Then
        QrException exception = assertThrows(QrException.class,
            () -> qrController.issueQr(orderId, principal));

        assertNotNull(exception);
        verify(qrCodeService, times(1)).issue(orderId);
    }

    @Test
    @DisplayName("QR 발급 실패 - 주문을 찾을 수 없음")
    void issueQr_shouldHandleOrderNotFound() {
        // Given
        when(qrCodeService.issue(orderId)).thenThrow(QrException.orderNotFound());

        // When & Then
        QrException exception = assertThrows(QrException.class,
            () -> qrController.issueQr(orderId, principal));

        assertNotNull(exception);
        verify(qrCodeService, times(1)).issue(orderId);
    }

    @Test
    @DisplayName("QR 조회 성공")
    void getQr_shouldSuccessfullyRetrieveQrCode() {
        // Given
        QrCodeResponse expectedResponse = QrCodeResponse.builder()
            .orderId(orderId)
            .qrCode(qrCode)
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .build();

        when(qrCodeService.get(orderId)).thenReturn(expectedResponse);

        // When
        ResponseEntity<BaseResponse<QrCodeResponse>> response = qrController.getQr(orderId, principal);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        BaseResponse<QrCodeResponse> body = response.getBody();
        assertNotNull(body.getData());
        assertEquals(orderId, body.getData().getOrderId());
        assertEquals(qrCode, body.getData().getQrCode());
        assertNotNull(body.getData().getExpiresAt());

        verify(qrCodeService, times(1)).get(orderId);
    }

    @Test
    @DisplayName("QR 조회 실패 - QR 없음")
    void getQr_shouldHandleQrNotFound() {
        // Given
        when(qrCodeService.get(orderId)).thenThrow(QrException.qrNotFound());

        // When & Then
        QrException exception = assertThrows(QrException.class,
            () -> qrController.getQr(orderId, principal));

        assertNotNull(exception);
        verify(qrCodeService, times(1)).get(orderId);
    }

    @Test
    @DisplayName("QR 조회 실패 - QR 만료")
    void getQr_shouldHandleExpiredQr() {
        // Given
        when(qrCodeService.get(orderId)).thenThrow(QrException.qrExpired());

        // When & Then
        QrException exception = assertThrows(QrException.class,
            () -> qrController.getQr(orderId, principal));

        assertNotNull(exception);
        verify(qrCodeService, times(1)).get(orderId);
    }

    @Test
    @DisplayName("QR 검증 성공")
    void verifyQr_shouldSuccessfullyVerifyQrCode() {
        // Given
        QrVerifyRequest request = QrVerifyRequest.builder().qrCode(qrCode).build();

        QrVerifyResponse expectedResponse = QrVerifyResponse.builder()
            .valid(true)
            .checkinId(checkinId)
            .orderId(orderId)
            .qrCode(qrCode)
            .expiresAt(LocalDateTime.now().plusMinutes(2))
            .build();

        when(qrCodeService.verify(qrCode)).thenReturn(expectedResponse);

        // When
        ResponseEntity<BaseResponse<QrVerifyResponse>> response = qrController.verifyQr(request, principal);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        BaseResponse<QrVerifyResponse> body = response.getBody();
        assertNotNull(body.getData());
        assertTrue(body.getData().isValid());
        assertEquals(checkinId, body.getData().getCheckinId());
        assertEquals(orderId, body.getData().getOrderId());
        assertEquals(qrCode, body.getData().getQrCode());
        assertNotNull(body.getData().getExpiresAt());

        verify(qrCodeService, times(1)).verify(qrCode);
    }

    @Test
    @DisplayName("QR 검증 실패 - 잘못된 QR 코드")
    void verifyQr_shouldHandleInvalidQrCode() {
        // Given
        String invalidQrCode = "INVALID_QR_CODE";
        QrVerifyRequest request = QrVerifyRequest.builder().qrCode(invalidQrCode).build();

        when(qrCodeService.verify(invalidQrCode)).thenThrow(QrException.qrNotFound());

        // When & Then
        QrException exception = assertThrows(QrException.class,
            () -> qrController.verifyQr(request, principal));

        assertNotNull(exception);
        verify(qrCodeService, times(1)).verify(invalidQrCode);
    }

    @Test
    @DisplayName("QR 검증 실패 - 만료된 QR 코드")
    void verifyQr_shouldHandleExpiredQrCode() {
        // Given
        QrVerifyRequest request = QrVerifyRequest.builder().qrCode(qrCode).build();

        when(qrCodeService.verify(qrCode)).thenThrow(QrException.qrExpired());

        // When & Then
        QrException exception = assertThrows(QrException.class,
            () -> qrController.verifyQr(request, principal));

        assertNotNull(exception);
        verify(qrCodeService, times(1)).verify(qrCode);
    }

    @ParameterizedTest
    @ValueSource(strings = {"QR_ALPHA_123", "QR_BETA_456", "QR_GAMMA_789", "SPECIAL-QR-001"})
    @DisplayName("다양한 QR 코드 형식에 대한 검증")
    void verifyQr_shouldHandleVariousQrCodeFormats(String testQrCode) {
        // Given
        QrVerifyRequest request = QrVerifyRequest.builder().qrCode(testQrCode).build();

        QrVerifyResponse expectedResponse = QrVerifyResponse.builder()
            .valid(true)
            .checkinId(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .qrCode(testQrCode)
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .build();

        when(qrCodeService.verify(testQrCode)).thenReturn(expectedResponse);

        // When
        ResponseEntity<BaseResponse<QrVerifyResponse>> response = qrController.verifyQr(request, principal);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getData().isValid());
        assertEquals(testQrCode, response.getBody().getData().getQrCode());

        verify(qrCodeService, times(1)).verify(testQrCode);
    }
}