package com.popcorn.order.service;

import java.time.LocalDateTime;
import java.util.UUID;

import com.popcorn.order.dto.payment.PaymentUrlResponse;
import com.popcorn.order.dto.payment.SecurePaymentToken;

public interface PaymentUrlService {

    
    PaymentUrlResponse generateSecurePaymentUrl(UUID orderId, String orderNo, Long userId,
                                               Long amount, String paymentMethod, int expiryMinutes);


    SecurePaymentToken validatePaymentToken(String token) throws SecurityException;

    
    PaymentUrlResponse refreshPaymentToken(String oldToken, int additionalMinutes);


    void invalidatePaymentToken(String token, String reason);

    java.util.Map<String, Object> getPaymentUrlStatistics(LocalDateTime startTime, LocalDateTime endTime);


    int cleanupExpiredTokens();

}
