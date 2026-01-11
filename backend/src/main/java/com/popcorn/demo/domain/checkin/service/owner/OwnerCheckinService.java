package com.popcorn.demo.domain.checkin.service.owner;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.checkin.dto.owner.response.OwnerCheckinListResponse;
import com.popcorn.demo.domain.checkin.exception.owner.OwnerCheckinException;
import com.popcorn.demo.domain.checkin.repository.CheckinRow;
import com.popcorn.demo.domain.checkin.repository.owner.OwnerCheckinRepository;
import com.popcorn.demo.domain.popup.exception.PopupException;
import com.popcorn.demo.domain.popup.repository.owner.OwnerPopupRepository;
import com.popcorn.demo.domain.popup.repository.owner.OwnerPopupScheduleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OwnerCheckinService {

	private final OwnerCheckinRepository ownerCheckinRepository;
	private final OwnerPopupRepository ownerPopupRepository;
	private final OwnerPopupScheduleRepository ownerPopupScheduleRepository;

	@Transactional(readOnly = true)
	public OwnerCheckinListResponse getCheckinsByPopup(Long ownerId, UUID popupId) {
		validateOwner(ownerId);
		validatePopupId(popupId);

		ownerPopupRepository.findOwnedPopup(popupId, ownerId)
				.orElseThrow(PopupException::popupNotFound);

		List<CheckinRow> rows = ownerCheckinRepository.findByPopupId(popupId, ownerId);
		List<OwnerCheckinListResponse.Item> items = rows.stream()
				.map(row -> OwnerCheckinListResponse.Item.builder()
						.checkinId(row.checkinId())
						.orderId(row.orderId())
						.qrCode(row.qrCode())
						.createdAt(row.createdAt())
						.build())
				.toList();

		return OwnerCheckinListResponse.builder()
				.count(items.size())
				.items(items)
				.build();
	}

	@Transactional(readOnly = true)
	public OwnerCheckinListResponse getCheckinsBySchedule(Long ownerId, UUID popupId, UUID scheduleId) {
		validateOwner(ownerId);
		validatePopupId(popupId);
		validateScheduleId(scheduleId);

		ownerPopupRepository.findOwnedPopup(popupId, ownerId)
				.orElseThrow(PopupException::popupNotFound);

		if (!ownerPopupScheduleRepository.existsSchedule(scheduleId, popupId)) {
			throw OwnerCheckinException.scheduleNotFound();
		}

		List<CheckinRow> rows = ownerCheckinRepository.findByScheduleId(popupId, scheduleId, ownerId);
		List<OwnerCheckinListResponse.Item> items = rows.stream()
				.map(row -> OwnerCheckinListResponse.Item.builder()
						.checkinId(row.checkinId())
						.orderId(row.orderId())
						.qrCode(row.qrCode())
						.createdAt(row.createdAt())
						.build())
				.toList();

		return OwnerCheckinListResponse.builder()
				.count(items.size())
				.items(items)
				.build();
	}

	private void validateOwner(Long ownerId) {
		if (ownerId == null || ownerId <= 0) {
			throw OwnerCheckinException.userIdRequired();
		}
	}

	private void validatePopupId(UUID popupId) {
		if (popupId == null) {
			throw OwnerCheckinException.popupIdRequired();
		}
	}

	private void validateScheduleId(UUID scheduleId) {
		if (scheduleId == null) {
			throw OwnerCheckinException.scheduleIdRequired();
		}
	}
}
