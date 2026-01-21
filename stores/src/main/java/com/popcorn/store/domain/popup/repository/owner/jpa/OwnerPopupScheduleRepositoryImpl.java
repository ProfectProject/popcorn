package com.popcorn.store.domain.popup.repository.owner.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.popcorn.store.domain.popup.repository.owner.OwnerPopupScheduleRepository;
import com.popcorn.store.domain.popup.repository.owner.view.OwnerPopupScheduleView;

@Repository
public class OwnerPopupScheduleRepositoryImpl implements OwnerPopupScheduleRepository {

	private final JpaOwnerPopupScheduleRepository jpaOwnerPopupScheduleRepository;

	public OwnerPopupScheduleRepositoryImpl(JpaOwnerPopupScheduleRepository jpaOwnerPopupScheduleRepository) {
		this.jpaOwnerPopupScheduleRepository = jpaOwnerPopupScheduleRepository;
	}

	@Override
	public void insertSchedule(UUID scheduleId, UUID popupId, LocalDateTime startAt, LocalDateTime endAt,
			Integer price, Integer capacity, Integer remainingCapacity, boolean active,
			LocalDateTime now, Long createdBy, Long updatedBy) {
		jpaOwnerPopupScheduleRepository.insertSchedule(scheduleId, popupId, startAt, endAt, price, capacity,
				remainingCapacity, active, now, createdBy, updatedBy);
	}

	@Override
	public int updateSchedule(UUID scheduleId, UUID popupId, LocalDateTime startAt, LocalDateTime endAt,
			Integer price, Integer capacity, Boolean active, LocalDateTime now, Long updatedBy) {
		return jpaOwnerPopupScheduleRepository.updateSchedule(scheduleId, popupId, startAt, endAt, price,
				capacity, active, now, updatedBy);
	}

	@Override
	public int softDeleteSchedule(UUID scheduleId, UUID popupId, LocalDateTime now, Long deletedBy) {
		return jpaOwnerPopupScheduleRepository.softDeleteSchedule(scheduleId, popupId, now, deletedBy);
	}

	@Override
	public int softDeleteSchedulesByPopup(UUID popupId, LocalDateTime now, Long deletedBy) {
		return jpaOwnerPopupScheduleRepository.softDeleteSchedulesByPopup(popupId, now, deletedBy);
	}

	@Override
	public int deactivateActiveSchedulesByPopup(UUID popupId, LocalDateTime now, Long updatedBy) {
		return jpaOwnerPopupScheduleRepository.deactivateActiveSchedulesByPopup(popupId, now, updatedBy);
	}

	@Override
	public List<OwnerPopupScheduleView> findSchedulesByPopup(UUID popupId) {
		return jpaOwnerPopupScheduleRepository.findSchedulesByPopup(popupId);
	}

	@Override
	public boolean existsSchedule(UUID scheduleId, UUID popupId) {
		return jpaOwnerPopupScheduleRepository.existsSchedule(scheduleId, popupId);
	}
}
