package com.popcorn.checkIns.checkin.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

/**
 * CheckinRepository 단위 테스트
 * JDBC 기반 체크인 리포지토리의 모든 메소드를 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
class CheckinRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CheckinRepository checkinRepository;

    private UUID testOrderId;
    private UUID testOrderQrCodeId;
    private UUID testCheckinId;
    private Long testCreatedBy;
    private LocalDateTime testCreatedAt;
    private String testQrCode;
    private CheckinRow testCheckinRow;

    @BeforeEach
    void setUp() {
        testOrderId = UUID.randomUUID();
        testOrderQrCodeId = UUID.randomUUID();
        testCheckinId = UUID.randomUUID();
        testCreatedBy = 12345L;
        testCreatedAt = LocalDateTime.now();
        testQrCode = "QR_CHECKIN_123";
        testCheckinRow = new CheckinRow(
                testCheckinId,
                testOrderId,
                testOrderQrCodeId,
                testQrCode,
                testCreatedAt,
                testCreatedBy
        );
    }

    @Nested
    @DisplayName("체크인 삽입 테스트")
    class InsertTest {

        @Test
        @DisplayName("정상적으로 체크인을 삽입하고 체크인 ID를 반환한다")
        void insert_Success() {
            // Given
            when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any()))
                    .thenReturn(1);

            // When
            UUID result = checkinRepository.insert(testOrderId, testOrderQrCodeId, testCreatedBy, testCreatedAt);

            // Then
            assertThat(result).isNotNull();
            verify(jdbcTemplate).update(
                    contains("INSERT INTO qr.qr_checkins"),
                    eq(result), // 생성된 UUID
                    eq(testOrderId),
                    eq(testOrderQrCodeId),
                    any(Timestamp.class),
                    eq(testCreatedBy)
            );
        }

        @Test
        @DisplayName("생성자 정보가 null인 체크인을 삽입한다")
        void insert_WithNullCreatedBy() {
            // Given
            when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), isNull()))
                    .thenReturn(1);

            // When
            UUID result = checkinRepository.insert(testOrderId, testOrderQrCodeId, null, testCreatedAt);

            // Then
            assertThat(result).isNotNull();
            verify(jdbcTemplate).update(
                    contains("INSERT INTO qr.qr_checkins"),
                    eq(result),
                    eq(testOrderId),
                    eq(testOrderQrCodeId),
                    any(Timestamp.class),
                    isNull()
            );
        }

        @Test
        @DisplayName("생성 시간이 null인 체크인을 삽입한다")
        void insert_WithNullCreatedAt() {
            // Given
            when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any()))
                    .thenReturn(1);

            // When
            UUID result = checkinRepository.insert(testOrderId, testOrderQrCodeId, testCreatedBy, null);

            // Then
            assertThat(result).isNotNull();
            verify(jdbcTemplate).update(
                    contains("INSERT INTO qr.qr_checkins"),
                    eq(result),
                    eq(testOrderId),
                    eq(testOrderQrCodeId),
                    isNull(), // null LocalDateTime이 null Timestamp로 변환됨
                    eq(testCreatedBy)
            );
        }
    }

    @Nested
    @DisplayName("QR 코드 ID로 체크인 조회 테스트")
    class FindLatestByOrderQrCodeIdTest {

        @Test
        @DisplayName("정상적으로 최신 체크인을 조회한다")
        void findLatestByOrderQrCodeId_Success() {
            // Given
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(testOrderQrCodeId)))
                    .thenReturn(List.of(testCheckinRow));

            // When
            Optional<CheckinRow> result = checkinRepository.findLatestByOrderQrCodeId(testOrderQrCodeId);

            // Then
            assertThat(result).isPresent();
            CheckinRow row = result.get();
            assertThat(row.checkinId()).isEqualTo(testCheckinId);
            assertThat(row.orderId()).isEqualTo(testOrderId);
            assertThat(row.orderQrCodeId()).isEqualTo(testOrderQrCodeId);
            assertThat(row.qrCode()).isEqualTo(testQrCode);
            assertThat(row.createdBy()).isEqualTo(testCreatedBy);

            verify(jdbcTemplate).query(
                    contains("JOIN qr.qr_order_qr_codes q ON q.qr_id = c.order_qr_code_id"),
                    any(RowMapper.class),
                    eq(testOrderQrCodeId)
            );
        }

        @Test
        @DisplayName("존재하지 않는 QR 코드 ID로 조회 시 빈 결과를 반환한다")
        void findLatestByOrderQrCodeId_NotFound() {
            // Given
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(testOrderQrCodeId)))
                    .thenReturn(List.of());

            // When
            Optional<CheckinRow> result = checkinRepository.findLatestByOrderQrCodeId(testOrderQrCodeId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("전체 체크인 조회 테스트")
    class FindAllTest {

        @Test
        @DisplayName("제한된 수만큼 체크인 목록을 조회한다")
        void findAll_WithLimit() {
            // Given
            int limit = 10;
            List<CheckinRow> expectedCheckins = List.of(testCheckinRow);
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(limit)))
                    .thenReturn(expectedCheckins);

            // When
            List<CheckinRow> result = checkinRepository.findAll(limit);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testCheckinRow);
            verify(jdbcTemplate).query(
                    contains("LIMIT ?"),
                    any(RowMapper.class),
                    eq(limit)
            );
        }

        @Test
        @DisplayName("체크인이 없는 경우 빈 목록을 반환한다")
        void findAll_EmptyResult() {
            // Given
            int limit = 10;
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(limit)))
                    .thenReturn(List.of());

            // When
            List<CheckinRow> result = checkinRepository.findAll(limit);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("0개 제한으로 조회할 수 있다")
        void findAll_ZeroLimit() {
            // Given
            int limit = 0;
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(limit)))
                    .thenReturn(List.of());

            // When
            List<CheckinRow> result = checkinRepository.findAll(limit);

            // Then
            assertThat(result).isEmpty();
            verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq(0));
        }
    }

    @Nested
    @DisplayName("체크인 ID로 조회 테스트")
    class FindByIdTest {

        @Test
        @DisplayName("정상적으로 체크인 ID로 조회한다")
        void findById_Success() {
            // Given
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(testCheckinId)))
                    .thenReturn(List.of(testCheckinRow));

            // When
            Optional<CheckinRow> result = checkinRepository.findById(testCheckinId);

            // Then
            assertThat(result).isPresent();
            CheckinRow row = result.get();
            assertThat(row.checkinId()).isEqualTo(testCheckinId);
            verify(jdbcTemplate).query(
                    contains("WHERE c.checkin_id = ?"),
                    any(RowMapper.class),
                    eq(testCheckinId)
            );
        }

        @Test
        @DisplayName("존재하지 않는 체크인 ID로 조회 시 빈 결과를 반환한다")
        void findById_NotFound() {
            // Given
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(testCheckinId)))
                    .thenReturn(List.of());

            // When
            Optional<CheckinRow> result = checkinRepository.findById(testCheckinId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("유틸리티 메소드 테스트")
    class UtilityMethodTest {

        @Test
        @DisplayName("null LocalDateTime을 null Timestamp로 변환한다")
        void nullLocalDateTimeToTimestamp() {
            // Given & When
            UUID result = checkinRepository.insert(testOrderId, testOrderQrCodeId, testCreatedBy, null);

            // Then
            assertThat(result).isNotNull();
            verify(jdbcTemplate).update(
                    anyString(),
                    any(UUID.class),
                    eq(testOrderId),
                    eq(testOrderQrCodeId),
                    isNull(), // null LocalDateTime이 null Timestamp로 변환
                    eq(testCreatedBy)
            );
        }

        @Test
        @DisplayName("RowMapper가 올바르게 CheckinRow를 생성한다")
        void rowMapperCreatesCheckinRowCorrectly() {
            // Given - createdBy가 null인 경우도 테스트
            CheckinRow checkinWithNullCreatedBy = new CheckinRow(
                    testCheckinId,
                    testOrderId,
                    testOrderQrCodeId,
                    testQrCode,
                    testCreatedAt,
                    null // createdBy가 null
            );

            when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(testCheckinId)))
                    .thenReturn(List.of(checkinWithNullCreatedBy));

            // When
            Optional<CheckinRow> result = checkinRepository.findById(testCheckinId);

            // Then
            assertThat(result).isPresent();
            CheckinRow row = result.get();
            assertThat(row.checkinId()).isEqualTo(testCheckinId);
            assertThat(row.orderId()).isEqualTo(testOrderId);
            assertThat(row.orderQrCodeId()).isEqualTo(testOrderQrCodeId);
            assertThat(row.qrCode()).isEqualTo(testQrCode);
            assertThat(row.createdAt()).isEqualTo(testCreatedAt);
            assertThat(row.createdBy()).isNull(); // null createdBy 처리 확인
        }

        @Test
        @DisplayName("여러 체크인이 있을 때 첫 번째를 반환한다")
        void returnsFirstWhenMultipleResults() {
            // Given
            CheckinRow secondCheckin = new CheckinRow(
                    UUID.randomUUID(),
                    testOrderId,
                    testOrderQrCodeId,
                    testQrCode,
                    testCreatedAt.plusMinutes(10),
                    testCreatedBy
            );

            when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(testOrderQrCodeId)))
                    .thenReturn(List.of(testCheckinRow, secondCheckin));

            // When
            Optional<CheckinRow> result = checkinRepository.findLatestByOrderQrCodeId(testOrderQrCodeId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().checkinId()).isEqualTo(testCheckinId); // 첫 번째 체크인 반환
        }
    }
}