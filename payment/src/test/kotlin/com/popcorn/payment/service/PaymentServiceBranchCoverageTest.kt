package com.popcorn.payment.service

import com.popcorn.payment.dto.ApiResponse
import com.popcorn.payment.entity.Payment
import com.popcorn.payment.entity.PaymentMethod
import com.popcorn.payment.entity.PaymentStatus
import com.popcorn.payment.service.QrIssuanceTracker
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 결제 서비스 브랜치 커버리지 향상 테스트
 *
 * [커버리지 향상을 위한 테스트]
 * - 조건문의 모든 분기 테스트
 * - 다양한 입력값과 상태 조합 테스트
 * - 80% 커버리지 달성에 기여하는 핵심 테스트
 */
class PaymentServiceBranchCoverageTest {

    @Test
    fun `OrderDetailApiResponse 모든 필드 조합 테스트`() {
        val testCases = listOf(
            // 기본 케이스
            Triple("READY", "NORMAL", 10000),
            Triple("PAID", "EXPRESS", 50000),
            Triple("CANCELLED", "GIFT", 25000),
            Triple("FAILED", "SUBSCRIPTION", 100000)
        )

        testCases.forEach { (status, orderType, amount) ->
            // Given
            val orderId = UUID.randomUUID()
            val customerId = Random().nextLong()
            val createdAt = LocalDateTime.now()

            // When
            val response = OrderDetailApiResponse(
                orderId = orderId,
                orderNo = "ORDER-$status-$orderType",
                customerId = customerId,
                totalAmount = amount,
                status = status,
                orderType = orderType,
                createdAt = createdAt
            )

            // Then
            assertEquals(orderId, response.orderId)
            assertEquals(status, response.status)
            assertEquals(orderType, response.orderType)
            assertEquals(amount, response.totalAmount)
            assertEquals(customerId, response.customerId)
            assertNotNull(response.toString())

            // equals와 hashCode 테스트
            val same = OrderDetailApiResponse(
                orderId = orderId,
                orderNo = "ORDER-$status-$orderType",
                customerId = customerId,
                totalAmount = amount,
                status = status,
                orderType = orderType,
                createdAt = createdAt
            )
            assertEquals(response, same)
            assertEquals(response.hashCode(), same.hashCode())

            // copy 테스트
            val copied = response.copy(status = "MODIFIED")
            assertEquals("MODIFIED", copied.status)
            assertEquals(amount, copied.totalAmount)
        }
    }

    @Test
    fun `OrderSummaryApiResponse 모든 필드 조합 테스트`() {
        val testCases = listOf(
            Triple("PENDING", "BASIC", 15000),
            Triple("PROCESSING", "PREMIUM", 75000),
            Triple("COMPLETED", "VIP", 200000),
            Triple("REFUNDED", "TRIAL", 1000)
        )

        testCases.forEach { (status, orderType, amount) ->
            // Given
            val orderId = UUID.randomUUID()
            val customerId = Random().nextLong()

            // When
            val response = OrderSummaryApiResponse(
                orderId = orderId,
                orderNo = "SUMMARY-$status",
                customerId = customerId,
                totalAmount = amount,
                status = status,
                orderType = orderType,
                createdAt = LocalDateTime.now()
            )

            // Then
            assertEquals(status, response.status)
            assertEquals(orderType, response.orderType)
            assertEquals(amount, response.totalAmount)
            assertNotNull(response.toString())

            // copy 테스트
            val copied = response.copy(totalAmount = amount * 2)
            assertEquals(amount * 2, copied.totalAmount)
            assertEquals(status, copied.status)
        }
    }

    @Test
    fun `OrderInfo 모든 필드 조합 테스트`() {
        val statusOptions = listOf("CREATED", "CONFIRMED", "SHIPPED", "DELIVERED", "RETURNED")
        val typeOptions = listOf("ONLINE", "OFFLINE", "MOBILE", "KIOSK", "PHONE")

        statusOptions.forEach { status ->
            typeOptions.forEach { type ->
                // Given
                val orderId = UUID.randomUUID()
                val orderNo = "INFO-$status-$type"
                val customerId = Random().nextLong()
                val amount = Random().nextInt(1000, 100000)

                // When
                val orderInfo = OrderInfo(
                    id = orderId,
                    orderNo = orderNo,
                    customerId = customerId,
                    totalAmount = amount,
                    status = status,
                    orderType = type,
                    createdAt = LocalDateTime.now()
                )

                // Then
                assertEquals(status, orderInfo.status)
                assertEquals(type, orderInfo.orderType)
                assertEquals(amount, orderInfo.totalAmount)
                assertEquals(orderNo, orderInfo.orderNo)
                assertNotNull(orderInfo.toString())

                // copy 테스트
                val copied = orderInfo.copy(status = "UPDATED")
                assertEquals("UPDATED", copied.status)
                assertEquals(type, copied.orderType)
            }
        }
    }

    @Test
    fun `PageResponse 다양한 크기 테스트`() {
        val testCases = listOf(
            emptyList<String>(),
            listOf("single"),
            (1..10).map { "item$it" },
            (1..25).map { "item$it" },
            (1..100).map { "item$it" }
        )

        testCases.forEach { content ->
            // When
            val pageResponse = PageResponse(content = content)

            // Then
            assertEquals(content.size, pageResponse.content.size)
            assertEquals(content, pageResponse.content)
            assertNotNull(pageResponse.toString())

            // equals 테스트
            val same = PageResponse(content)
            assertEquals(pageResponse, same)

            // copy 테스트
            val copied = pageResponse.copy(content = content + listOf("extra"))
            assertEquals(content.size + 1, copied.content.size)
            assertTrue(copied.content.contains("extra"))
        }
    }

    @Test
    fun `ApiResponse 성공 케이스들 테스트`() {
        val testData = listOf(
            "문자열 데이터",
            42,
            true,
            listOf("item1", "item2"),
            mapOf("key" to "value"),
            LocalDateTime.now()
        )

        testData.forEach { data ->
            // When
            val successResponse = ApiResponse.success(data, "성공 메시지")

            // Then
            assertTrue(successResponse.success)
            assertEquals(data, successResponse.data)
            assertEquals("성공 메시지", successResponse.message)
            assertNotNull(successResponse.toString())

            // equals 테스트
            val same = ApiResponse.success(data, "성공 메시지")
            assertEquals(successResponse, same)
        }
    }

    @Test
    fun `ApiResponse 오류 케이스들 테스트`() {
        val errorCases = listOf(
            Pair("일반 오류", null),
            Pair("잘못된 요청", "INVALID_REQUEST"),
            Pair("인증 실패", "AUTHENTICATION_FAILED"),
            Pair("권한 없음", "ACCESS_DENIED"),
            Pair("리소스 없음", "RESOURCE_NOT_FOUND"),
            Pair("서버 오류", "INTERNAL_SERVER_ERROR")
        )

        errorCases.forEach { (message, errorCode) ->
            // When
            val errorResponse = ApiResponse.error<String>(message, errorCode)

            // Then
            assertFalse(errorResponse.success)
            assertEquals(message, errorResponse.message)
            assertEquals(errorCode, errorResponse.errorCode)
            assertEquals(null, errorResponse.data)
            assertNotNull(errorResponse.toString())

            // equals 테스트
            val same = ApiResponse.error<String>(message, errorCode)
            assertEquals(errorResponse, same)
        }
    }

    @Test
    fun `PaymentTokenPayload 극한값 테스트`() {
        val extremeCases = listOf(
            Triple(UUID.randomUUID(), Int.MIN_VALUE, Long.MIN_VALUE),
            Triple(UUID.randomUUID(), Int.MAX_VALUE, Long.MAX_VALUE),
            Triple(UUID.randomUUID(), 0, 0L),
            Triple(UUID.randomUUID(), 1, 1L),
            Triple(UUID.randomUUID(), -1, -1L)
        )

        extremeCases.forEach { (orderId, amount, issuedAtMillis) ->
            // When
            val payload = PaymentTokenPayload(orderId, amount, issuedAtMillis)

            // Then
            assertEquals(orderId, payload.orderId)
            assertEquals(amount, payload.amount)
            assertEquals(issuedAtMillis, payload.issuedAtMillis)
            assertNotNull(payload.toString())

            // equals 테스트
            val same = PaymentTokenPayload(orderId, amount, issuedAtMillis)
            assertEquals(payload, same)
            assertEquals(payload.hashCode(), same.hashCode())

            // copy 테스트
            val copied = payload.copy(amount = amount + 1000)
            assertEquals(amount + 1000, copied.amount)
            assertEquals(orderId, copied.orderId)
        }
    }

    @Test
    fun `다양한 결제 방법과 상태 조합 테스트`() {
        val methods = PaymentMethod.values()
        val statuses = PaymentStatus.values()

        methods.forEach { method ->
            statuses.forEach { status ->
                // Given
                val orderId = UUID.randomUUID()
                val payment = Payment.create(orderId, method, 10000, "key_${method}_${status}", "{}")

                // When
                payment.updateStatus(status)
                if (status == PaymentStatus.PAID) {
                    payment.updateStatus(status, LocalDateTime.now())
                }

                // Then
                assertEquals(method, payment.paymentMethod)
                assertEquals(status, payment.status)
                assertEquals(orderId, payment.orderId)
                assertNotNull(payment.toString())

                // 메서드별 추가 검증
                when (method) {
                    PaymentMethod.CARD -> {
                        assertEquals("CARD", method.name)
                        assertEquals(0, method.ordinal)
                    }
                    PaymentMethod.TRANSFER -> {
                        assertEquals("TRANSFER", method.name)
                        assertEquals(1, method.ordinal)
                    }
                    PaymentMethod.VIRTUAL_ACCOUNT -> {
                        assertEquals("VIRTUAL_ACCOUNT", method.name)
                        assertEquals(2, method.ordinal)
                    }
                    PaymentMethod.MOBILE_PHONE -> {
                        assertEquals("MOBILE_PHONE", method.name)
                        assertEquals(3, method.ordinal)
                    }
                    PaymentMethod.GIFT_CERTIFICATE -> {
                        assertEquals("GIFT_CERTIFICATE", method.name)
                        assertEquals(4, method.ordinal)
                    }
                }

                // 상태별 추가 검증
                when (status) {
                    PaymentStatus.READY -> {
                        assertEquals("READY", status.name)
                        assertEquals(0, status.ordinal)
                    }
                    PaymentStatus.PAID -> {
                        assertEquals("PAID", status.name)
                        assertEquals(1, status.ordinal)
                    }
                    PaymentStatus.CANCELLED -> {
                        assertEquals("CANCELLED", status.name)
                        assertEquals(2, status.ordinal)
                    }
                    PaymentStatus.FAILED -> {
                        assertEquals("FAILED", status.name)
                        assertEquals(3, status.ordinal)
                    }
                }
            }
        }
    }

    @Test
    fun `null 안전성 테스트`() {
        // PaymentDetailResult with nulls
        val paymentId = UUID.randomUUID()
        val detailResult = PaymentDetailResult(
            paymentId = paymentId,
            orderId = null,
            paymentKey = null,
            status = "READY",
            amount = 5000,
            approvedAt = null,
            rawPayload = null
        )

        // Then
        assertEquals(paymentId, detailResult.paymentId)
        assertEquals(null, detailResult.orderId)
        assertEquals(null, detailResult.paymentKey)
        assertEquals(null, detailResult.approvedAt)
        assertEquals(null, detailResult.rawPayload)
        assertNotNull(detailResult.toString()) // null 값들과도 잘 동작

        // QrIssuanceTracker 실제 동작 테스트
        val tracker = QrIssuanceTracker()
        val orderId = UUID.randomUUID()

        // 처음 시작은 성공해야 함
        assertTrue(tracker.tryStart(orderId))

        // 이미 진행 중이므로 재시작 불가
        assertFalse(tracker.tryStart(orderId))
    }

    @Test
    fun `동등성과 해시 충돌 테스트`() {
        val baseUuid = UUID.randomUUID()

        // 동일한 데이터로 여러 객체 생성
        val objects = (1..10).map {
            PaymentCreationResult(
                paymentId = baseUuid,
                status = "READY",
                amount = 10000,
                createdAt = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
            )
        }

        // 모든 객체가 서로 같아야 함
        objects.forEach { obj1 ->
            objects.forEach { obj2 ->
                assertEquals(obj1, obj2)
                assertEquals(obj1.hashCode(), obj2.hashCode())
            }
        }

        // 다른 데이터로 객체 생성 - 달라야 함
        val different = PaymentCreationResult(
            paymentId = UUID.randomUUID(),
            status = "READY",
            amount = 10000,
            createdAt = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
        )

        objects.forEach { obj ->
            assertTrue(obj != different)
            assertTrue(obj.hashCode() != different.hashCode())
        }
    }
}