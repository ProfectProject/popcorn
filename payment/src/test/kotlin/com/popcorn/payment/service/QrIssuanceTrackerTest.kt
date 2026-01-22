package com.popcorn.payment.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * QrIssuanceTracker 테스트
 *
 * [커버리지 향상을 위한 테스트]
 * - QR 발급 중복 방지 로직 테스트
 * - 동시성 제어 및 상태 관리 테스트
 * - 발급 성공/실패 시나리오 테스트
 * - 멀티스레드 환경에서의 안전성 테스트
 * - 80% 커버리지 달성에 기여하는 핵심 테스트
 */
class QrIssuanceTrackerTest {

    private lateinit var qrIssuanceTracker: QrIssuanceTracker

    @BeforeEach
    fun setUp() {
        qrIssuanceTracker = QrIssuanceTracker()
    }

    @Test
    fun `최초 QR 발급 시작 성공 테스트`() {
        // Given
        val orderId = UUID.randomUUID()

        // When
        val canStart = qrIssuanceTracker.tryStart(orderId)

        // Then
        assertTrue(canStart, "최초 QR 발급은 시작할 수 있어야 합니다")
    }

    @Test
    fun `동일한 주문ID로 중복 발급 시도 실패 테스트`() {
        // Given
        val orderId = UUID.randomUUID()

        // When
        val firstAttempt = qrIssuanceTracker.tryStart(orderId)
        val secondAttempt = qrIssuanceTracker.tryStart(orderId)

        // Then
        assertTrue(firstAttempt, "첫 번째 발급 시도는 성공해야 합니다")
        assertFalse(secondAttempt, "동일한 주문ID로 두 번째 발급 시도는 실패해야 합니다")
    }

    @Test
    fun `발급 성공 후 완료 상태 유지 테스트`() {
        // Given
        val orderId = UUID.randomUUID()

        // When
        qrIssuanceTracker.tryStart(orderId)
        qrIssuanceTracker.markSuccess(orderId)
        val afterSuccess = qrIssuanceTracker.tryStart(orderId)

        // Then
        assertFalse(afterSuccess, "발급 완료된 주문ID는 재시도할 수 없어야 합니다")
    }

    @Test
    fun `발급 실패 후 재시도 가능 테스트`() {
        // Given
        val orderId = UUID.randomUUID()

        // When
        val firstAttempt = qrIssuanceTracker.tryStart(orderId)
        qrIssuanceTracker.markFailure(orderId)
        val retryAttempt = qrIssuanceTracker.tryStart(orderId)

        // Then
        assertTrue(firstAttempt, "첫 번째 발급 시도는 성공해야 합니다")
        assertTrue(retryAttempt, "발급 실패 후 재시도는 가능해야 합니다")
    }

    @Test
    fun `다른 주문ID는 독립적으로 처리 테스트`() {
        // Given
        val orderId1 = UUID.randomUUID()
        val orderId2 = UUID.randomUUID()

        // When
        val start1 = qrIssuanceTracker.tryStart(orderId1)
        val start2 = qrIssuanceTracker.tryStart(orderId2)

        // Then
        assertTrue(start1, "첫 번째 주문ID 발급 시작은 성공해야 합니다")
        assertTrue(start2, "두 번째 주문ID 발급 시작도 성공해야 합니다")
    }

    @Test
    fun `발급 실패 후 다시 성공으로 처리 가능 테스트`() {
        // Given
        val orderId = UUID.randomUUID()

        // When
        qrIssuanceTracker.tryStart(orderId)
        qrIssuanceTracker.markFailure(orderId)
        val retryStart = qrIssuanceTracker.tryStart(orderId)
        qrIssuanceTracker.markSuccess(orderId)
        val afterSuccess = qrIssuanceTracker.tryStart(orderId)

        // Then
        assertTrue(retryStart, "실패 후 재시도는 성공해야 합니다")
        assertFalse(afterSuccess, "성공 후에는 재시도할 수 없어야 합니다")
    }

    @Test
    fun `존재하지 않는 주문ID에 대한 성공 처리 테스트`() {
        // Given
        val orderId = UUID.randomUUID()

        // When - 시작하지 않은 주문ID에 대해 성공 처리
        qrIssuanceTracker.markSuccess(orderId)
        val canStart = qrIssuanceTracker.tryStart(orderId)

        // Then
        assertFalse(canStart, "이미 완료된 상태로 설정된 주문ID는 시작할 수 없어야 합니다")
    }

    @Test
    fun `존재하지 않는 주문ID에 대한 실패 처리 테스트`() {
        // Given
        val orderId = UUID.randomUUID()

        // When - 시작하지 않은 주문ID에 대해 실패 처리 (아무 효과 없음)
        qrIssuanceTracker.markFailure(orderId)
        val canStart = qrIssuanceTracker.tryStart(orderId)

        // Then
        assertTrue(canStart, "존재하지 않는 주문ID 실패 처리 후에도 시작할 수 있어야 합니다")
    }

    @Test
    fun `여러 주문ID 동시 처리 테스트`() {
        // Given
        val orderIds = (1..10).map { UUID.randomUUID() }

        // When
        val startResults = orderIds.map { orderId ->
            qrIssuanceTracker.tryStart(orderId)
        }

        // 일부는 성공으로, 일부는 실패로 처리
        orderIds.take(5).forEach { qrIssuanceTracker.markSuccess(it) }
        orderIds.drop(5).forEach { qrIssuanceTracker.markFailure(it) }

        // 재시도 테스트
        val retryResults = orderIds.map { orderId ->
            qrIssuanceTracker.tryStart(orderId)
        }

        // Then
        startResults.forEach { assertTrue(it, "모든 첫 번째 시도는 성공해야 합니다") }

        retryResults.take(5).forEach {
            assertFalse(it, "성공으로 처리된 주문ID는 재시도할 수 없어야 합니다")
        }
        retryResults.drop(5).forEach {
            assertTrue(it, "실패로 처리된 주문ID는 재시도할 수 있어야 합니다")
        }
    }

    @Test
    fun `동시성 테스트 - 같은 주문ID 멀티스레드 시작 시도`() {
        // Given
        val orderId = UUID.randomUUID()
        val threadCount = 100
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val results = mutableListOf<Boolean>()

        // When
        val futures = (1..threadCount).map {
            CompletableFuture.supplyAsync({
                try {
                    latch.countDown()
                    latch.await() // 모든 스레드가 준비될 때까지 대기
                    qrIssuanceTracker.tryStart(orderId)
                } catch (e: Exception) {
                    false
                }
            }, executor)
        }

        // 모든 Future 완료 대기
        futures.forEach { future ->
            results.add(future.get())
        }

        executor.shutdown()

        // Then
        val successCount = results.count { it }
        assertTrue(successCount == 1, "동시 접근 시 정확히 하나의 스레드만 성공해야 합니다. 실제: $successCount")
    }

    @Test
    fun `동시성 테스트 - 서로 다른 주문ID 멀티스레드 시작 시도`() {
        // Given
        val threadCount = 50
        val orderIds = (1..threadCount).map { UUID.randomUUID() }
        val executor = Executors.newFixedThreadPool(threadCount)
        val results = mutableListOf<Boolean>()

        // When
        val futures = orderIds.mapIndexed { index, orderId ->
            CompletableFuture.supplyAsync({
                try {
                    qrIssuanceTracker.tryStart(orderId)
                } catch (e: Exception) {
                    false
                }
            }, executor)
        }

        // 모든 Future 완료 대기
        futures.forEach { future ->
            results.add(future.get())
        }

        executor.shutdown()

        // Then
        val successCount = results.count { it }
        assertTrue(successCount == threadCount, "서로 다른 주문ID는 모두 성공해야 합니다. 실제: $successCount/$threadCount")
    }

    @Test
    fun `복합 시나리오 테스트 - 시작, 실패, 재시도, 성공`() {
        // Given
        val orderId = UUID.randomUUID()

        // When & Then
        // 1. 최초 시작
        assertTrue(qrIssuanceTracker.tryStart(orderId), "최초 시작은 성공해야 합니다")

        // 2. 중복 시작 시도 (실패)
        assertFalse(qrIssuanceTracker.tryStart(orderId), "중복 시작은 실패해야 합니다")

        // 3. 발급 실패 처리
        qrIssuanceTracker.markFailure(orderId)

        // 4. 재시도 (성공)
        assertTrue(qrIssuanceTracker.tryStart(orderId), "실패 후 재시도는 성공해야 합니다")

        // 5. 다시 중복 시작 시도 (실패)
        assertFalse(qrIssuanceTracker.tryStart(orderId), "재시도 후 중복 시작은 실패해야 합니다")

        // 6. 발급 성공 처리
        qrIssuanceTracker.markSuccess(orderId)

        // 7. 완료 후 시작 시도 (실패)
        assertFalse(qrIssuanceTracker.tryStart(orderId), "완료 후 시작은 실패해야 합니다")

        // 8. 실패 처리 시도 (완료 상태는 유지됨)
        qrIssuanceTracker.markFailure(orderId)
        assertFalse(qrIssuanceTracker.tryStart(orderId), "완료 후 실패 처리해도 여전히 시작할 수 없어야 합니다")
    }

    @Test
    fun `동시성 테스트 - 시작과 완료가 경쟁하는 상황`() {
        // Given
        val orderId = UUID.randomUUID()
        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val results = mutableListOf<Boolean>()

        // When
        // 첫 번째 스레드에서 시작
        qrIssuanceTracker.tryStart(orderId)

        // 여러 스레드에서 동시에 성공 처리
        val futures = (1..threadCount).map {
            CompletableFuture.supplyAsync({
                try {
                    startLatch.await()
                    qrIssuanceTracker.markSuccess(orderId)
                    qrIssuanceTracker.tryStart(orderId) // 성공 처리 후 시작 시도
                } catch (e: Exception) {
                    true // 예외 발생해도 괜찮음
                }
            }, executor)
        }

        startLatch.countDown() // 모든 스레드 시작

        futures.forEach { future ->
            results.add(future.get())
        }

        executor.shutdown()

        // Then
        val finalAttempt = qrIssuanceTracker.tryStart(orderId)
        assertFalse(finalAttempt, "동시 성공 처리 후에는 시작할 수 없어야 합니다")
    }
}