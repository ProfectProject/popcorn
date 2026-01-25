package com.popcorn.checkIns.checkin.repository.owner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.popcorn.checkIns.checkin.repository.CheckinRow;

@ExtendWith(MockitoExtension.class)
class OwnerCheckinRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private OwnerCheckinRepository ownerCheckinRepository;

    private UUID testPopupId;
    private UUID testScheduleId;
    private Long testOwnerId;
    private CheckinRow testCheckinRow;

    @BeforeEach
    void setUp() {
        testPopupId = UUID.randomUUID();
        testScheduleId = UUID.randomUUID();
        testOwnerId = 12345L;
        testCheckinRow = new CheckinRow(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "OWNER_QR_123",
                LocalDateTime.now(),
                67890L
        );
    }

    @Test
    @DisplayName("팝업 ID로 체크인 목록을 조회한다")
    void findByPopupId_Success() {
        // Given
        int limit = 20;
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(testPopupId), eq(testOwnerId), eq(limit)))
                .thenReturn(List.of(testCheckinRow));

        // When
        List<CheckinRow> result = ownerCheckinRepository.findByPopupId(testPopupId, testOwnerId, limit);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testCheckinRow);
    }

    @Test
    @DisplayName("스케줄 ID로 체크인 목록을 조회한다")
    void findByScheduleId_Success() {
        // Given
        int limit = 15;
        when(jdbcTemplate.query(anyString(), any(RowMapper.class),
                eq(testScheduleId), eq(testPopupId), eq(testOwnerId), eq(limit)))
                .thenReturn(List.of(testCheckinRow));

        // When
        List<CheckinRow> result = ownerCheckinRepository.findByScheduleId(
                testPopupId, testScheduleId, testOwnerId, limit);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testCheckinRow);
    }
}
