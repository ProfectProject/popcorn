package com.popcorn.payment.common

import com.popcorn.common.dto.BaseResponse
import com.popcorn.common.entity.BaseEntity
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 공통 클래스들에 대한 포괄적 테스트
 *
 * [커버리지 향상을 위한 테스트]
 * - BaseEntity, BaseResponse 등 공통 클래스 테스트
 * - 모든 메서드와 프로퍼티 커버
 * - 80% 커버리지 달성에 기여하는 핵심 테스트
 */
class CommonClassesTest {

    // 테스트용 BaseEntity 구현체
    class TestEntity : BaseEntity() {
        var name: String = "test"
    }

    @Test
    fun `BaseEntity 기본 동작 테스트`() {
        // Given
        val entity = TestEntity()
        val beforeTime = LocalDateTime.now().minusSeconds(1)

        // When & Then - 초기 상태
        assertNotNull(entity.createdAt)
        assertNotNull(entity.updatedAt)
        assertFalse(entity.isDeleted)
        assertTrue(entity.createdAt.isAfter(beforeTime))
        assertTrue(entity.updatedAt.isAfter(beforeTime))
    }

    @Test
    fun `BaseEntity delete 메서드 테스트`() {
        // Given
        val entity = TestEntity()
        val beforeDelete = LocalDateTime.now()
        assertFalse(entity.isDeleted)

        // When
        Thread.sleep(10) // 시간 차이를 위해
        entity.delete()

        // Then
        assertTrue(entity.isDeleted)
        assertTrue(entity.updatedAt.isAfter(beforeDelete))
    }

    @Test
    fun `BaseEntity updatedAt 자동 갱신 테스트`() {
        // Given
        val entity = TestEntity()
        val originalUpdatedAt = entity.updatedAt

        // When
        Thread.sleep(10) // 시간 차이를 위해
        entity.delete() // updatedAt이 갱신되는 동작

        // Then
        assertTrue(entity.updatedAt.isAfter(originalUpdatedAt))
    }

    @Test
    fun `BaseResponse 성공 응답 테스트`() {
        // Given
        val testData = "테스트 데이터"
        val message = "성공 메시지"

        // When
        val response = BaseResponse(
            data = testData,
            message = message,
            status = "SUCCESS"
        )

        // Then
        assertEquals(testData, response.data)
        assertEquals(message, response.message)
        assertEquals("SUCCESS", response.status)
    }

    @Test
    fun `BaseResponse 기본값 테스트`() {
        // When
        val response = BaseResponse<String>()

        // Then
        assertEquals(null, response.data)
        assertEquals("", response.message)
        assertEquals("SUCCESS", response.status)
    }

    @Test
    fun `BaseResponse 다양한 데이터 타입 테스트`() {
        val testCases = listOf(
            "문자열",
            42,
            true,
            listOf(1, 2, 3),
            mapOf("key" to "value"),
            LocalDateTime.now()
        )

        testCases.forEach { testData ->
            // When
            val response = BaseResponse(
                data = testData,
                message = "테스트 메시지",
                status = "SUCCESS"
            )

            // Then
            assertEquals(testData, response.data)
            assertEquals("테스트 메시지", response.message)
            assertEquals("SUCCESS", response.status)
            assertNotNull(response.toString())

            // equals 테스트
            val same = BaseResponse(testData, "테스트 메시지", "SUCCESS")
            assertEquals(response, same)
            assertEquals(response.hashCode(), same.hashCode())

            // copy 테스트
            val copied = response.copy(status = "MODIFIED")
            assertEquals("MODIFIED", copied.status)
            assertEquals(testData, copied.data)
        }
    }

    @Test
    fun `BaseResponse 오류 상태 테스트`() {
        val errorStatuses = listOf("ERROR", "FAILED", "TIMEOUT", "INVALID", "UNAUTHORIZED")

        errorStatuses.forEach { status ->
            // When
            val response = BaseResponse<String>(
                data = null,
                message = "오류가 발생했습니다: $status",
                status = status
            )

            // Then
            assertEquals(null, response.data)
            assertTrue(response.message.contains(status))
            assertEquals(status, response.status)
            assertNotNull(response.toString())
        }
    }

    @Test
    fun `BaseEntity 상태 변화 시나리오 테스트`() {
        // Given
        val entity = TestEntity()
        val initialCreatedAt = entity.createdAt
        val initialUpdatedAt = entity.updatedAt

        // When - 데이터 변경
        Thread.sleep(10)
        entity.name = "changed"

        // Then - createdAt은 변하지 않음
        assertEquals(initialCreatedAt, entity.createdAt)

        // When - 삭제 처리
        Thread.sleep(10)
        entity.delete()

        // Then
        assertTrue(entity.isDeleted)
        assertTrue(entity.updatedAt.isAfter(initialUpdatedAt))
        assertEquals(initialCreatedAt, entity.createdAt) // 여전히 변하지 않음
    }

    @Test
    fun `BaseEntity 여러 엔티티 독립성 테스트`() {
        // Given
        val entity1 = TestEntity()
        val entity2 = TestEntity()

        // When
        Thread.sleep(10)
        entity1.delete()
        entity2.name = "different"

        // Then - 각각 독립적
        assertTrue(entity1.isDeleted)
        assertFalse(entity2.isDeleted)
        assertEquals("test", entity1.name)
        assertEquals("different", entity2.name)
    }

    @Test
    fun `BaseResponse null 안전성 테스트`() {
        // Given & When
        val response = BaseResponse<String?>(
            data = null,
            message = "",
            status = "NULL_TEST"
        )

        // Then
        assertEquals(null, response.data)
        assertEquals("", response.message)
        assertEquals("NULL_TEST", response.status)
        assertNotNull(response.toString()) // null data와도 잘 동작

        // copy with different values
        val copied = response.copy(data = null, message = "modified message")
        assertEquals(null, copied.data)
        assertEquals("modified message", copied.message)
    }

    @Test
    fun `BaseResponse hashCode 일관성 테스트`() {
        // Given
        val data = "consistent data"
        val message = "consistent message"
        val status = "CONSISTENT"

        // When - 동일한 데이터로 여러 번 생성
        val responses = (1..10).map {
            BaseResponse(data, message, status)
        }

        // Then - 모든 hashCode가 동일해야 함
        val firstHashCode = responses.first().hashCode()
        responses.forEach { response ->
            assertEquals(firstHashCode, response.hashCode())
            assertEquals(responses.first(), response)
        }
    }

    @Test
    fun `BaseEntity 시간 정밀도 테스트`() {
        // Given
        val entities = mutableListOf<TestEntity>()

        // When - 빠르게 여러 엔티티 생성
        repeat(5) {
            entities.add(TestEntity())
            Thread.sleep(1) // 최소 지연
        }

        // Then - 생성 시간이 순차적이어야 함 (또는 같을 수 있음)
        for (i in 1 until entities.size) {
            val prev = entities[i-1].createdAt
            val current = entities[i].createdAt
            assertTrue(current.isAfter(prev) || current.isEqual(prev))
        }
    }

    @Test
    fun `BaseResponse 복합 데이터 구조 테스트`() {
        // Given
        val complexData = mapOf(
            "users" to listOf(
                mapOf("id" to 1, "name" to "User1"),
                mapOf("id" to 2, "name" to "User2")
            ),
            "metadata" to mapOf(
                "total" to 2,
                "page" to 1,
                "hasMore" to false
            )
        )

        // When
        val response = BaseResponse(
            data = complexData,
            message = "복합 데이터 조회 성공",
            status = "SUCCESS"
        )

        // Then
        assertEquals(complexData, response.data)
        assertNotNull(response.toString())

        // 복합 데이터가 제대로 저장되고 접근 가능한지 확인
        val users = (response.data as Map<String, Any>)["users"] as List<Map<String, Any>>
        assertEquals(2, users.size)
        assertEquals("User1", users[0]["name"])
    }
}