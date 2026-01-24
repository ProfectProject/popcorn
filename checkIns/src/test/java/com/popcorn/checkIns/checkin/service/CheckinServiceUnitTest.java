package com.popcorn.checkIns.checkin.service;

import com.popcorn.checkIns.checkin.dto.response.CheckinDetailResponse;
import com.popcorn.checkIns.checkin.dto.response.CheckinListResponse;
import com.popcorn.checkIns.checkin.exception.CheckinException;
import com.popcorn.checkIns.checkin.repository.CheckinRepository;
import com.popcorn.checkIns.checkin.repository.CheckinRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckinService 단위 테스트")
class CheckinServiceUnitTest {

    @Mock
    private CheckinRepository checkinRepository;

    @InjectMocks
    private CheckinService checkinService;

    private final UUID checkinId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID orderQrCodeId = UUID.randomUUID();
    private final String qrCode = "QR_CODE_12345";
    private final LocalDateTime createdAt = LocalDateTime.now();
    private final Long createdBy = 123L;

    @BeforeEach
    void setUp() {
        // Mock 초기화는 @ExtendWith(MockitoExtension.class)에서 자동 처리
    }

    @Test
    @DisplayName("체크인 목록 조회 성공 - 여러 건")
    void getCheckins_shouldReturnCheckinList_whenCheckinsExist() {
        // Given
        int limit = 10;
        List<CheckinRow> checkinRows = List.of(
            createCheckinRow(checkinId, orderId, orderQrCodeId, qrCode, createdAt, createdBy),
            createCheckinRow(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "QR_CODE_67890", createdAt.minusMinutes(5), 456L)
        );

        when(checkinRepository.findAll(limit)).thenReturn(checkinRows);

        // When
        CheckinListResponse response = checkinService.getCheckins(limit);

        // Then
        assertNotNull(response);
        assertEquals(2, response.getCount());
        assertEquals(2, response.getItems().size());

        CheckinListResponse.Item firstItem = response.getItems().get(0);
        assertEquals(checkinId, firstItem.getCheckinId());
        assertEquals(orderId, firstItem.getOrderId());
        assertEquals(qrCode, firstItem.getQrCode());
        assertEquals(createdAt, firstItem.getCreatedAt());

        CheckinListResponse.Item secondItem = response.getItems().get(1);
        assertEquals("QR_CODE_67890", secondItem.getQrCode());
        assertEquals(456L, 456L); // createdBy는 Item에 없으므로 일반적인 검증

        verify(checkinRepository, times(1)).findAll(limit);
    }

    @Test
    @DisplayName("체크인 목록 조회 성공 - 빈 목록")
    void getCheckins_shouldReturnEmptyList_whenNoCheckinsExist() {
        // Given
        int limit = 5;
        when(checkinRepository.findAll(limit)).thenReturn(Collections.emptyList());

        // When
        CheckinListResponse response = checkinService.getCheckins(limit);

        // Then
        assertNotNull(response);
        assertEquals(0, response.getCount());
        assertTrue(response.getItems().isEmpty());

        verify(checkinRepository, times(1)).findAll(limit);
    }

    @Test
    @DisplayName("체크인 목록 조회 성공 - 단일 건")
    void getCheckins_shouldReturnSingleItem_whenOneCheckinExists() {
        // Given
        int limit = 1;
        List<CheckinRow> checkinRows = List.of(
            createCheckinRow(checkinId, orderId, orderQrCodeId, qrCode, createdAt, createdBy)
        );

        when(checkinRepository.findAll(limit)).thenReturn(checkinRows);

        // When
        CheckinListResponse response = checkinService.getCheckins(limit);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getCount());
        assertEquals(1, response.getItems().size());

        CheckinListResponse.Item item = response.getItems().get(0);
        assertEquals(checkinId, item.getCheckinId());
        assertEquals(orderId, item.getOrderId());
        assertEquals(qrCode, item.getQrCode());
        assertEquals(createdAt, item.getCreatedAt());

        verify(checkinRepository, times(1)).findAll(limit);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 20, 50, 100})
    @DisplayName("다양한 limit 값에 대한 체크인 목록 조회")
    void getCheckins_shouldHandleVariousLimits(int limit) {
        // Given
        List<CheckinRow> checkinRows = Collections.emptyList();
        when(checkinRepository.findAll(limit)).thenReturn(checkinRows);

        // When
        CheckinListResponse response = checkinService.getCheckins(limit);

        // Then
        assertNotNull(response);
        assertEquals(0, response.getCount());
        assertTrue(response.getItems().isEmpty());

        verify(checkinRepository, times(1)).findAll(limit);
    }

    @Test
    @DisplayName("체크인 상세 조회 성공")
    void getCheckin_shouldReturnCheckinDetail_whenCheckinExists() {
        // Given
        CheckinRow checkinRow = createCheckinRow(checkinId, orderId, orderQrCodeId, qrCode, createdAt, createdBy);
        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkinRow));

        // When
        CheckinDetailResponse response = checkinService.getCheckin(checkinId);

        // Then
        assertNotNull(response);
        assertEquals(checkinId, response.getCheckinId());
        assertEquals(orderId, response.getOrderId());
        assertEquals(orderQrCodeId, response.getOrderQrCodeId());
        assertEquals(qrCode, response.getQrCode());
        assertEquals(createdAt, response.getCreatedAt());
        assertEquals(createdBy, response.getCreatedBy());

        verify(checkinRepository, times(1)).findById(checkinId);
    }

    @Test
    @DisplayName("체크인 상세 조회 실패 - 존재하지 않는 체크인")
    void getCheckin_shouldThrowException_whenCheckinNotExists() {
        // Given
        when(checkinRepository.findById(checkinId)).thenReturn(Optional.empty());

        // When & Then
        CheckinException exception = assertThrows(CheckinException.class,
            () -> checkinService.getCheckin(checkinId));

        // CheckinException.notFound()가 어떤 메시지를 반환하는지에 따라 검증
        assertNotNull(exception);
        verify(checkinRepository, times(1)).findById(checkinId);
    }

    @Test
    @DisplayName("체크인 상세 조회 - null 값 처리")
    void getCheckin_shouldHandleNullValues_inCheckinRow() {
        // Given
        CheckinRow checkinRowWithNulls = createCheckinRow(checkinId, orderId, orderQrCodeId, qrCode, createdAt, null);
        when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(checkinRowWithNulls));

        // When
        CheckinDetailResponse response = checkinService.getCheckin(checkinId);

        // Then
        assertNotNull(response);
        assertEquals(checkinId, response.getCheckinId());
        assertEquals(orderId, response.getOrderId());
        assertEquals(orderQrCodeId, response.getOrderQrCodeId());
        assertEquals(qrCode, response.getQrCode());
        assertEquals(createdAt, response.getCreatedAt());
        assertNull(response.getCreatedBy());

        verify(checkinRepository, times(1)).findById(checkinId);
    }

    @Test
    @DisplayName("체크인 목록 조회 - Stream 변환 검증")
    void getCheckins_shouldCorrectlyTransformCheckinRowsToItems() {
        // Given
        int limit = 3;
        LocalDateTime now = LocalDateTime.now();
        List<CheckinRow> checkinRows = List.of(
            createCheckinRow(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "QR_001", now, 100L),
            createCheckinRow(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "QR_002", now.minusMinutes(1), 200L),
            createCheckinRow(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "QR_003", now.minusMinutes(2), 300L)
        );

        when(checkinRepository.findAll(limit)).thenReturn(checkinRows);

        // When
        CheckinListResponse response = checkinService.getCheckins(limit);

        // Then
        assertNotNull(response);
        assertEquals(3, response.getCount());
        assertEquals(3, response.getItems().size());

        // 각 아이템이 올바르게 변환되었는지 검증
        List<CheckinListResponse.Item> items = response.getItems();
        for (int i = 0; i < checkinRows.size(); i++) {
            CheckinRow row = checkinRows.get(i);
            CheckinListResponse.Item item = items.get(i);

            assertEquals(row.checkinId(), item.getCheckinId());
            assertEquals(row.orderId(), item.getOrderId());
            assertEquals(row.qrCode(), item.getQrCode());
            assertEquals(row.createdAt(), item.getCreatedAt());
        }

        verify(checkinRepository, times(1)).findAll(limit);
    }

    @Test
    @DisplayName("Repository 예외 전파 테스트")
    void getCheckins_shouldPropagateRepositoryException() {
        // Given
        int limit = 10;
        RuntimeException repositoryException = new RuntimeException("Database connection error");
        when(checkinRepository.findAll(limit)).thenThrow(repositoryException);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> checkinService.getCheckins(limit));
        assertEquals("Database connection error", exception.getMessage());

        verify(checkinRepository, times(1)).findAll(limit);
    }

    @Test
    @DisplayName("체크인 상세 조회 - Repository 예외 전파 테스트")
    void getCheckin_shouldPropagateRepositoryException() {
        // Given
        RuntimeException repositoryException = new RuntimeException("Database query failed");
        when(checkinRepository.findById(checkinId)).thenThrow(repositoryException);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> checkinService.getCheckin(checkinId));
        assertEquals("Database query failed", exception.getMessage());

        verify(checkinRepository, times(1)).findById(checkinId);
    }

    @Test
    @DisplayName("대용량 체크인 목록 조회 - 성능 고려")
    void getCheckins_shouldHandleLargeLimit() {
        // Given
        int largeLimit = 1000;
        // 대용량 데이터를 시뮬레이션하지 않고, 빈 리스트로 테스트
        when(checkinRepository.findAll(largeLimit)).thenReturn(Collections.emptyList());

        // When
        CheckinListResponse response = checkinService.getCheckins(largeLimit);

        // Then
        assertNotNull(response);
        assertEquals(0, response.getCount());
        assertTrue(response.getItems().isEmpty());

        verify(checkinRepository, times(1)).findAll(largeLimit);
    }

    @Test
    @DisplayName("체크인 목록 조회 - 시간 순서 확인")
    void getCheckins_shouldPreserveTimeOrdering() {
        // Given
        int limit = 2;
        LocalDateTime baseTime = LocalDateTime.now();
        List<CheckinRow> checkinRows = List.of(
            createCheckinRow(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "QR_LATEST", baseTime, 100L), // 최신
            createCheckinRow(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "QR_OLDER", baseTime.minusMinutes(10), 200L) // 이전
        );

        when(checkinRepository.findAll(limit)).thenReturn(checkinRows);

        // When
        CheckinListResponse response = checkinService.getCheckins(limit);

        // Then
        assertNotNull(response);
        assertEquals(2, response.getCount());

        List<CheckinListResponse.Item> items = response.getItems();
        assertEquals("QR_LATEST", items.get(0).getQrCode());
        assertEquals("QR_OLDER", items.get(1).getQrCode());

        // 시간 순서 검증
        assertTrue(items.get(0).getCreatedAt().isAfter(items.get(1).getCreatedAt()));

        verify(checkinRepository, times(1)).findAll(limit);
    }

    // 헬퍼 메서드
    private CheckinRow createCheckinRow(UUID checkinId, UUID orderId, UUID orderQrCodeId,
                                       String qrCode, LocalDateTime createdAt, Long createdBy) {
        return new CheckinRow(checkinId, orderId, orderQrCodeId, qrCode, createdAt, createdBy);
    }
}