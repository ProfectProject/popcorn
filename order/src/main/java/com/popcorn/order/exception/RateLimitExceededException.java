package com.popcorn.order.exception;

import lombok.Getter;

/**
 * Rate Limit 초과 예외
 *
 * API 호출 한도를 초과했을 때 발생하는 예외입니다.
 * HTTP 429 Too Many Requests 응답과 함께 사용됩니다.
 */
@Getter
public class RateLimitExceededException extends RuntimeException {

    private final int requestLimit;
    private final int windowSeconds;
    private final String limitKey;

    public RateLimitExceededException(String message, int requestLimit, int windowSeconds) {
        super(message);
        this.requestLimit = requestLimit;
        this.windowSeconds = windowSeconds;
        this.limitKey = null;
    }

    public RateLimitExceededException(String message, int requestLimit, int windowSeconds, String limitKey) {
        super(message);
        this.requestLimit = requestLimit;
        this.windowSeconds = windowSeconds;
        this.limitKey = limitKey;
    }

    /**
     * 다시 시도할 수 있는 시간(초) 반환
     * @return 재시도 권장 시간
     */
    public int getRetryAfterSeconds() {
        return windowSeconds;
    }

}