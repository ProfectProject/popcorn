package com.popcorn.demo.domain.popup.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.popup.dto.owner.request.CreatePopupRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.CreatePopupScheduleRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.UpdatePopupRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.UpdatePopupScheduleRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.UpdatePopupStatusRequest;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupCreatedDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupDetailDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupStatusUpdatedDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupUpdatedDto;
import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
import com.popcorn.demo.domain.popup.exception.PopupException;
import com.popcorn.demo.domain.popup.repository.owner.OwnerPopupRepository;
import com.popcorn.demo.domain.popup.repository.owner.OwnerPopupScheduleRepository;
import com.popcorn.demo.domain.popup.repository.owner.view.OwnerPopupScheduleView;
import com.popcorn.demo.domain.popup.service.owner.OwnerPopupService;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@Sql(scripts = "classpath:sql/test-schema.sql")
@Transactional
class OwnerPopupServiceIntegrationTest {

	@Autowired
	private OwnerPopupService ownerPopupService;

	@Autowired
	private OwnerPopupRepository ownerPopupRepository;

	@Autowired
	private OwnerPopupScheduleRepository ownerPopupScheduleRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("팝업 생성 시 REQUEST 상태로 저장되고 스케줄이 함께 생성된다")
	void 팝업_생성_시_REQUEST_스케줄_동시_생성() {
		Long ownerId = 1001L;
		UUID storeId = createStore(ownerId, "owner-store");

		CreatePopupRequest request = CreatePopupRequest.builder()
				.storeId(storeId)
				.title("테스트 팝업")
				.description("설명")
				.category(PopupCategory.FOOD)
				.schedules(List.of(CreatePopupScheduleRequest.builder()
						.startAt(LocalDateTime.now().plusDays(1))
						.endAt(LocalDateTime.now().plusDays(2))
						.price(10000)
						.capacity(50)
						.build()))
				.build();

		PopupCreatedDto created = ownerPopupService.createPopup(ownerId, request);

		assertThat(created.getPopupId()).isNotNull();
		assertThat(created.getStatus()).isEqualTo(PopupStatus.REQUEST);

		List<OwnerPopupScheduleView> schedules = ownerPopupScheduleRepository.findSchedulesByPopup(created.getPopupId());
		assertThat(schedules).hasSize(1);
		assertThat(schedules.get(0).getRemainingCapacity()).isEqualTo(50);
		assertThat(schedules.get(0).isActive()).isFalse();
	}

	// TdDo 추후 주석 삭제
//	@Test
//	@DisplayName("팝업 상태 변경은 REQUEST 상태에서 허용되지 않는다")
//	void 팝업_상태_변경_REQUEST_차단() {
//		Long ownerId = 1002L;
//		UUID storeId = createStore(ownerId, "owner-store-2");
//
//		PopupCreatedDto created = ownerPopupService.createPopup(ownerId, createPopupRequest(storeId, "팝업1"));
//
//		UpdatePopupStatusRequest request = UpdatePopupStatusRequest.builder()
//				.status(PopupStatus.CLOSED)
//				.build();
//
//		assertThatThrownBy(() -> ownerPopupService.updatePopupStatus(ownerId, created.getPopupId(), request))
//				.isInstanceOf(PopupException.class);
//	}

	@Test
	@DisplayName("팝업 상태 변경 시 활성 스케줄이 비활성 처리된다")
	void 팝업_상태_변경_시_스케줄_비활성() {
		Long ownerId = 1003L;
		UUID storeId = createStore(ownerId, "owner-store-3");

		PopupCreatedDto created = ownerPopupService.createPopup(ownerId, createPopupRequest(storeId, "팝업2"));
		UUID popupId = created.getPopupId();

		OwnerPopupScheduleView schedule = ownerPopupScheduleRepository.findSchedulesByPopup(popupId).get(0);

		UpdatePopupRequest updateRequest = UpdatePopupRequest.builder()
				.updateSchedules(List.of(UpdatePopupScheduleRequest.builder()
						.scheduleId(schedule.getScheduleId())
						.active(true)
						.build()))
				.build();
		ownerPopupService.updatePopup(ownerId, popupId, updateRequest);

		updatePopupStatusDirect(popupId, PopupStatus.APPROVED);
		entityManager.clear();

		UpdatePopupStatusRequest statusRequest = UpdatePopupStatusRequest.builder()
				.status(PopupStatus.CLOSED)
				.build();
		PopupStatusUpdatedDto updated = ownerPopupService.updatePopupStatus(ownerId, popupId, statusRequest);

		assertThat(updated.getStatus()).isEqualTo(PopupStatus.CLOSED);
		List<OwnerPopupScheduleView> schedules = ownerPopupScheduleRepository.findSchedulesByPopup(popupId);
		assertThat(schedules).hasSize(1);
		assertThat(schedules.get(0).isActive()).isFalse();
	}

	@Test
	@DisplayName("팝업 수정 시 스케줄 추가/수정/삭제가 함께 반영된다")
	void 팝업_수정_스케줄_추가_수정_삭제() {
		Long ownerId = 1004L;
		UUID storeId = createStore(ownerId, "owner-store-4");

		PopupCreatedDto created = ownerPopupService.createPopup(ownerId, createPopupRequest(storeId, "팝업3"));
		UUID popupId = created.getPopupId();

		OwnerPopupScheduleView schedule = ownerPopupScheduleRepository.findSchedulesByPopup(popupId).get(0);

		UpdatePopupRequest request = UpdatePopupRequest.builder()
				.title("팝업3-수정")
				.popupCategory(PopupCategory.ART)
				.createSchedules(List.of(CreatePopupScheduleRequest.builder()
						.startAt(LocalDateTime.now().plusDays(3))
						.endAt(LocalDateTime.now().plusDays(4))
						.price(20000)
						.capacity(100)
						.build()))
				.updateSchedules(List.of(UpdatePopupScheduleRequest.builder()
						.scheduleId(schedule.getScheduleId())
						.price(15000)
						.capacity(70)
						.build()))
				.deleteScheduleIds(List.of(schedule.getScheduleId()))
				.build();

		PopupUpdatedDto updated = ownerPopupService.updatePopup(ownerId, popupId, request);

		assertThat(updated.getTitle()).isEqualTo("팝업3-수정");
		assertThat(updated.getPopupCategory()).isEqualTo(PopupCategory.ART);

		List<OwnerPopupScheduleView> schedules = ownerPopupScheduleRepository.findSchedulesByPopup(popupId);
		assertThat(schedules).hasSize(1);
		assertThat(schedules.get(0).getPrice()).isEqualTo(20000);
	}

	@Test
	@DisplayName("팝업 삭제 시 스케줄도 소프트 삭제된다")
	void 팝업_삭제_시_스케줄_소프트_삭제() {
		Long ownerId = 1005L;
		UUID storeId = createStore(ownerId, "owner-store-5");

		PopupCreatedDto created = ownerPopupService.createPopup(ownerId, createPopupRequest(storeId, "팝업4"));

		ownerPopupService.deletePopup(ownerId, created.getPopupId());

		Integer scheduleCount = jdbcTemplate.queryForObject(
				"select count(*) from p_popup_schedules where popup_id = ? and deleted_at is not null",
				Integer.class,
				created.getPopupId());
		assertThat(scheduleCount).isEqualTo(1);
	}

	@Test
	@DisplayName("팝업 상세 조회 시 스케줄 목록이 포함된다")
	void 팝업_상세_조회_스케줄_포함() {
		Long ownerId = 1006L;
		UUID storeId = createStore(ownerId, "owner-store-6");

		PopupCreatedDto created = ownerPopupService.createPopup(ownerId, createPopupRequest(storeId, "팝업5"));

		PopupDetailDto detail = ownerPopupService.getPopupDetail(ownerId, created.getPopupId());

		assertThat(detail.getPopupId()).isEqualTo(created.getPopupId());
		assertThat(detail.getSchedules()).hasSize(1);
	}

	private UUID createStore(Long ownerId, String name) {
		ensureOwner(ownerId);
		UUID storeId = UUID.randomUUID();
		LocalDateTime now = LocalDateTime.now();
		jdbcTemplate.update(
				"insert into p_stores (store_id, user_id, store_name, status, reason, deleted_at, deleted_by, created_at, updated_at, created_by, updated_by) "
						+ "values (?, ?, ?, cast(? as store_status), null, null, null, ?, ?, ?, ?)",
				storeId, ownerId, name, "ACTIVE", now, now, ownerId, ownerId);
		return storeId;
	}

	private void ensureOwner(Long ownerId) {
		LocalDateTime now = LocalDateTime.now();
		Integer existing = jdbcTemplate.queryForObject(
				"select count(*) from p_users where user_id = ?",
				Integer.class,
				ownerId);
		if (existing != null && existing > 0) {
			return;
		}
		jdbcTemplate.update(
				"insert into p_users (user_id, email, password, name, role, is_active, created_at, updated_at) "
						+ "values (?, ?, ?, ?, cast(? as user_role), true, ?, ?)",
				ownerId,
				"owner_" + ownerId + "@test.com",
				"password",
				"테스트 오너",
				"OWNER",
				now,
				now);
	}

	private CreatePopupRequest createPopupRequest(UUID storeId, String title) {
		return CreatePopupRequest.builder()
				.storeId(storeId)
				.title(title)
				.description("설명")
				.category(PopupCategory.FOOD)
				.schedules(List.of(CreatePopupScheduleRequest.builder()
						.startAt(LocalDateTime.now().plusDays(1))
						.endAt(LocalDateTime.now().plusDays(2))
						.price(10000)
						.capacity(50)
						.build()))
				.build();
	}

	private void updatePopupStatusDirect(UUID popupId, PopupStatus status) {
		jdbcTemplate.update(
				"update p_popups set status = cast(? as popup_status) where popup_id = ?",
				status.name(),
				popupId);
	}
}
