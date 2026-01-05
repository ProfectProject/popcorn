package com.popcorn.demo.domain.merch.exception;

import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.dto.ResponseCode;
import com.popcorn.demo.common.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MerchExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse<Void>> handleBaseException(BaseException exception) {
        ResponseCode responseCode = exception.getResponseCode();
        return ResponseEntity
                .status(resolveStatus(responseCode))
                .body(BaseResponse.error(responseCode));
    }

    private HttpStatus resolveStatus(ResponseCode responseCode) {
        if (responseCode == ResponseCode.NOT_FOUND) {
            return HttpStatus.NOT_FOUND;
        }
        if (responseCode == ResponseCode.INVALID_REQUEST) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
