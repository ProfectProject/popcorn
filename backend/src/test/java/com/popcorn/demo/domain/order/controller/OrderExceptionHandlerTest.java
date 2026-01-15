package com.popcorn.demo.domain.order.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.popcorn.demo.common.cache.IdempotencyService;
import com.popcorn.demo.common.dto.BaseError;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.order.exception.OrderValidationException;
import com.popcorn.demo.common.exception.BaseException;

class OrderExceptionHandlerTest {

	@Test
	@DisplayName("BaseException을 사용자 메시지로 변환한다")
	void handlesBaseException() {
		OrderExceptionHandler handler = new OrderExceptionHandler();
		BaseException ex = OrderValidationException.invalidStatusTransition();

		ResponseEntity<BaseResponse<BaseError>> response = handler.handleBaseException(ex);

		assertThat(response.getBody().getData().getDetail()).isNotBlank();
	}

	@Test
	@DisplayName("검증 오류를 처리한다")
	void handlesValidationException() throws Exception {
		OrderExceptionHandler handler = new OrderExceptionHandler();

		Method method = OrderExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class);
		MethodParameter parameter = new MethodParameter(method, 0);
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
		bindingResult.addError(new FieldError("request", "field", "invalid"));
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

		ResponseEntity<BaseResponse<BaseError>> response = handler.handleValidationException(ex);
		assertThat(response.getBody().getData().getDetail()).contains("field");
	}

	@Test
	@DisplayName("BindException을 처리한다")
	void handlesBindException() {
		OrderExceptionHandler handler = new OrderExceptionHandler();
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
		bindingResult.addError(new FieldError("request", "field", "invalid"));
		BindException ex = new BindException(bindingResult);

		ResponseEntity<BaseResponse<BaseError>> response = handler.handleBindException(ex);
		assertThat(response.getBody().getData().getDetail()).contains("field");
	}

	@Test
	@DisplayName("Missing 파라미터 예외를 처리한다")
	void handlesMissingRequestParam() {
		OrderExceptionHandler handler = new OrderExceptionHandler();
		MissingServletRequestParameterException ex =
				new MissingServletRequestParameterException("storeId", "UUID");

		ResponseEntity<BaseResponse<BaseError>> response = handler.handleMissingRequestParam(ex);
		assertThat(response.getBody().getData().getDetail()).contains("storeId");
	}

	@Test
	@DisplayName("타입 불일치 예외를 처리한다")
	void handlesTypeMismatch() {
		OrderExceptionHandler handler = new OrderExceptionHandler();
		MethodArgumentTypeMismatchException ex =
				new MethodArgumentTypeMismatchException("value", Integer.class, "size", null, new IllegalArgumentException());

		ResponseEntity<BaseResponse<BaseError>> response = handler.handleTypeMismatchException(ex);
		assertThat(response.getBody().getData().getDetail()).contains("Integer");
	}

	@Test
	@DisplayName("멱등성 예외를 처리한다")
	void handlesIdempotencyException() {
		OrderExceptionHandler handler = new OrderExceptionHandler();
		IdempotencyService.IdempotencyException ex =
				new IdempotencyService.IdempotencyException("duplicate", "key");

		ResponseEntity<BaseResponse<BaseError>> response = handler.handleIdempotencyException(ex);
		assertThat(response.getBody().getData().getDetail()).isNotBlank();
	}

	@Test
	@DisplayName("기타 예외를 처리한다")
	void handlesGeneralException() {
		OrderExceptionHandler handler = new OrderExceptionHandler();
		ResponseEntity<BaseResponse<BaseError>> response =
				handler.handleGeneralException(new RuntimeException("boom"));
		assertThat(response.getBody().getData()).isNotNull();
	}

	@SuppressWarnings("unused")
	private void dummy(String value) {
	}
}
