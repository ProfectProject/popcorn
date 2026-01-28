package com.popcorn.order.service;

import com.popcorn.order.dto.idempotency.IdempotencyKeyResponse;
import com.popcorn.order.dto.idempotency.IdempotencyKeyValidationResponse;
import com.popcorn.order.dto.idempotency.IdempotencyStatisticsResponse;

import java.time.LocalDateTime;
import java.util.Map;


public interface OrderIdempotencyService {

  
    IdempotencyKeyResponse generateKey();

    
    IdempotencyKeyValidationResponse validateAndUseKey(String idempotencyKey, String requestId, Long userId);

  
    Object getPreviousResult(String idempotencyKey);

  
    void storeResult(String idempotencyKey, Object result, int ttlMinutes);

    
    void invalidateKey(String idempotencyKey, String reason);

    IdempotencyStatisticsResponse getStatistics(LocalDateTime startTime, LocalDateTime endTime, String period);

   
    int cleanupExpiredKeys();

    Map<String, Object> getKeyStatus(String idempotencyKey);

    IdempotencyKeyResponse getActiveKeysForUser(Long userId);

}
