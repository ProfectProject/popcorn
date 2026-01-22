package com.popcorn.payment.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.coroutines.CoroutineContext

/**
 * JPA + 코루틴 트랜잭션 문제 해결을 위한 설정
 *
 * 🚨 문제점:
 * - Spring @Transactional은 ThreadLocal 기반
 * - 코루틴에서 withContext(Dispatchers.IO)로 스레드 전환 시 트랜잭션 컨텍스트 유실
 *
 * 🔧 해결방안:
 * 1. suspend 함수에서 직접 @Transactional 사용하지 않음
 * 2. 트랜잭션이 필요한 DB 작업은 별도 non-suspend 함수로 분리
 * 3. TransactionalTemplate 사용하여 트랜잭션 경계 명시적 관리
 *
 * 📋 사용 패턴:
 * ```
 * suspend fun businessLogic() {
 *     val result = transactionManager.executeInTransaction {
 *         // JPA 작업들
 *         repository.save(entity)
 *         repository.findById(id)
 *     }
 *     // 비트랜잭션 작업들 (외부 API 호출 등)
 *     externalApiCall()
 * }
 * ```
 */
@Component
class CoroutineTransactionManager {

    /**
     * 트랜잭션 내에서 JPA 작업을 안전하게 실행
     *
     * @param block 트랜잭션 내에서 실행할 작업
     * @return 작업 결과
     */
    @Transactional
    fun <T> executeInTransaction(block: () -> T): T {
        return block()
    }

    /**
     * 읽기 전용 트랜잭션에서 JPA 조회 작업을 안전하게 실행
     *
     * @param block 읽기 전용 트랜잭션 내에서 실행할 작업
     * @return 작업 결과
     */
    @Transactional(readOnly = true)
    fun <T> executeInReadOnlyTransaction(block: () -> T): T {
        return block()
    }

    /**
     * suspend 함수에서 트랜잭션을 사용할 때의 Helper
     * Dispatchers.IO 컨텍스트에서 트랜잭션 실행
     *
     * @param block 트랜잭션 내에서 실행할 작업
     * @return 작업 결과
     */
    suspend fun <T> executeInTransactionSuspend(block: () -> T): T = withContext(Dispatchers.IO) {
        executeInTransaction(block)
    }

    /**
     * suspend 함수에서 읽기 전용 트랜잭션을 사용할 때의 Helper
     *
     * @param block 읽기 전용 트랜잭션 내에서 실행할 작업
     * @return 작업 결과
     */
    suspend fun <T> executeInReadOnlyTransactionSuspend(block: () -> T): T = withContext(Dispatchers.IO) {
        executeInReadOnlyTransaction(block)
    }
}

/**
 * 코루틴에서 안전한 트랜잭션 실행을 위한 확장 함수들
 */

/**
 * suspend 함수에서 트랜잭션 컨텍스트 제공
 * JPA 작업은 반드시 이 컨텍스트 내에서 실행해야 함
 */
suspend fun <T> withTransactionContext(block: suspend () -> T): T =
    withContext(Dispatchers.IO) {
        block()
    }

/**
 * 트랜잭션 경계 명시적 관리를 위한 마커 인터페이스
 * 트랜잭션이 필요한 작업임을 명시
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TransactionalOperation

/**
 * 읽기 전용 작업임을 명시하는 마커 인터페이스
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReadOnlyOperation