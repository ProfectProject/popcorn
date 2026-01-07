package com.popcorn.demo.domain.popup.controller.owner;

import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.popup.dto.owner.request.CreatePopupRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.UpdatePopupRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.UpdatePopupStatusRequest;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupCreatedDto;
import com.popcorn.demo.domain.auth.dto.CustomUserDetails;
import com.popcorn.demo.domain.popup.exception.owner.OwnerPopupException;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupDetailDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupDeletedDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupListDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupStatusUpdatedDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupUpdatedDto;
import com.popcorn.demo.domain.popup.service.owner.OwnerPopupService;
import com.popcorn.demo.domain.users.entity.enums.UserRole;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "OwnerPopupController", description = "팝업 관리(Owner)")
@RestController
@RequestMapping("/api/v1/owner/stores")
public class OwnerPopupController {

    private final OwnerPopupService popupService;

    public OwnerPopupController(OwnerPopupService popupService) {
        this.popupService = popupService;
    }

    @PostMapping("/popups")
    public ResponseEntity<BaseResponse<PopupCreatedDto>> createPopup(
            Authentication authentication,
            @Parameter(description = "팝업 생성 요청", required = true) @Valid @RequestBody CreatePopupRequest request
    ) {

        Long userId = getCurrentOwnerId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(popupService.createPopup(userId, request)));
    }

    @GetMapping("/popups")
    public ResponseEntity<BaseResponse<List<PopupListDto>>> getPopupList(
            Authentication authentication,
            @Parameter(description = "스토어 ID", required = true) @RequestParam UUID storeId
    ) {
        Long userId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(BaseResponse.success(popupService.getPopupByStoreId(userId, storeId)));
    }

    @GetMapping("/popups/{popupId}")
    public ResponseEntity<BaseResponse<PopupDetailDto>> getPopupDetail(
            Authentication authentication,
            @Parameter(description = "팝업 ID", required = true) @PathVariable UUID popupId
    ) {
        Long userId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(BaseResponse.success(popupService.getPopupDetail(userId, popupId)));
    }

    @PutMapping("/popups/{popupId}")
    public ResponseEntity<BaseResponse<PopupUpdatedDto>> updatePopup(
            Authentication authentication,
            @Parameter(description = "팝업 ID", required = true) @PathVariable UUID popupId,
            @Parameter(description = "팝업 수정 요청", required = true) @Valid @RequestBody UpdatePopupRequest request
    ) {
        Long userId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(BaseResponse.success(popupService.updatePopup(userId, popupId, request)));
    }

    @PatchMapping("/popups/{popupId}/status")
    public ResponseEntity<BaseResponse<PopupStatusUpdatedDto>> updatePopupStatus(
            Authentication authentication,
            @Parameter(description = "팝업 ID", required = true) @PathVariable UUID popupId,
            @Parameter(description = "팝업 상태 변경 요청", required = true) @Valid @RequestBody UpdatePopupStatusRequest request
    ) {
        Long userId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(BaseResponse.success(popupService.updatePopupStatus(userId, popupId, request)));
    }

    @DeleteMapping("/popups/{popupId}")
    public ResponseEntity<BaseResponse<PopupDeletedDto>> deletePopup(
            Authentication authentication,
            @Parameter(description = "팝업 ID", required = true) @PathVariable UUID popupId
    ) {
        Long userId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(BaseResponse.success(popupService.deletePopup(userId, popupId)));
    }


    // 인증 정보에서 오너 ID를 추출하고 OWNER 권한을 확인합니다.
    private Long getCurrentOwnerId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw OwnerPopupException.unauthenticated();
        }

        Long userId = null;
        String name = authentication.getName();
        if (name != null) {
            try {
                userId = Long.parseLong(name);
            } catch (NumberFormatException ignored) {
                // Non-numeric name treated as CustomUserDetails.
            }
        }

        Object principal = authentication.getPrincipal();
        String roleValue = null;
        if (principal instanceof CustomUserDetails userDetails) {
            if (userId == null) {
                userId = userDetails.getUserId();
            }
            roleValue = userDetails.getRole();
        } else if (userId == null) {
            throw OwnerPopupException.invalidPrincipal();
        }

        if (userId == null) {
            throw OwnerPopupException.userIdRequired();
        }

        if (roleValue == null) {
            roleValue = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(auth -> auth != null && !auth.isBlank())
                    .map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
                    .findFirst()
                    .orElseThrow(OwnerPopupException::invalidRole);
        }

        UserRole role;
        try {
            role = UserRole.valueOf(roleValue);
        } catch (Exception e) {
            throw OwnerPopupException.invalidRole();
        }

        if (role != UserRole.OWNER) {
            throw OwnerPopupException.notOwner();
        }

        return userId;
    }

}
