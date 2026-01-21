package com.popcorn.store.domain.store.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.popcorn.common.dto.BaseError;
import com.popcorn.common.dto.BaseResponse;
import com.popcorn.common.dto.CommonResponseCode;
import com.popcorn.store.domain.store.exception.StoreException;

class StoreExceptionHandlerTest {

	@Test
	void handlesBaseException() {
		StoreExceptionHandler handler = new StoreExceptionHandler();

		ResponseEntity<BaseResponse<BaseError>> response = handler.handleBaseException(StoreException.invalidRequest());

		assertThat(response.getStatusCode().value()).isEqualTo(CommonResponseCode.INVALID_REQUEST.getHttpStatus());
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST.getCode());
	}

	@Test
	void handlesValidationException() throws Exception {
		StoreExceptionHandler handler = new StoreExceptionHandler();

		MethodParameter parameter = new MethodParameter(
				StoreExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class), 0);
		BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
		bindingResult.addError(new FieldError("request", "name", "required"));
		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

		ResponseEntity<BaseResponse<BaseError>> response = handler.handleValidationException(exception);

		assertThat(response.getStatusCode().value()).isEqualTo(CommonResponseCode.INVALID_REQUEST.getHttpStatus());
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getData().getDetail()).isEqualTo("입력값이 유효하지 않습니다.");
	}

	@Test
	void handlesBindException() {
		StoreExceptionHandler handler = new StoreExceptionHandler();

		BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
		bindingResult.addError(new FieldError("request", "name", "required"));
		BindException exception = new BindException(bindingResult);

		ResponseEntity<BaseResponse<BaseError>> response = handler.handleBindException(exception);

		assertThat(response.getStatusCode().value()).isEqualTo(CommonResponseCode.INVALID_REQUEST.getHttpStatus());
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getData().getDetail()).isEqualTo("요청 데이터 바인딩에 실패했습니다.");
	}

	private static void dummy(String input) {
		// no-op
	}
}
