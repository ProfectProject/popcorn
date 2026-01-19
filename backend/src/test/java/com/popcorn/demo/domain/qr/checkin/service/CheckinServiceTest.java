package com.popcorn.demo.domain.qr.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.domain.qr.checkin.dto.response.CheckinDetailResponse;
import com.popcorn.demo.domain.qr.checkin.dto.response.CheckinListResponse;
import com.popcorn.demo.domain.qr.checkin.exception.CheckinException;
import com.popcorn.demo.domain.qr.checkin.repository.CheckinRepository;
import com.popcorn.demo.domain.qr.checkin.repository.CheckinRow;

@DisplayName("체크인 서비스 테스트")
class CheckinServiceTest {

	private CheckinRepository checkinRepository;
	private CheckinService checkinService;

	@BeforeEach
	void setUp() {
		checkinRepository = Mockito.mock(CheckinRepository.class);
		checkinService = new CheckinService(checkinRepository);
	}

	@Test
	@DisplayName("체크인 목록 조회")
	void getCheckins_success() {
		UUID checkinId = UUID.fromString("90000000-0000-0000-0000-000000000201");
		UUID orderId = UUID.fromString("40000000-0000-0000-0000-000000000006");
		CheckinRow row = new CheckinRow(
				checkinId,
				orderId,
				UUID.fromString("80000000-0000-0000-0000-000000000002"),
				"qr-list-002",
				LocalDateTime.now(),
				1001L
		);

		when(checkinRepository.findAll(20)).thenReturn(List.of(row));

		CheckinListResponse response = checkinService.getCheckins(20);

		assertThat(response.getCount()).isEqualTo(1);
		assertThat(response.getItems().get(0).getCheckinId()).isEqualTo(checkinId);
	}

	@Test
	@DisplayName("체크인 상세 조회")
	void getCheckin_success() {
		UUID checkinId = UUID.fromString("90000000-0000-0000-0000-000000000202");
		UUID orderId = UUID.fromString("40000000-0000-0000-0000-000000000007");
		CheckinRow row = new CheckinRow(
				checkinId,
				orderId,
				UUID.fromString("80000000-0000-0000-0000-000000000003"),
				"qr-detail-002",
				LocalDateTime.now(),
				1002L
		);

		when(checkinRepository.findById(checkinId)).thenReturn(Optional.of(row));

		CheckinDetailResponse response = checkinService.getCheckin(checkinId);

		assertThat(response.getCheckinId()).isEqualTo(checkinId);
		assertThat(response.getOrderId()).isEqualTo(orderId);
	}

	@Test
	@DisplayName("체크인 상세 조회 실패 - 없음")
	void getCheckin_notFound() {
		UUID checkinId = UUID.fromString("90000000-0000-0000-0000-000000000203");

		when(checkinRepository.findById(checkinId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> checkinService.getCheckin(checkinId))
				.isInstanceOf(CheckinException.class);
	}
}
