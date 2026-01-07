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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "오너 팝업 생성", description = "팝업 생성과 동시에 스케줄을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = PopupCreatedDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "스토어 없음")
    })
    @PostMapping("/popups")
    public ResponseEntity<BaseResponse<PopupCreatedDto>> createPopup(
            Authentication authentication,
            @Parameter(description = "팝업 생성 요청", required = true) @Valid @RequestBody CreatePopupRequest request
    ) {

        Long userId = getCurrentOwnerId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(popupService.createPopup(userId, request)));
    }

    @Operation(summary = "오너 팝업 목록 조회", description = "스토어 기준으로 팝업 기본 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "스토어 없음")
    })
    @GetMapping("/popups")
    public ResponseEntity<BaseResponse<List<PopupListDto>>> getPopupList(
            Authentication authentication,
            @Parameter(description = "스토어 ID", required = true) @RequestParam UUID storeId
    ) {
        Long userId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(BaseResponse.success(popupService.getPopupByStoreId(userId, storeId)));
    }

    @Operation(summary = "오너 팝업 상세 조회", description = "팝업 기본 정보와 스케줄 정보를 함께 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PopupDetailDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "팝업 없음")
    })
    @GetMapping("/popups/{popupId}")
    public ResponseEntity<BaseResponse<PopupDetailDto>> getPopupDetail(
            Authentication authentication,
            @Parameter(description = "팝업 ID", required = true) @PathVariable UUID popupId
    ) {
        Long userId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(BaseResponse.success(popupService.getPopupDetail(userId, popupId)));
    }

    @Operation(summary = "오너 팝업 수정", description = "팝업 정보 및 스케줄 변경을 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = PopupUpdatedDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "팝업 없음")
    })
    @PutMapping("/popups/{popupId}")
    public ResponseEntity<BaseResponse<PopupUpdatedDto>> updatePopup(
            Authentication authentication,
            @Parameter(description = "팝업 ID", required = true) @PathVariable UUID popupId,
            @Parameter(description = "팝업 수정 요청", required = true) @Valid @RequestBody UpdatePopupRequest request
    ) {
        Long userId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(BaseResponse.success(popupService.updatePopup(userId, popupId, request)));
    }

    @Operation(summary = "오너 팝업 상태 변경", description = "팝업 상태를 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공",
                    content = @Content(schema = @Schema(implementation = PopupStatusUpdatedDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "팝업 없음")
    })
    @PatchMapping("/popups/{popupId}/status")
    public ResponseEntity<BaseResponse<PopupStatusUpdatedDto>> updatePopupStatus(
            Authentication authentication,
            @Parameter(description = "팝업 ID", required = true) @PathVariable UUID popupId,
            @Parameter(description = "팝업 상태 변경 요청", required = true) @Valid @RequestBody UpdatePopupStatusRequest request
    ) {
        Long userId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(BaseResponse.success(popupService.updatePopupStatus(userId, popupId, request)));
    }

    @Operation(summary = "오너 팝업 삭제", description = "팝업과 스케줄을 소프트 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(schema = @Schema(implementation = PopupDeletedDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "팝업 없음")
    })
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
