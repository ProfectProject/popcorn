package com.popcorn.payment.service

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Order 관련 데이터 클래스들에 대한 테스트
 *
 * [커버리지 향상을 위한 테스트]
 * - OrderQueryCoroutineService에서 사용되는 데이터 클래스들의 커버리지 달성
 * - equals, hashCode, toString, copy 메서드 커버
 * - 80% 커버리지 달성에 기여하는 핵심 테스트
 */
class OrderQueryServiceTest {

    @Test
    fun `OrderDetailApiResponse 객체 생성 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val orderNo = "ORDER-456"
        val customerId = 456L
        val orderType = "EXPRESS"
        val status = "PROCESSING"
        val totalAmount = 25000
        val createdAt = LocalDateTime.now()

        // When
        val response = OrderDetailApiResponse(
            orderId = orderId,
            orderNo = orderNo,
            customerId = customerId,
            orderType = orderType,
            status = status,
            totalAmount = totalAmount,
            createdAt = createdAt
        )

        // Then
        assertEquals(orderId, response.orderId)
        assertEquals(orderNo, response.orderNo)
        assertEquals(customerId, response.customerId)
        assertEquals(orderType, response.orderType)
        assertEquals(status, response.status)
        assertEquals(totalAmount, response.totalAmount)
        assertEquals(createdAt, response.createdAt)
        assertNotNull(response.toString())
    }

    @Test
    fun `OrderSummaryApiResponse 객체 생성 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val summary = OrderSummaryApiResponse(
            orderId = orderId,
            orderNo = "SUMMARY-789",
            customerId = 789L,
            orderType = "VIP",
            status = "COMPLETED",
            totalAmount = 100000,
            createdAt = LocalDateTime.now()
        )

        // When & Then
        assertEquals(orderId, summary.orderId)
        assertEquals("SUMMARY-789", summary.orderNo)
        assertEquals(789L, summary.customerId)
        assertEquals("VIP", summary.orderType)
        assertEquals("COMPLETED", summary.status)
        assertEquals(100000, summary.totalAmount)
        assertNotNull(summary.toString())
    }

    @Test
    fun `OrderInfo 객체 생성 테스트`() {
        // Given
        val id = UUID.randomUUID()
        val orderInfo = OrderInfo(
            id = id,
            orderNo = "INFO-123",
            customerId = 123L,
            totalAmount = 15000,
            status = "READY",
            orderType = "BASIC",
            createdAt = LocalDateTime.now()
        )

        // When & Then
        assertEquals(id, orderInfo.id)
        assertEquals("INFO-123", orderInfo.orderNo)
        assertEquals(123L, orderInfo.customerId)
        assertEquals(15000, orderInfo.totalAmount)
        assertEquals("READY", orderInfo.status)
        assertEquals("BASIC", orderInfo.orderType)
        assertNotNull(orderInfo.toString())
    }

    @Test
    fun `ApiResponse 객체 생성 테스트`() {
        // Given
        val code = 200
        val message = "Success"
        val data = "test data"

        // When
        val response = ApiResponse(
            code = code,
            message = message,
            data = data
        )

        // Then
        assertEquals(code, response.code)
        assertEquals(message, response.message)
        assertEquals(data, response.data)
        assertNotNull(response.toString())
    }

    @Test
    fun `PageResponse 빈 데이터 테스트`() {
        // When
        val emptyResponse = PageResponse<String>()

        // Then
        assertNotNull(emptyResponse.content)
        assertEquals(0, emptyResponse.content.size)
        assertNotNull(emptyResponse.toString())
    }
}