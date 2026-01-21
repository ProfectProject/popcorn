package com.example.orderquery.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.popcorn.common.controller.BaseController;
import com.popcorn.common.dto.BaseError;
import com.popcorn.common.dto.BaseResponse;
import com.example.orderquery.domain.itemView.exception.ItemViewException;
import com.example.orderquery.domain.summary.exception.SummaryException;
import com.example.orderquery.global.exception.OwnerAuthException;
import com.popcorn.common.dto.CommonResponseCode;
import com.popcorn.common.exception.BaseException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class OrderQueryExceptionHandler extends BaseController {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse<BaseError>> handleBaseException(BaseException ex) {
        log.warn("OrderQuery business exception: {}", ex.getMessage());
        if (ex instanceof ItemViewException itemViewException) {
            return error(itemViewException.getResponseCode(), itemViewException.getDetail());
        }
        if (ex instanceof SummaryException summaryException) {
            return error(summaryException.getResponseCode(), summaryException.getDetail());
        }
        if (ex instanceof OwnerAuthException ownerAuthException) {
            return error(ownerAuthException.getResponseCode(), ownerAuthException.getDetail());
        }
        return error(ex.getResponseCode(), ex.getMessage());
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    public ResponseEntity<BaseResponse<BaseError>> handleValidationException(Exception ex) {
        log.warn("OrderQuery validation error: {}", ex.getMessage());
        return error(CommonResponseCode.INVALID_REQUEST, "입력값이 올바르지 않습니다.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BaseResponse<BaseError>> handleMissingRequestParam(
            MissingServletRequestParameterException ex) {
        log.warn("OrderQuery missing request parameter: {}", ex.getMessage());
        return error(CommonResponseCode.INVALID_REQUEST, "필수 요청 파라미터가 누락되었습니다.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<BaseError>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("OrderQuery type mismatch: {}", ex.getMessage());
        return error(CommonResponseCode.INVALID_REQUEST, "요청 파라미터 타입이 올바르지 않습니다.");
    }
}
