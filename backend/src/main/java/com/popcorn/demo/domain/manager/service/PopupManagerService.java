package com.popcorn.demo.domain.manager.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.manager.handler.ApprovalNotAllowedException;
import com.popcorn.demo.domain.manager.handler.NotFoundException;
import com.popcorn.demo.domain.popup.dto.manager.OrderCancelRequest;
import com.popcorn.demo.domain.popup.dto.manager.OrderCancelResponse;
import com.popcorn.demo.domain.popup.dto.manager.OrderDetailResponse;
import com.popcorn.demo.domain.popup.dto.manager.OrderListResponse;
import com.popcorn.demo.domain.popup.dto.manager.OrderStatusUpdateRequest;
import com.popcorn.demo.domain.popup.dto.manager.OrderStatusUpdateResponse;
import com.popcorn.demo.domain.popup.dto.manager.PendingStoreListResponse;
import com.popcorn.demo.domain.popup.dto.manager.StoreApproveRequest;
import com.popcorn.demo.domain.popup.dto.manager.StoreApproveResponse;
import com.popcorn.demo.domain.popup.dto.manager.StoreForceStopRequest;
import com.popcorn.demo.domain.popup.dto.manager.StoreForceStopResponse;
import com.popcorn.demo.domain.popup.dto.manager.StoreRejectRequest;
import com.popcorn.demo.domain.popup.dto.manager.StoreRejectResponse;
import com.popcorn.demo.domain.popup.dto.manager.StoreWithdrawRequest;
import com.popcorn.demo.domain.popup.dto.manager.StoreWithdrawResponse;
import com.popcorn.demo.domain.popup.entity.Popup;
import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
import com.popcorn.demo.domain.popup.repository.PopupQueryRepository;
import com.popcorn.demo.domain.popup.repository.owner.OwnerPopupRepository;
import com.popcorn.demo.domain.popup.repository.owner.OwnerPopupScheduleRepository;
import com.popcorn.demo.domain.popup.repository.view.PopupListView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PopupManagerService {

	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final OwnerPopupRepository ownerPopupRepository;
	private final OwnerPopupScheduleRepository ownerPopupScheduleRepository;
	private final PopupQueryRepository popupQueryRepository;

	@Transactional
	public OrderCancelResponse cancelPopup(OrderCancelRequest request, String status) {
		if (request == null || request.getPopupId() == null) {
			throw new IllegalArgumentException("팝업 ID가 필요합니다.");
		}

		Popup popup = findPopup(request.getPopupId());
		if (popup.getStatus() != PopupStatus.APPROVED && popup.getStatus() != PopupStatus.OPEN) {
			throw new ApprovalNotAllowedException("승인된 팝업만 취소할 수 있습니다.");
		}

		PopupStatus targetStatus = parseStatus(status);
		if (targetStatus == PopupStatus.REQUEST) {
			throw new IllegalArgumentException("요청 상태로 변경할 수 없습니다.");
		}

		Popup saved = applyStatusChange(popup, targetStatus, request.getManagerId());
		return OrderCancelResponse.builder()
				.popupId(saved.getId())
				.status(saved.getStatus())
				.reason(request.getReason())
				.updatedAt(saved.getUpdatedAt())
				.updatedBy(saved.getUpdatedBy())
				.build();
	}

	@Transactional
	public OrderStatusUpdateResponse updatePopupStatus(UUID popupId, OrderStatusUpdateRequest request) {
		if (popupId == null) {
			throw new IllegalArgumentException("팝업 ID가 필요합니다.");
		}
		if (request == null || request.getStatus() == null) {
			throw new IllegalArgumentException("변경할 상태가 필요합니다.");
		}
		if (request.getStatus() == PopupStatus.REQUEST) {
			throw new IllegalArgumentException("요청 상태로 변경할 수 없습니다.");
		}

		Popup popup = findPopup(popupId);
		Popup saved = applyStatusChange(popup, request.getStatus(), request.getManagerId());
		return OrderStatusUpdateResponse.builder()
				.popupId(saved.getId())
				.status(saved.getStatus())
				.updatedAt(saved.getUpdatedAt())
				.updatedBy(saved.getUpdatedBy())
				.build();
	}

	@Transactional(readOnly = true)
	public OrderDetailResponse getPopupDetail(UUID popupId) {
		if (popupId == null) {
			throw new IllegalArgumentException("팝업 ID가 필요합니다.");
		}

		PopupListView view = popupQueryRepository.findPopupDetail(popupId)
				.orElseThrow(() -> new NotFoundException("팝업 정보를 찾을 수 없습니다."));

		return OrderDetailResponse.builder()
				.popupId(toUuid(view.getId()))
				.storeId(toUuid(view.getStoreId()))
				.title(view.getTitle())
				.description(view.getDescription())
				.popupCategory(toCategory(view.getCategory()))
				.status(toStatus(view.getStatus()))
				.eventStartAt(view.getEventStartAt())
				.eventEndAt(view.getEventEndAt())
				.build();
	}

	@Transactional(readOnly = true)
	public OrderListResponse getPopupsByStatus(String status, Integer page, Integer size) {
		PopupStatus targetStatus = parseStatus(status);
		int normalizedPage = normalizePage(page);
		int normalizedSize = normalizeSize(size);
		long offset = (long) (normalizedPage - 1) * normalizedSize;

		List<PopupListView> views = popupQueryRepository.findPopupsByStatus(
				targetStatus.name(),
				normalizedSize,
				offset
		);
		long total = popupQueryRepository.countPopupsByStatus(targetStatus.name());

		List<OrderListResponse.ItemDto> items = views.stream()
				.map(view -> OrderListResponse.ItemDto.builder()
						.popupId(toUuid(view.getId()))
						.storeId(toUuid(view.getStoreId()))
						.title(view.getTitle())
						.popupCategory(toCategory(view.getCategory()))
						.status(toStatus(view.getStatus()))
						.eventStartAt(view.getEventStartAt())
						.eventEndAt(view.getEventEndAt())
						.build())
				.toList();

		return OrderListResponse.builder()
				.items(items)
				.page(normalizedPage)
				.size(normalizedSize)
				.total(total)
				.build();
	}

	@Transactional
	public StoreForceStopResponse forceStopPopup(UUID popupId, StoreForceStopRequest request) {
		Popup popup = findPopup(popupId);
		if (popup.getStatus() == PopupStatus.CANCELLED || popup.getStatus() == PopupStatus.CLOSED) {
			throw new ApprovalNotAllowedException("이미 중단된 팝업입니다.");
		}

		Long managerId = request == null ? null : request.getManagerId();
		Popup saved = applyStatusChange(popup, PopupStatus.CANCELLED, managerId);
		return StoreForceStopResponse.builder()
				.popupId(saved.getId())
				.status(saved.getStatus())
				.updatedAt(saved.getUpdatedAt())
				.updatedBy(saved.getUpdatedBy())
				.build();
	}

	@Transactional
	public StoreApproveResponse approvePopup(UUID popupId, StoreApproveRequest request) {
		Popup popup = findPopup(popupId);
		if (popup.getStatus() != PopupStatus.REQUEST) {
			throw new ApprovalNotAllowedException("승인 대기 상태가 아닙니다.");
		}

		Long managerId = request == null ? null : request.getManagerId();
		Popup saved = applyStatusChange(popup, PopupStatus.APPROVED, managerId);
		return StoreApproveResponse.builder()
				.popupId(saved.getId())
				.status(saved.getStatus())
				.updatedAt(saved.getUpdatedAt())
				.updatedBy(saved.getUpdatedBy())
				.build();
	}

	@Transactional
	public StoreRejectResponse rejectPopup(UUID popupId, StoreRejectRequest request) {
		Popup popup = findPopup(popupId);
		if (popup.getStatus() != PopupStatus.REQUEST) {
			throw new ApprovalNotAllowedException("승인 대기 상태가 아닙니다.");
		}

		Long managerId = request == null ? null : request.getManagerId();
		Popup saved = applyStatusChange(popup, PopupStatus.CANCELLED, managerId);
		return StoreRejectResponse.builder()
				.popupId(saved.getId())
				.status(saved.getStatus())
				.updatedAt(saved.getUpdatedAt())
				.updatedBy(saved.getUpdatedBy())
				.build();
	}

	@Transactional
	public StoreWithdrawResponse withdrawPopup(UUID popupId, StoreWithdrawRequest request) {
		Popup popup = findPopup(popupId);
		if (popup.getStatus() != PopupStatus.REQUEST) {
			throw new ApprovalNotAllowedException("승인 대기 상태가 아닙니다.");
		}

		Long managerId = request == null ? null : request.getManagerId();
		Popup saved = applyStatusChange(popup, PopupStatus.CANCELLED, managerId);
		return StoreWithdrawResponse.builder()
				.popupId(saved.getId())
				.status(saved.getStatus())
				.updatedAt(saved.getUpdatedAt())
				.updatedBy(saved.getUpdatedBy())
				.build();
	}

	@Transactional(readOnly = true)
	public PendingStoreListResponse getPendingPopups(Integer page, Integer size) {
		int normalizedPage = normalizePage(page);
		int normalizedSize = normalizeSize(size);
		long offset = (long) (normalizedPage - 1) * normalizedSize;

		List<PopupListView> views = popupQueryRepository.findPopupsByStatus(
				PopupStatus.REQUEST.name(),
				normalizedSize,
				offset
		);
		long total = popupQueryRepository.countPopupsByStatus(PopupStatus.REQUEST.name());

		List<PendingStoreListResponse.ItemDto> items = views.stream()
				.map(view -> PendingStoreListResponse.ItemDto.builder()
						.popupId(toUuid(view.getId()))
						.storeId(toUuid(view.getStoreId()))
						.title(view.getTitle())
						.popupCategory(toCategory(view.getCategory()))
						.status(toStatus(view.getStatus()))
						.eventStartAt(view.getEventStartAt())
						.eventEndAt(view.getEventEndAt())
						.build())
				.toList();

		return PendingStoreListResponse.builder()
				.items(items)
				.page(normalizedPage)
				.size(normalizedSize)
				.total(total)
				.build();
	}

	private Popup findPopup(UUID popupId) {
		if (popupId == null) {
			throw new IllegalArgumentException("팝업 ID가 필요합니다.");
		}
		return ownerPopupRepository.findById(popupId)
				.orElseThrow(() -> new NotFoundException("팝업 정보를 찾을 수 없습니다."));
	}

	private Popup applyStatusChange(Popup popup, PopupStatus status, Long updatedBy) {
		popup.setStatus(status);
		popup.setUpdatedBy(updatedBy);

		Popup saved = ownerPopupRepository.save(popup);
		if (isInactiveStatus(status)) {
			ownerPopupScheduleRepository.deactivateActiveSchedulesByPopup(
					saved.getId(),
					LocalDateTime.now(),
					updatedBy
			);
		}
		return saved;
	}

	private PopupStatus parseStatus(String status) {
		if (status == null || status.isBlank()) {
			throw new IllegalArgumentException("상태 값이 필요합니다.");
		}
		if ("CANCELED".equalsIgnoreCase(status)) {
			return PopupStatus.CANCELLED;
		}
		try {
			return PopupStatus.valueOf(status.toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("유효하지 않은 상태입니다.");
		}
	}

	private int normalizePage(Integer page) {
		return page == null || page < 1 ? DEFAULT_PAGE : page;
	}

	private int normalizeSize(Integer size) {
		if (size == null || size < 1) {
			return DEFAULT_SIZE;
		}
		return Math.min(size, MAX_SIZE);
	}

	private PopupCategory toCategory(String value) {
		return value == null ? null : PopupCategory.valueOf(value.toUpperCase());
	}

	private PopupStatus toStatus(String value) {
		if (value == null) {
			return null;
		}
		if ("CANCELED".equalsIgnoreCase(value)) {
			return PopupStatus.CANCELLED;
		}
		return PopupStatus.valueOf(value.toUpperCase());
	}

	private UUID toUuid(String value) {
		return value == null ? null : UUID.fromString(value);
	}

	private boolean isInactiveStatus(PopupStatus status) {
		return status == PopupStatus.DRAFT || status == PopupStatus.CLOSED || status == PopupStatus.CANCELLED;
	}
}
