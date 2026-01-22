package com.popcorn.order.dto.payment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Payment 서비스 API 응답 래퍼
 */
@Getter
@NoArgsConstructor
@ToString
public class PaymentApiResponse<T> {

    private boolean success;
    private T data;
    private String message;
    private String errorCode;
}
