package com.popcorn.demo.domain.store.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StoreTest {

	@Test
	@DisplayName("스토어 생성 시 기본값 설정")
	void 스토어_생성_시_기본값_설정() {
		Store store = Store.builder()
				.name("테스트 스토어")
				.ownerId(123L)
				.publishStatus(StorePublishStatus.DRAFT)
				.createdBy(123L)
				.updatedBy(123L)
				.build();

		assertThat(store.getName()).isEqualTo("테스트 스토어");
		assertThat(store.getOwnerId()).isEqualTo(123L);
		assertThat(store.getPublishStatus()).isEqualTo(StorePublishStatus.DRAFT);
		assertThat(store.getCreatedBy()).isEqualTo(123L);
		assertThat(store.getUpdatedBy()).isEqualTo(123L);
		assertThat(store.isDeleted()).isFalse();
		assertThat(store.isDraft()).isTrue();
		assertThat(store.isActive()).isFalse();
	}

	@Test
	@DisplayName("스토어 이름 변경")
	void 스토어_이름_변경() {
		Store store = Store.builder()
				.name("원래 이름")
				.ownerId(123L)
				.publishStatus(StorePublishStatus.DRAFT)
				.build();

		store.updateName("변경된 이름");

		assertThat(store.getName()).isEqualTo("변경된 이름");
	}

	@Test
	@DisplayName("스토어 발행 상태 변경")
	void 스토어_발행_상태_변경() {
		Store store = Store.builder()
				.name("테스트 스토어")
				.ownerId(123L)
				.publishStatus(StorePublishStatus.DRAFT)
				.build();

		store.updatePublishStatus(StorePublishStatus.ACTIVE);

		assertThat(store.getPublishStatus()).isEqualTo(StorePublishStatus.ACTIVE);
		assertThat(store.isActive()).isTrue();
		assertThat(store.isDraft()).isFalse();
	}

	@Test
	@DisplayName("스토어 소프트 삭제")
	void 스토어_소프트_삭제() {
		Store store = Store.builder()
				.name("삭제될 스토어")
				.ownerId(123L)
				.publishStatus(StorePublishStatus.DRAFT)
				.build();

		Long deleterId = 456L;

		store.delete(deleterId);

		assertThat(store.isDeleted()).isTrue();
		assertThat(store.getDeletedAt()).isNotNull();
		assertThat(store.getDeletedBy()).isEqualTo(deleterId);
		assertThat(store.getDeletedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
	}

	@Test
	@DisplayName("스토어 오너 확인")
	void 스토어_오너_확인() {
		Long ownerId = 123L;
		Store store = Store.builder()
				.name("오너 테스트 스토어")
				.ownerId(ownerId)
				.publishStatus(StorePublishStatus.DRAFT)
				.build();

		assertThat(store.isOwner(ownerId)).isTrue();
		assertThat(store.isOwner(456L)).isFalse();
		assertThat(store.isOwner(null)).isFalse();
	}

	@Test
	@DisplayName("스토어 ACTIVE 상태 확인")
	void 스토어_ACTIVE_상태_확인() {
		Store store = Store.builder()
				.name("테스트 스토어")
				.ownerId(123L)
				.publishStatus(StorePublishStatus.ACTIVE)
				.build();

		assertThat(store.isActive()).isTrue();
		assertThat(store.isDraft()).isFalse();
	}
}
