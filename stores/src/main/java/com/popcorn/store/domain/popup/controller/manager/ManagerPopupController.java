package com.popcorn.store.domain.popup.controller.manager;

import java.util.UUID;

import com.popcorn.common.controller.BaseController;
import com.popcorn.common.dto.BaseResponse;
import com.popcorn.store.domain.popup.dto.manager.PendingStoreListResponse;
import com.popcorn.store.domain.popup.dto.manager.StoreApproveResponse;
import com.popcorn.store.domain.popup.dto.manager.StoreForceStopRequest;
import com.popcorn.store.domain.popup.dto.manager.StoreForceStopResponse;
import com.popcorn.store.domain.popup.dto.manager.StoreRejectResponse;
import com.popcorn.store.domain.popup.dto.manager.StoreWithdrawResponse;
import com.popcorn.store.domain.popup.service.manager.ManagerPopupService;
import com.popcorn.store.global.security.ManagerAuthenticationResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "매니저팝업", description = "매니저 팝업 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/manager/popups")
public class ManagerPopupController extends BaseController {

    private final ManagerPopupService managerPopupService;

    @Operation(summary = "승인된 팝업 강제 중단", description = "승인 또는 오픈 상태인 팝업을 CANCELLED로 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팝업이 중단되었습니다."),
            @ApiResponse(responseCode = "401", description = "매니저 인증 필요"),
            @ApiResponse(responseCode = "403", description = "매니저 권한 필요"),
            @ApiResponse(responseCode = "404", description = "팝업 정보를 찾을 수 없습니다.")
    })
    @PostMapping("/{popupId}/force_stop")
    public ResponseEntity<BaseResponse<StoreForceStopResponse>> forceStopPopup(
            Authentication authentication,
            @Parameter(description = "팝업 ID", required = true) @PathVariable UUID popupId,
            @RequestBody(required = false) StoreForceStopRequest request) {
        Long managerId = ManagerAuthenticationResolver.resolveManagerId(authentication);
        return ok(managerPopupService.forceStopPopup(popupId, managerId, request));
    }

    @Operation(summary = "팝업 게시 승인", description = "승인 대기 중인 팝업을 APPROVED 상태로 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팝업이 승인되었습니다."),
            @ApiResponse(responseCode = "401", description = "매니저 인증 필요"),
            @ApiResponse(responseCode = "403", description = "매니저 권한 필요"),
            @ApiResponse(responseCode = "404", description = "팝업 정보를 찾을 수 없습니다.")
    })
    @PostMapping("/{popupId}/approve")
    public ResponseEntity<BaseResponse<StoreApproveResponse>> approvePopup(
            Authentication authentication,
            @Parameter(description = "팝업 ID", required = true) @PathVariable UUID popupId) {
        Long managerId = ManagerAuthenticationResolver.resolveManagerId(authentication);
        return ok(managerPopupService.approvePopup(popupId, managerId));
    }

    @Operation(summary = "팝업 게시 반려", description = "승인 대기 중인 팝업을 CANCELLED 상태로 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팝업이 반려되었습니다."),
            @ApiResponse(responseCode = "401", description = "매니저 인증 필요"),
            @ApiResponse(responseCode = "403", description = "매니저 권한 필요"),
            @ApiResponse(responseCode = "404", description = "팝업 정보를 찾을 수 없습니다.")
    })
    @PostMapping("/{popupId}/reject")
    public ResponseEntity<BaseResponse<StoreRejectResponse>> rejectPopup(
            Authentication authentication,
            @Parameter(description = "팝업 ID", required = true) @PathVariable UUID popupId) {
        Long managerId = ManagerAuthenticationResolver.resolveManagerId(authentication);
        return ok(managerPopupService.rejectPopup(popupId, managerId));
    }

    @Operation(summary = "팝업 승인 전 취소(철회)", description = "승인 대기 중인 팝업을 MANAGER 요청으로 취소합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팝업이 취소되었습니다."),
            @ApiResponse(responseCode = "401", description = "매니저 인증 필요"),
            @ApiResponse(responseCode = "403", description = "매니저 권한 필요"),
            @ApiResponse(responseCode = "404", description = "팝업 정보를 찾을 수 없습니다.")
    })
    @PostMapping("/{popupId}/withdraw")
    public ResponseEntity<BaseResponse<StoreWithdrawResponse>> withdrawPopup(
            Authentication authentication,
            @Parameter(description = "팝업 ID", required = true) @PathVariable UUID popupId) {
        Long managerId = ManagerAuthenticationResolver.resolveManagerId(authentication);
        return ok(managerPopupService.withdrawPopup(popupId, managerId));
    }

    @Operation(summary = "승인 대기 팝업 목록", description = "승인 상태인 팝업을 페이징 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팝업 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "매니저 인증 필요"),
            @ApiResponse(responseCode = "403", description = "매니저 권한 필요")
    })
    @GetMapping("/pending")
    public ResponseEntity<BaseResponse<PendingStoreListResponse>> getPendingPopups(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        ManagerAuthenticationResolver.resolveManagerId(authentication);
        return ok(managerPopupService.getPendingPopups(page, size));
    }
}
