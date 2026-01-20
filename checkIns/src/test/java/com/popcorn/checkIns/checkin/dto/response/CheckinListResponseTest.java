package com.popcorn.checkIns.checkin.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("체크인 목록 응답 DTO 테스트")
class CheckinListResponseTest {

	@Test
	@DisplayName("빌더 패턴으로 체크인 목록 응답 생성")
	void builder_createsCheckinListResponse() {
		// Given
		UUID checkinId1 = UUID.fromString("90000000-0000-0000-0000-000000000001");
		UUID orderId1 = UUID.fromString("40000000-0000-0000-0000-000000000001");
		LocalDateTime createdAt1 = LocalDateTime.now();

		UUID checkinId2 = UUID.fromString("90000000-0000-0000-0000-000000000002");
		UUID orderId2 = UUID.fromString("40000000-0000-0000-0000-000000000002");
		LocalDateTime createdAt2 = LocalDateTime.now().minusMinutes(10);

		CheckinListResponse.Item item1 = CheckinListResponse.Item.builder()
				.checkinId(checkinId1)
				.orderId(orderId1)
				.qrCode("qr-code-1")
				.createdAt(createdAt1)
				.build();

		CheckinListResponse.Item item2 = CheckinListResponse.Item.builder()
				.checkinId(checkinId2)
				.orderId(orderId2)
				.qrCode("qr-code-2")
				.createdAt(createdAt2)
				.build();

		List<CheckinListResponse.Item> items = Arrays.asList(item1, item2);

		// When
		CheckinListResponse response = CheckinListResponse.builder()
				.count(2)
				.items(items)
				.build();

		// Then
		assertThat(response.getCount()).isEqualTo(2);
		assertThat(response.getItems()).hasSize(2);
		assertThat(response.getItems().get(0)).isEqualTo(item1);
		assertThat(response.getItems().get(1)).isEqualTo(item2);
	}

	@Test
	@DisplayName("기본 생성자로 객체 생성")
	void noArgsConstructor_createsObject() {
		CheckinListResponse response = new CheckinListResponse();

		assertThat(response).isNotNull();
		assertThat(response.getCount()).isEqualTo(0); // primitive int 기본값
		assertThat(response.getItems()).isNull();
	}

	@Test
	@DisplayName("모든 인수 생성자로 객체 생성")
	void allArgsConstructor_createsObjectCorrectly() {
		List<CheckinListResponse.Item> items = Arrays.asList();

		CheckinListResponse response = new CheckinListResponse(0, items);

		assertThat(response.getCount()).isEqualTo(0);
		assertThat(response.getItems()).isEqualTo(items);
	}

	@Test
	@DisplayName("빈 목록으로 응답 생성")
	void builder_createsEmptyResponse() {
		CheckinListResponse response = CheckinListResponse.builder()
				.count(0)
				.items(Arrays.asList())
				.build();

		assertThat(response.getCount()).isEqualTo(0);
		assertThat(response.getItems()).isEmpty();
	}

	@DisplayName("체크인 목록 아이템 테스트")
	static class ItemTest {

		@Test
		@DisplayName("빌더 패턴으로 아이템 생성")
		void builder_createsItem() {
			// Given
			UUID checkinId = UUID.fromString("90000000-0000-0000-0000-000000000001");
			UUID orderId = UUID.fromString("40000000-0000-0000-0000-000000000001");
			String qrCode = "item-qr-code";
			LocalDateTime createdAt = LocalDateTime.now();

			// When
			CheckinListResponse.Item item = CheckinListResponse.Item.builder()
					.checkinId(checkinId)
					.orderId(orderId)
					.qrCode(qrCode)
					.createdAt(createdAt)
					.build();

			// Then
			assertThat(item.getCheckinId()).isEqualTo(checkinId);
			assertThat(item.getOrderId()).isEqualTo(orderId);
			assertThat(item.getQrCode()).isEqualTo(qrCode);
			assertThat(item.getCreatedAt()).isEqualTo(createdAt);
		}

		@Test
		@DisplayName("기본 생성자로 아이템 생성")
		void noArgsConstructor_createsItem() {
			CheckinListResponse.Item item = new CheckinListResponse.Item();

			assertThat(item).isNotNull();
			assertThat(item.getCheckinId()).isNull();
			assertThat(item.getOrderId()).isNull();
			assertThat(item.getQrCode()).isNull();
			assertThat(item.getCreatedAt()).isNull();
		}

		@Test
		@DisplayName("모든 인수 생성자로 아이템 생성")
		void allArgsConstructor_createsItemCorrectly() {
			// Given
			UUID checkinId = UUID.fromString("90000000-0000-0000-0000-000000000003");
			UUID orderId = UUID.fromString("40000000-0000-0000-0000-000000000003");
			String qrCode = "constructor-qr-code";
			LocalDateTime createdAt = LocalDateTime.now();

			// When
			CheckinListResponse.Item item = new CheckinListResponse.Item(
					checkinId, orderId, qrCode, createdAt
			);

			// Then
			assertThat(item.getCheckinId()).isEqualTo(checkinId);
			assertThat(item.getOrderId()).isEqualTo(orderId);
			assertThat(item.getQrCode()).isEqualTo(qrCode);
			assertThat(item.getCreatedAt()).isEqualTo(createdAt);
		}

		@Test
		@DisplayName("아이템 부분적으로 빌드 가능")
		void builder_partialItemBuild() {
			UUID checkinId = UUID.randomUUID();

			CheckinListResponse.Item item = CheckinListResponse.Item.builder()
					.checkinId(checkinId)
					.build();

			assertThat(item.getCheckinId()).isEqualTo(checkinId);
			assertThat(item.getOrderId()).isNull();
			assertThat(item.getQrCode()).isNull();
			assertThat(item.getCreatedAt()).isNull();
		}
	}

	@Test
	@DisplayName("카운트와 아이템 개수 불일치 허용")
	void response_allowsCountMismatch() {
		CheckinListResponse.Item item = CheckinListResponse.Item.builder()
				.checkinId(UUID.randomUUID())
				.build();

		CheckinListResponse response = CheckinListResponse.builder()
				.count(10) // 실제 아이템 개수와 다름
				.items(Arrays.asList(item))
				.build();

		assertThat(response.getCount()).isEqualTo(10);
		assertThat(response.getItems()).hasSize(1);
	}
}