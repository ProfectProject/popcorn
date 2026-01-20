package com.popcorn.checkIns.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.popcorn.checkIns.dto.QrResponseCode;
import com.popcorn.checkIns.exception.QrException;
import com.popcorn.common.dto.BaseError;
import com.popcorn.common.dto.BaseResponse;
import com.popcorn.common.dto.CommonResponseCode;

@DisplayName("QR 예외 핸들러 테스트")
class QrExceptionHandlerTest {

	private QrExceptionHandler exceptionHandler;

	@BeforeEach
	void setUp() {
		exceptionHandler = new QrExceptionHandler();
	}

	@Test
	@DisplayName("BaseException 처리")
	void handleBaseException_returnsCorrectResponse() {
		// Given
		QrException exception = QrException.qrNotFound();

		// When
		ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleBaseException(exception);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo(QrResponseCode.QR_NOT_FOUND.getCode());
		assertThat(response.getBody().getData().getMessage()).isNotBlank();
	}

	@Test
	@DisplayName("MethodArgumentNotValidException 처리")
	void handleValidationException_returnsValidationError() {
		// Given
		BindingResult bindingResult = mock(BindingResult.class);
		FieldError fieldError = new FieldError("qrVerifyRequest", "qrCode", "qrCode는 필수입니다.");
		when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError));

		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

		// When
		ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleValidationException(exception);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST.getCode());
		// 실제로는 BaseController의 error 메서드가 CommonResponseCode의 메시지를 사용함
		assertThat(response.getBody().getData().getMessage()).contains("잘못된 요청입니다");
	}

	@Test
	@DisplayName("BindException 처리")
	void handleBindException_returnsValidationError() {
		// Given
		BindingResult bindingResult = mock(BindingResult.class);
		FieldError fieldError = new FieldError("request", "field", "필드가 잘못되었습니다.");
		when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError));

		BindException exception = new BindException(bindingResult);

		// When
		ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleBindException(exception);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST.getCode());
		assertThat(response.getBody().getData().getMessage()).contains("잘못된 요청입니다");
	}

	@Test
	@DisplayName("MethodArgumentTypeMismatchException 처리")
	void handleTypeMismatchException_returnsTypeError() {
		// Given
		MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
		when(exception.getValue()).thenReturn("invalid-uuid");
		when(exception.getRequiredType()).thenReturn((Class) String.class);

		// When
		ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleTypeMismatchException(exception);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST.getCode());
		assertThat(response.getBody().getData().getMessage()).contains("잘못된 요청입니다");
	}

	@Test
	@DisplayName("일반 Exception 처리")
	void handleGeneralException_returnsInternalError() {
		// Given
		Exception exception = new RuntimeException("예상치 못한 오류");

		// When
		ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleGeneralException(exception);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo(CommonResponseCode.INTERNAL_ERROR.getCode());
		assertThat(response.getBody().getData().getMessage()).contains("서버 오류가 발생했습니다");
	}

	@Test
	@DisplayName("MethodArgumentTypeMismatchException requiredType null 처리")
	void handleTypeMismatchException_withNullRequiredType() {
		// Given
		MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
		when(exception.getValue()).thenReturn("invalid-value");
		when(exception.getRequiredType()).thenReturn(null);

		// When
		ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleTypeMismatchException(exception);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getData().getMessage()).contains("잘못된 요청입니다");
	}

	@Test
	@DisplayName("MethodArgumentNotValidException 필드 에러 없을 때")
	void handleValidationException_withNoFieldErrors() {
		// Given
		BindingResult bindingResult = mock(BindingResult.class);
		when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList());

		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

		// When
		ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleValidationException(exception);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getData().getMessage()).isEqualTo("잘못된 요청입니다.");
	}

	@Test
	@DisplayName("BindException 필드 에러 없을 때")
	void handleBindException_withNoFieldErrors() {
		// Given
		BindingResult bindingResult = mock(BindingResult.class);
		when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList());

		BindException exception = new BindException(bindingResult);

		// When
		ResponseEntity<BaseResponse<BaseError>> response = exceptionHandler.handleBindException(exception);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getData().getMessage()).isEqualTo("잘못된 요청입니다.");
	}
}