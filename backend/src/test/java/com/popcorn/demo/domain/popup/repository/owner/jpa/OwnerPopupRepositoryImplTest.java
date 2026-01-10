package com.popcorn.demo.domain.popup.repository.owner.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.domain.popup.entity.Popup;
import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;

class OwnerPopupRepositoryImplTest {

	@Test
	@DisplayName("스토어/상태/카테고리 필터링과 페이지 처리가 동작한다")
	void ownerPopupRepositoryFilters() {
		JpaOwnerPopupRepository jpa = Mockito.mock(JpaOwnerPopupRepository.class);
		OwnerPopupRepositoryImpl repo = new OwnerPopupRepositoryImpl(jpa);

		UUID storeId = UUID.randomUUID();
		UUID otherStoreId = UUID.randomUUID();

		Popup activeFood = popup(storeId, PopupCategory.FOOD, PopupStatus.OPEN, null);
		Popup deleted = popup(storeId, PopupCategory.FOOD, PopupStatus.OPEN, LocalDateTime.now());
		Popup art = popup(storeId, PopupCategory.ART, PopupStatus.CLOSED, null);
		Popup otherStore = popup(otherStoreId, PopupCategory.FOOD, PopupStatus.OPEN, null);

		when(jpa.findAll()).thenReturn(List.of(activeFood, deleted, art, otherStore));

		assertThat(repo.findAllByStoreIdAndDeletedAtIsNull(storeId))
				.containsExactly(activeFood, art);
		assertThat(repo.findByPublishStatusAndStoreId(PopupStatus.OPEN, storeId))
				.containsExactly(activeFood, deleted);
		assertThat(repo.findByPopupCategoryAndStoreId(PopupCategory.ART, storeId))
				.containsExactly(art);
		assertThat(repo.countByStoreId(storeId)).isEqualTo(3);
		assertThat(repo.countByPopupStatus(PopupStatus.OPEN)).isEqualTo(3);
		assertThat(repo.countByPopupCategory(PopupCategory.FOOD)).isEqualTo(3);
	}

	@Test
	@DisplayName("카테고리 필터와 페이지네이션이 적용된다")
	void findOwnedPopupsWithPagination() {
		JpaOwnerPopupRepository jpa = Mockito.mock(JpaOwnerPopupRepository.class);
		OwnerPopupRepositoryImpl repo = new OwnerPopupRepositoryImpl(jpa);
		UUID storeId = UUID.randomUUID();

		Popup food1 = popup(storeId, PopupCategory.FOOD, PopupStatus.OPEN, null);
		Popup food2 = popup(storeId, PopupCategory.FOOD, PopupStatus.OPEN, null);
		Popup art = popup(storeId, PopupCategory.ART, PopupStatus.OPEN, null);

		when(jpa.findOwnedPopupsByStore(storeId, 1L)).thenReturn(List.of(food1, food2, art));

		assertThat(repo.findOwnedPopupsByStoreWithPagination(storeId, 1L, 1, 1, "FOOD"))
				.containsExactly(food1);
		assertThat(repo.findOwnedPopupsByStoreWithPagination(storeId, 1L, 1, 10, "INVALID"))
				.isEmpty();
	}

	@Test
	@DisplayName("저장/조회/삭제는 JPA 저장소에 위임한다")
	void basicDelegation() {
		JpaOwnerPopupRepository jpa = Mockito.mock(JpaOwnerPopupRepository.class);
		OwnerPopupRepositoryImpl repo = new OwnerPopupRepositoryImpl(jpa);
		UUID popupId = UUID.randomUUID();
		Popup popup = popup(UUID.randomUUID(), PopupCategory.FOOD, PopupStatus.OPEN, null);

		when(jpa.save(popup)).thenReturn(popup);
		when(jpa.findById(popupId)).thenReturn(Optional.of(popup));

		assertThat(repo.save(popup)).isEqualTo(popup);
		assertThat(repo.findById(popupId)).contains(popup);
	}

	private Popup popup(UUID storeId, PopupCategory category, PopupStatus status, LocalDateTime deletedAt) {
		Popup popup = Popup.builder()
				.storeId(storeId)
				.title("title")
				.description("desc")
				.category(category)
				.status(status)
				.build();
		popup.setDeletedAt(deletedAt);
		return popup;
	}
}
