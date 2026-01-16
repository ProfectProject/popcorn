package com.popcorn.demo.domain.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.popcorn.demo.common.dto.BaseError;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.domain.order.dto.OrderResponseCode;
import com.popcorn.demo.common.exception.BaseException;

class PaymentExceptionHandlerTest {

    private PaymentExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PaymentExceptionHandler();
    }

    @Test
    void handleBaseException_WithNotFoundError_ReturnsUserFriendlyMessage() {
        // given
        BaseException ex = new BaseException(CommonResponseCode.NOT_FOUND, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");

        // when
        ResponseEntity<BaseResponse<BaseError>> response = handler.handleBaseException(ex);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getData().getDetail()).isEqualTo("결제 정보를 찾을 수 없습니다.");
    }

    @Test
    void handleBaseException_WithInvalidRequestError_ReturnsUserFriendlyMessage() {
        // given
        BaseException ex = new BaseException(CommonResponseCode.INVALID_REQUEST, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");

        // when
        ResponseEntity<BaseResponse<BaseError>> response = handler.handleBaseException(ex);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getData().getDetail()).isEqualTo("요청값을 확인해 주세요.");
    }

    @Test
    void handleBaseException_WithPaymentAlreadyExistsError_ReturnsSpecificMessage() {
        // given
        BaseException ex = new BaseException(OrderResponseCode.PAYMENT_ALREADY_EXISTS, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");

        // when
        ResponseEntity<BaseResponse<BaseError>> response = handler.handleBaseException(ex);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(OrderResponseCode.PAYMENT_ALREADY_EXISTS.getHttpStatus());
        assertThat(response.getBody().getData().getDetail()).isEqualTo("이미 결제 기록이 존재합니다.");
    }

    @Test
    void handleBaseException_WithInvalidStatusTransitionError_ReturnsSpecificMessage() {
        // given
        BaseException ex = new BaseException(OrderResponseCode.INVALID_STATUS_TRANSITION, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");

        // when
        ResponseEntity<BaseResponse<BaseError>> response = handler.handleBaseException(ex);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(OrderResponseCode.INVALID_STATUS_TRANSITION.getHttpStatus());
        assertThat(response.getBody().getData().getDetail()).isEqualTo("현재 상태에서는 해당 작업을 수행할 수 없습니다.");
    }

    @Test
    void handleBaseException_WithOtherOrderResponseCode_ReturnsOriginalMessage() {
        // given
        BaseException ex = new BaseException(OrderResponseCode.ORDER_NOT_FOUND, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");

        // when
        ResponseEntity<BaseResponse<BaseError>> response = handler.handleBaseException(ex);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(OrderResponseCode.ORDER_NOT_FOUND.getHttpStatus());
        assertThat(response.getBody().getData().getDetail()).isEqualTo("주문을 찾을 수 없습니다.");
    }

    @Test
    void handleValidationException_WithFieldError_ReturnsFieldSpecificMessage() {
        // given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("payment", "amount", "must not be null");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        // when
        ResponseEntity<BaseResponse<BaseError>> response = handler.handleValidationException(ex);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getData().getDetail()).isEqualTo("amount: must not be null");
    }

    @Test
    void handleValidationException_WithNoFieldError_ReturnsGenericMessage() {
        // given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        // when
        ResponseEntity<BaseResponse<BaseError>> response = handler.handleValidationException(ex);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getData().getDetail()).isEqualTo("입력값이 올바르지 않습니다.");
    }

    @Test
    void handleBindException_WithFieldError_ReturnsFieldSpecificMessage() {
        // given
        BindException ex = mock(BindException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("payment", "status", "invalid status value");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        // when
        ResponseEntity<BaseResponse<BaseError>> response = handler.handleBindException(ex);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getData().getDetail()).isEqualTo("status: invalid status value");
    }

    @Test
    void handleTypeMismatchException_WithKnownType_ReturnsTypeSpecificMessage() {
        // given
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        MethodParameter parameter = mock(MethodParameter.class);

        when(ex.getRequiredType()).thenReturn((Class) Integer.class);
        when(ex.getValue()).thenReturn("invalid-number");

        // when
        ResponseEntity<BaseResponse<BaseError>> response = handler.handleTypeMismatchException(ex);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getData().getDetail()).contains("잘못된 Integer 형식입니다: invalid-number");
    }

    @Test
    void handleTypeMismatchException_WithUnknownType_ReturnsGenericMessage() {
        // given
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);

        when(ex.getRequiredType()).thenReturn(null);
        when(ex.getValue()).thenReturn("invalid-value");

        // when
        ResponseEntity<BaseResponse<BaseError>> response = handler.handleTypeMismatchException(ex);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getData().getDetail()).contains("잘못된 알 수 없는 타입 형식입니다: invalid-value");
    }

    @Test
    void handleGeneralException_ReturnsGenericErrorMessage() {
        // given
        Exception ex = new RuntimeException("Something went wrong");

        // when
        ResponseEntity<BaseResponse<BaseError>> response = handler.handleGeneralException(ex);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getData().getDetail()).isEqualTo("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    @Test
    void handleGeneralException_WithNullPointerException_ReturnsGenericErrorMessage() {
        // given
        Exception ex = new NullPointerException("Null value encountered");

        // when
        ResponseEntity<BaseResponse<BaseError>> response = handler.handleGeneralException(ex);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getData().getDetail()).isEqualTo("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    @Test
    void handleValidationException_WithMultipleFieldErrors_ReturnsFirstError() {
        // given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError firstError = new FieldError("payment", "amount", "must be positive");
        FieldError secondError = new FieldError("payment", "method", "must not be null");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(firstError, secondError));

        // when
        ResponseEntity<BaseResponse<BaseError>> response = handler.handleValidationException(ex);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getData().getDetail()).isEqualTo("amount: must be positive");
    }
}