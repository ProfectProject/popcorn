package com.popcorn.store.domain.popup.repository.owner.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.popcorn.store.domain.popup.entity.PopupSchedule;
import com.popcorn.store.domain.popup.repository.owner.view.OwnerPopupScheduleView;

public interface JpaOwnerPopupScheduleRepository extends JpaRepository<PopupSchedule, UUID> {

	@Modifying
	@Query(value = "insert into popup_schedules "
			+ "(schedule_id, popup_id, start_at, end_at, price, capacity, remaining_capacity, is_active, "
			+ "created_at, updated_at, deleted_at, created_by, updated_by, deleted_by) "
			+ "values (:scheduleId, :popupId, :startAt, :endAt, :price, :capacity, :remainingCapacity, :active, "
			+ ":now, :now, null, :createdBy, :updatedBy, null)", nativeQuery = true)
	void insertSchedule(@Param("scheduleId") UUID scheduleId,
			@Param("popupId") UUID popupId,
			@Param("startAt") LocalDateTime startAt,
			@Param("endAt") LocalDateTime endAt,
			@Param("price") Integer price,
			@Param("capacity") Integer capacity,
			@Param("remainingCapacity") Integer remainingCapacity,
			@Param("active") boolean active,
			@Param("now") LocalDateTime now,
			@Param("createdBy") Long createdBy,
			@Param("updatedBy") Long updatedBy);

	@Modifying
	@Query(value = "update popup_schedules set "
			+ "start_at = coalesce(:startAt, start_at), "
			+ "end_at = coalesce(:endAt, end_at), "
			+ "price = coalesce(:price, price), "
			+ "capacity = coalesce(:capacity, capacity), "
			+ "remaining_capacity = case when :capacity is null then remaining_capacity else :capacity end, "
			+ "is_active = coalesce(:active, is_active), "
			+ "updated_at = :now, updated_by = :updatedBy "
			+ "where schedule_id = :scheduleId and popup_id = :popupId and deleted_at is null", nativeQuery = true)
	int updateSchedule(@Param("scheduleId") UUID scheduleId,
			@Param("popupId") UUID popupId,
			@Param("startAt") LocalDateTime startAt,
			@Param("endAt") LocalDateTime endAt,
			@Param("price") Integer price,
			@Param("capacity") Integer capacity,
			@Param("active") Boolean active,
			@Param("now") LocalDateTime now,
			@Param("updatedBy") Long updatedBy);

	@Modifying
	@Query(value = "update popup_schedules set "
			+ "deleted_at = :now, deleted_by = :deletedBy, "
			+ "updated_at = :now, updated_by = :deletedBy, "
			+ "is_active = false "
			+ "where schedule_id = :scheduleId and popup_id = :popupId and deleted_at is null", nativeQuery = true)
	int softDeleteSchedule(@Param("scheduleId") UUID scheduleId,
			@Param("popupId") UUID popupId,
			@Param("now") LocalDateTime now,
			@Param("deletedBy") Long deletedBy);

	@Modifying
	@Query(value = "update popup_schedules set "
			+ "deleted_at = :now, deleted_by = :deletedBy, "
			+ "updated_at = :now, updated_by = :deletedBy, "
			+ "is_active = false "
			+ "where popup_id = :popupId and deleted_at is null", nativeQuery = true)
	int softDeleteSchedulesByPopup(@Param("popupId") UUID popupId,
			@Param("now") LocalDateTime now,
			@Param("deletedBy") Long deletedBy);

	@Modifying
	@Query(value = "update popup_schedules set "
			+ "is_active = false, updated_at = :now, updated_by = :updatedBy "
			+ "where popup_id = :popupId and is_active = true and deleted_at is null", nativeQuery = true)
	int deactivateActiveSchedulesByPopup(@Param("popupId") UUID popupId,
			@Param("now") LocalDateTime now,
			@Param("updatedBy") Long updatedBy);

	@Query("select s.id as scheduleId, s.startAt as startAt, s.endAt as endAt, "
			+ "s.price as price, s.capacity as capacity, s.remainingCapacity as remainingCapacity, "
			+ "s.active as active "
			+ "from PopupSchedule s where s.popupId = :popupId and s.deletedAt is null")
	List<OwnerPopupScheduleView> findSchedulesByPopup(@Param("popupId") UUID popupId);

	@Query("select (count(s) > 0) from PopupSchedule s "
			+ "where s.id = :scheduleId and s.popupId = :popupId and s.deletedAt is null")
	boolean existsSchedule(@Param("scheduleId") UUID scheduleId, @Param("popupId") UUID popupId);;
}
