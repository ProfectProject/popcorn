package com.popcorn.demo.domain.manager.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.manager.handler.ApprovalNotAllowedException;
import com.popcorn.demo.domain.manager.handler.NotFoundException;
import com.popcorn.demo.domain.manager.client.OrderMicroserviceClient;
import com.popcorn.order.dto.request.OrderCancelRequest;
import com.popcorn.order.dto.request.OrderStatusUpdateRequest;
import com.popcorn.order.dto.response.OrderCancelResponse;
import com.popcorn.order.dto.response.OrderDetailResponse;
import com.popcorn.order.dto.response.OrderListResponse;
import com.popcorn.order.dto.response.OrderStatusUpdateResponse;
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
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopupManagerService {

	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final OwnerPopupRepository ownerPopupRepository;
	private final OwnerPopupScheduleRepository ownerPopupScheduleRepository;
	private final PopupQueryRepository popupQueryRepository;
	private final OrderMicroserviceClient orderMicroserviceClient;

	// ========================= Order 마이크로서비스 연동 메서드 =========================

	/**
	 * 팝업 취소 (Order 마이크로서비스 호출)
	 */
	@Transactional
	public OrderCancelResponse cancelPopup(OrderCancelRequest request, String status) {
		if (request == null || request.getPopupId() == null) {
			throw new IllegalArgumentException("팝업 ID가 필요합니다.");
		}

		log.info("📞 Order 마이크로서비스 호출 - 팝업 취소: popupId={}", request.getPopupId());

		try {
			// Order 마이크로서비스로 취소 요청 전달
			OrderCancelResponse response = orderMicroserviceClient.cancelOrder(request);
			log.info("✅ Order 마이크로서비스 응답 완료 - 팝업 취소: popupId={}", response.getPopupId());
			return response;

		} catch (Exception e) {
			log.error("❌ Order 마이크로서비스 호출 실패 - 팝업 취소", e);
			throw new RuntimeException("팝업 취소 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}

	/**
	 * 팝업 상태 변경 (Order 마이크로서비스 호출)
	 */
	@Transactional
	public OrderStatusUpdateResponse updatePopupStatus(UUID popupId, OrderStatusUpdateRequest request) {
		if (popupId == null) {
			throw new IllegalArgumentException("팝업 ID가 필요합니다.");
		}
		if (request == null || request.getStatus() == null) {
			throw new IllegalArgumentException("변경할 상태가 필요합니다.");
		}

		log.info("📞 Order 마이크로서비스 호출 - 상태 변경: popupId={}, status={}",
			popupId, request.getStatus());

		try {
			// Order 마이크로서비스로 상태 변경 요청 전달
			OrderStatusUpdateResponse response = orderMicroserviceClient.updateOrderStatus(popupId, request);
			log.info("✅ Order 마이크로서비스 응답 완료 - 상태 변경: popupId={}", response.getPopupId());
			return response;

		} catch (Exception e) {
			log.error("❌ Order 마이크로서비스 호출 실패 - 상태 변경", e);
			throw new RuntimeException("팝업 상태 변경 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}

	/**
	 * 팝업 상세 조회 (Order 마이크로서비스 호출)
	 */
	@Transactional(readOnly = true)
	public OrderDetailResponse getPopupDetail(UUID popupId) {
		if (popupId == null) {
			throw new IllegalArgumentException("팝업 ID가 필요합니다.");
		}

		log.info("📞 Order 마이크로서비스 호출 - 상세 조회: popupId={}", popupId);

		try {
			// Order 마이크로서비스로 상세 조회 요청 전달
			OrderDetailResponse response = orderMicroserviceClient.getOrderDetail(popupId);
			log.info("✅ Order 마이크로서비스 응답 완료 - 상세 조회: popupId={}", response.getPopupId());
			return response;

		} catch (Exception e) {
			log.error("❌ Order 마이크로서비스 호출 실패 - 상세 조회", e);
			throw new RuntimeException("팝업 상세 조회 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
	}

	/**
	 * 상태별 팝업 목록 조회 (Order 마이크로서비스 호출)
	 */
	@Transactional(readOnly = true)
	public OrderListResponse getPopupsByStatus(String status, Integer page, Integer size) {
		if (status == null || status.trim().isEmpty()) {
			throw new IllegalArgumentException("상태는 필수입니다.");
		}

		int normalizedPage = normalizePage(page);
		int normalizedSize = normalizeSize(size);

		log.info("📞 Order 마이크로서비스 호출 - 목록 조회: status={}, page={}, size={}",
			status, normalizedPage, normalizedSize);

		try {
			// Order 마이크로서비스로 목록 조회 요청 전달
			OrderListResponse response = orderMicroserviceClient.getOrderList(status, normalizedPage, normalizedSize);
			log.info("✅ Order 마이크로서비스 응답 완료 - 목록 조회: total={}", response.getTotal());
			return response;

		} catch (Exception e) {
			log.error("❌ Order 마이크로서비스 호출 실패 - 목록 조회", e);
			throw new RuntimeException("팝업 목록 조회 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
		}
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
