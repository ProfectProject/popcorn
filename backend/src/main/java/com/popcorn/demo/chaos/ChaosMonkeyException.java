package com.popcorn.demo.chaos;

/**
 * 🐒 Chaos Monkey 전용 예외
 *
 * 장애 시뮬레이션 중 발생하는 의도적인 예외
 */
public class ChaosMonkeyException extends RuntimeException {

    public ChaosMonkeyException(String message) {
        super("🐒 CHAOS MONKEY STRIKE! " + message);
    }

    public ChaosMonkeyException(String message, Throwable cause) {
        super("🐒 CHAOS MONKEY STRIKE! " + message, cause);
    }
}