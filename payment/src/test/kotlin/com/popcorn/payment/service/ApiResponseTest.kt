package com.popcorn.payment.service

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ApiResponseTest {

    @Test
    fun `ApiResponse 기본 생성 테스트`() {
        // Given & When
        val response = ApiResponse<String>()

        // Then
        assertNull(response.code)
        assertNull(response.message)
        assertNull(response.data)
    }

    @Test
    fun `ApiResponse 전체 필드 생성 테스트`() {
        // Given
        val code = 200
        val message = "Success"
        val data = "Test Data"

        // When
        val response = ApiResponse(code = code, message = message, data = data)

        // Then
        assertEquals(code, response.code)
        assertEquals(message, response.message)
        assertEquals(data, response.data)
    }

    @Test
    fun `ApiResponse copy 메서드 테스트`() {
        // Given
        val original = ApiResponse(code = 200, message = "Original", data = "data")

        // When
        val copied = original.copy(message = "Modified")

        // Then
        assertEquals(original.code, copied.code)
        assertEquals("Modified", copied.message)
        assertEquals(original.data, copied.data)
    }

    @Test
    fun `PageResponse 기본 생성 테스트`() {
        // Given & When
        val response = PageResponse<String>()

        // Then
        assertNotNull(response.content)
        assertEquals(0, response.content.size)
    }

    @Test
    fun `PageResponse 리스트 포함 생성 테스트`() {
        // Given
        val content = listOf("item1", "item2", "item3")

        // When
        val response = PageResponse(content = content)

        // Then
        assertEquals(content, response.content)
        assertEquals(3, response.content.size)
    }

    @Test
    fun `OrderDetailApiResponse 생성 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val orderNo = "ORDER-123"
        val customerId = 1L
        val orderType = "PURCHASE"
        val status = "PAID"
        val totalAmount = 15000
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
    }

    @Test
    fun `OrderDetailApiResponse createdAt null 테스트`() {
        // Given & When
        val response = OrderDetailApiResponse(
            orderId = UUID.randomUUID(),
            orderNo = "ORDER-123",
            customerId = 1L,
            orderType = "PURCHASE",
            status = "PAID",
            totalAmount = 15000
        )

        // Then
        assertNull(response.createdAt)
    }

    @Test
    fun `OrderSummaryApiResponse 생성 테스트`() {
        // Given
        val orderId = UUID.randomUUID()
        val orderNo = "ORDER-456"
        val customerId = 2L
        val orderType = "DELIVERY"
        val status = "PENDING"
        val totalAmount = 25000
        val createdAt = LocalDateTime.now()

        // When
        val response = OrderSummaryApiResponse(
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
    }

    @Test
    fun `OrderSummaryApiResponse createdAt null 테스트`() {
        // Given & When
        val response = OrderSummaryApiResponse(
            orderId = UUID.randomUUID(),
            orderNo = "ORDER-456",
            customerId = 2L,
            orderType = "DELIVERY",
            status = "PENDING",
            totalAmount = 25000
        )

        // Then
        assertNull(response.createdAt)
    }

    @Test
    fun `OrderInfo 생성 테스트`() {
        // Given
        val id = UUID.randomUUID()
        val orderNo = "ORDER-789"
        val customerId = 3L
        val totalAmount = 35000
        val status = "COMPLETED"
        val orderType = "PICKUP"
        val createdAt = LocalDateTime.now()

        // When
        val orderInfo = OrderInfo(
            id = id,
            orderNo = orderNo,
            customerId = customerId,
            totalAmount = totalAmount,
            status = status,
            orderType = orderType,
            createdAt = createdAt
        )

        // Then
        assertEquals(id, orderInfo.id)
        assertEquals(orderNo, orderInfo.orderNo)
        assertEquals(customerId, orderInfo.customerId)
        assertEquals(totalAmount, orderInfo.totalAmount)
        assertEquals(status, orderInfo.status)
        assertEquals(orderType, orderInfo.orderType)
        assertEquals(createdAt, orderInfo.createdAt)
    }

    @Test
    fun `OrderInfo copy 메서드 테스트`() {
        // Given
        val original = OrderInfo(
            id = UUID.randomUUID(),
            orderNo = "ORDER-789",
            customerId = 3L,
            totalAmount = 35000,
            status = "PENDING",
            orderType = "PICKUP",
            createdAt = LocalDateTime.now()
        )

        // When
        val copied = original.copy(status = "COMPLETED", totalAmount = 40000)

        // Then
        assertEquals(original.id, copied.id)
        assertEquals(original.orderNo, copied.orderNo)
        assertEquals(original.customerId, copied.customerId)
        assertEquals(40000, copied.totalAmount)
        assertEquals("COMPLETED", copied.status)
        assertEquals(original.orderType, copied.orderType)
        assertEquals(original.createdAt, copied.createdAt)
    }

    @Test
    fun `다양한 타입의 ApiResponse 테스트`() {
        // Given & When
        val stringResponse = ApiResponse<String>(data = "test")
        val intResponse = ApiResponse<Int>(data = 123)
        val listResponse = ApiResponse<List<String>>(data = listOf("a", "b", "c"))

        // Then
        assertEquals("test", stringResponse.data)
        assertEquals(123, intResponse.data)
        assertEquals(3, listResponse.data?.size)
    }
}