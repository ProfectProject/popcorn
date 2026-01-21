package com.popcorn.store.domain.popup.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PopupEventHandler {

	private static final Logger log = LoggerFactory.getLogger(PopupEventHandler.class);

	@EventListener
	public void handleCreated(PopupCreatedEvent event) {
		log.info("🧾 팝업 생성 이벤트 - ownerId: {}, popupId: {}, storeId: {}",
				event.getOwnerId(), event.getPopup().getId(), event.getPopup().getStoreId());
	}

	@EventListener
	public void handleUpdated(PopupUpdatedEvent event) {
		log.info("🛠️ 팝업 수정 이벤트 - ownerId: {}, popupId: {}, title: {}",
				event.getOwnerId(), event.getPopup().getId(), event.getPopup().getTitle());
	}

	@EventListener
	public void handleStatusUpdated(PopupStatusUpdatedEvent event) {
		log.info("📢 팝업 상태 변경 이벤트 - ownerId: {}, popupId: {}, status: {}",
				event.getOwnerId(), event.getPopup().getId(), event.getPopup().getStatus());
	}

	@EventListener
	public void handleDeleted(PopupDeletedEvent event) {
		log.info("🗑️ 팝업 삭제 이벤트 - ownerId: {}, popupId: {}, storeId: {}",
				event.getOwnerId(), event.getPopup().getId(), event.getPopup().getStoreId());
	}

	@EventListener
	public void handleScheduleCreated(PopupScheduleCreatedEvent event) {
		log.info("🗓️ 팝업 일정 생성 이벤트 - ownerId: {}, popupId: {}, scheduleId: {}, startAt: {}, endAt: {}",
				event.getOwnerId(), event.getPopupId(), event.getScheduleId(), event.getStartAt(), event.getEndAt());
	}

	@EventListener
	public void handleScheduleUpdated(PopupScheduleUpdatedEvent event) {
		log.info("🗓️ 팝업 일정 수정 이벤트 - ownerId: {}, popupId: {}, scheduleId: {}, active: {}",
				event.getOwnerId(), event.getPopupId(), event.getScheduleId(), event.getActive());
	}

	@EventListener
	public void handleScheduleDeleted(PopupScheduleDeletedEvent event) {
		log.info("🗓️ 팝업 일정 삭제 이벤트 - ownerId: {}, popupId: {}, scheduleId: {}",
				event.getOwnerId(), event.getPopupId(), event.getScheduleId());
	}
}
