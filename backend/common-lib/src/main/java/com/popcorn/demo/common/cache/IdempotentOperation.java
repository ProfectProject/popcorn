package com.popcorn.demo.common.cache;

/**
 * 멱등성 작업 인터페이스
 *
 * 이 인터페이스는 멱등성을 보장해야 하는 작업을 정의합니다.
 * 동일한 입력에 대해 여러 번 실행되어도 같은 결과를 반환하는 작업에 사용됩니다.
 *
 * @param <T> 작업 실행 결과의 타입
 */
@FunctionalInterface
public interface IdempotentOperation<T> {

    /**
     * 멱등성을 보장하는 작업을 실행합니다.
     *
     * @return 작업 실행 결과
     * @throws Exception 작업 실행 중 발생할 수 있는 예외
     */
    T execute() throws Exception;
}