package com.popcorn.demo.domain.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.popcorn.demo.common.dto.BaseError;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.common.exception.BaseException;
import com.popcorn.demo.domain.order.dto.OrderResponseCode;

class PaymentExceptionHandlerTest {

    private PaymentExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PaymentExceptionHandler();
    }

    @Test
    void handleBaseException_notFound_returnsUserFriendlyMessage() {
        BaseException ex = new BaseException(CommonResponseCode.NOT_FOUND, "ignored");

        ResponseEntity<BaseResponse<BaseError>> response = handler.handleBaseException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getData().getDetail()).isEqualTo("결제 정보를 찾을 수 없습니다.");
    }

    @Test
    void handleBaseException_orderPaymentAlreadyExists_returnsOrderMessage() {
        BaseException ex = new BaseException(OrderResponseCode.PAYMENT_ALREADY_EXISTS, "ignored");

        ResponseEntity<BaseResponse<BaseError>> response = handler.handleBaseException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getData().getDetail()).isEqualTo("이미 결제 기록이 존재합니다.");
    }

    @Test
    void handleBaseException_orderInvalidStatusTransition_returnsOrderMessage() {
        BaseException ex = new BaseException(OrderResponseCode.INVALID_STATUS_TRANSITION, "ignored");

        ResponseEntity<BaseResponse<BaseError>> response = handler.handleBaseException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getData().getDetail()).isEqualTo("현재 상태에서는 해당 작업을 수행할 수 없습니다.");
    }

    @Test
    void handleBaseException_orderOther_returnsOriginalMessage() {
        BaseException ex = new BaseException(OrderResponseCode.ORDER_NOT_FOUND, "ignored");

        ResponseEntity<BaseResponse<BaseError>> response = handler.handleBaseException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getData().getDetail()).isEqualTo("주문을 찾을 수 없습니다.");
    }

    @Test
    void handleValidationException_returnsFirstFieldError() throws Exception {
        MethodParameter parameter = methodParameter();
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "amount", "must not be null"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<BaseResponse<BaseError>> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getData().getDetail()).isEqualTo("amount: must not be null");
    }

    @Test
    void handleBindException_returnsFirstFieldError() throws Exception {
        BindException ex = new BindException(new Object(), "request");
        ex.addError(new FieldError("request", "orderId", "invalid"));

        ResponseEntity<BaseResponse<BaseError>> response = handler.handleBindException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getData().getDetail()).isEqualTo("orderId: invalid");
    }

    @Test
    void handleTypeMismatchException_formatsMessage() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "id", null, null);

        ResponseEntity<BaseResponse<BaseError>> response = handler.handleTypeMismatchException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getData().getDetail()).isEqualTo("잘못된 Integer 형식입니다: abc");
    }

    @Test
    void handleGeneralException_returnsGenericMessage() {
        ResponseEntity<BaseResponse<BaseError>> response =
                handler.handleGeneralException(new RuntimeException("boom"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getData().getDetail())
                .isEqualTo("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private MethodParameter methodParameter() throws NoSuchMethodException {
        Method method = PaymentExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private void dummy(String value) {
    }
}
