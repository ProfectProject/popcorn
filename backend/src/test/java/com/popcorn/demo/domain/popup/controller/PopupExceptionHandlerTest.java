package com.popcorn.demo.domain.popup.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.popcorn.demo.common.dto.BaseError;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.domain.popup.dto.PopupResponseCode;
import com.popcorn.demo.domain.popup.exception.PopupException;

class PopupExceptionHandlerTest {

    private PopupExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new PopupExceptionHandler();
    }

    @Test
    void handleBaseExceptionWithPopupNotFoundException() {
        PopupException exception = new PopupException(PopupResponseCode.POPUP_NOT_FOUND);

        ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleBaseException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(PopupResponseCode.POPUP_NOT_FOUND.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("팝업 정보를 찾을 수 없습니다.");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getDetail()).isEqualTo("팝업 정보를 찾을 수 없습니다.");
    }

    @Test
    void handleBaseExceptionWithInvalidRequest() {
        PopupException exception = new PopupException(PopupResponseCode.INVALID_REQUEST);

        ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleBaseException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(PopupResponseCode.INVALID_REQUEST.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 요청입니다.");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getDetail()).isEqualTo("요청을 확인해 주세요.");
    }

    @Test
    void handleValidationExceptionWithFieldErrors() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("popup", "name", "이름은 필수입니다.");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleValidationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 요청입니다.");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getDetail()).isEqualTo("name: 이름은 필수입니다.");
    }

    @Test
    void handleValidationExceptionWithNoFieldErrors() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleValidationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 요청입니다.");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getDetail()).isEqualTo("입력값이 올바르지 않습니다.");
    }

    @Test
    void handleBindExceptionWithFieldErrors() {
        BindException exception = mock(BindException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("popup", "startDate", "시작일은 필수입니다.");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleBindException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 요청입니다.");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getDetail()).isEqualTo("startDate: 시작일은 필수입니다.");
    }

    @Test
    void handleBindExceptionWithNoFieldErrors() {
        BindException exception = mock(BindException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleBindException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 요청입니다.");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getDetail()).isEqualTo("입력값이 올바르지 않습니다.");
    }

    @Test
    void handleTypeMismatchExceptionWithIntegerType() {
        MethodParameter parameter = mock(MethodParameter.class);
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "popupId", parameter, new NumberFormatException());

        ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleTypeMismatchException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 요청입니다.");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getDetail()).isEqualTo("잘못된 Integer 형식입니다: abc");
    }

    @Test
    void handleTypeMismatchExceptionWithNullRequiredType() {
        MethodParameter parameter = mock(MethodParameter.class);
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "invalid", null, "param", parameter, new IllegalArgumentException());

        ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleTypeMismatchException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 요청입니다.");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getDetail()).isEqualTo("잘못된 요청 형식입니다: invalid");
    }

    @Test
    void handleValidationExceptionWithMultipleFieldErrors() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError firstError = new FieldError("popup", "name", "이름은 필수입니다.");
        FieldError secondError = new FieldError("popup", "description", "설명은 필수입니다.");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(firstError, secondError));

        ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleValidationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 요청입니다.");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getDetail()).isEqualTo("name: 이름은 필수입니다.");
    }

    @Test
    void handleBindExceptionWithMultipleFieldErrors() {
        BindException exception = mock(BindException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError firstError = new FieldError("popup", "startDate", "시작일은 필수입니다.");
        FieldError secondError = new FieldError("popup", "endDate", "종료일은 필수입니다.");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(firstError, secondError));

        ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleBindException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 요청입니다.");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getDetail()).isEqualTo("startDate: 시작일은 필수입니다.");
    }

    @Test
    void handleBaseExceptionWithCustomMessage() {
        // Create a custom PopupException with a specific message that should return the original message
        PopupException exception = new PopupException(PopupResponseCode.INVALID_REQUEST) {
            @Override
            public String getMessage() {
                return "커스텀 에러 메시지";
            }
        };

        ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleBaseException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(PopupResponseCode.INVALID_REQUEST.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 요청입니다.");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getDetail()).isEqualTo("요청을 확인해 주세요.");
    }

    @Test
    void handleTypeMismatchExceptionWithLongType() {
        MethodParameter parameter = mock(MethodParameter.class);
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "not-a-number", Long.class, "userId", parameter, new NumberFormatException());

        ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleTypeMismatchException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 요청입니다.");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getDetail()).isEqualTo("잘못된 Long 형식입니다: not-a-number");
    }
}