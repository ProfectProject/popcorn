package com.popcorn.demo.domain.goods.exception;

import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.exception.BaseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GoodsExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse<Void>> handleBaseException(BaseException exception) {
        var responseCode = exception.getResponseCode();
        return ResponseEntity
                .status(responseCode.getHttpStatus())
                .body(BaseResponse.error(responseCode));
    }
}
