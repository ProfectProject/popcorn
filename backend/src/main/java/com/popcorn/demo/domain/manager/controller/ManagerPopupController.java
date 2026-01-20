
package com.popcorn.demo.domain.manager.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.common.controller.BaseController;
import com.popcorn.common.dto.BaseResponse;
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
import com.popcorn.demo.domain.manager.service.PopupManagerService;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "매니저팝업", description = "매니저 팝업 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/popups")
public class ManagerPopupController extends BaseController {

    private final PopupManagerService popupManagerService;

    @PostMapping
    public ResponseEntity<BaseResponse<OrderCancelResponse>> cancelPopup(
            @RequestParam(required = false, defaultValue = "CANCELLED") String status,
            @RequestBody OrderCancelRequest request) {
        return ok(popupManagerService.cancelPopup(request, status));
    }

    @PutMapping("/{popupId}/status")
    public ResponseEntity<BaseResponse<OrderStatusUpdateResponse>> updatePopupStatus(
            @PathVariable UUID popupId,
            @RequestBody OrderStatusUpdateRequest request) {
        return ok(popupManagerService.updatePopupStatus(popupId, request));
    }

    @GetMapping("/{popupId}")
    public ResponseEntity<BaseResponse<OrderDetailResponse>> getPopupDetail(@PathVariable UUID popupId) {
        return ok(popupManagerService.getPopupDetail(popupId));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<OrderListResponse>> getApprovedPopups(
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return ok(popupManagerService.getPopupsByStatus(status, page, size));
    }

    @PostMapping("/{popupId}/force_stop")
    public ResponseEntity<BaseResponse<StoreForceStopResponse>> forceStopPopup(
            @PathVariable UUID popupId,
            @RequestBody StoreForceStopRequest request) {
        return ok(popupManagerService.forceStopPopup(popupId, request));
    }

    @PostMapping("/{popupId}/approve")
    public ResponseEntity<BaseResponse<StoreApproveResponse>> approvePopup(
            @PathVariable UUID popupId,
            @RequestBody StoreApproveRequest request) {
        return ok(popupManagerService.approvePopup(popupId, request));
    }

    @PostMapping("/{popupId}/reject")
    public ResponseEntity<BaseResponse<StoreRejectResponse>> rejectPopup(
            @PathVariable UUID popupId,
            @RequestBody StoreRejectRequest request) {
        return ok(popupManagerService.rejectPopup(popupId, request));
    }

    @PostMapping("/{popupId}/withdraw")
    public ResponseEntity<BaseResponse<StoreWithdrawResponse>> withdrawPopup(
            @PathVariable UUID popupId,
            @RequestBody StoreWithdrawRequest request) {
        return ok(popupManagerService.withdrawPopup(popupId, request));
    }

    @GetMapping("/pending")
    public ResponseEntity<BaseResponse<PendingStoreListResponse>> getPendingPopups(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return ok(popupManagerService.getPendingPopups(page, size));
    }
}
