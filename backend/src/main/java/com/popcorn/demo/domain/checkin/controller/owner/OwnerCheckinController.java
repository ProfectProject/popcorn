package com.popcorn.demo.domain.checkin.controller.owner;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.auth.dto.CustomUserDetails;
import com.popcorn.demo.domain.checkin.dto.owner.response.OwnerCheckinListResponse;
import com.popcorn.demo.domain.checkin.exception.owner.OwnerCheckinException;
import com.popcorn.demo.domain.checkin.service.owner.OwnerCheckinService;
import com.popcorn.demo.domain.users.entity.enums.UserRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "OwnerCheckin", description = "오너 체크인 조회 API")
@RequestMapping("/api/v1/owner/popups")
@RequiredArgsConstructor
@Validated
public class OwnerCheckinController extends BaseController {

	private final OwnerCheckinService ownerCheckinService;

	@Operation(
			summary = "오너 체크인 목록 조회",
			description = "팝업별 체크인 기록을 조회합니다."
	)
	@GetMapping("/{popupId}/checkins")
	public ResponseEntity<BaseResponse<OwnerCheckinListResponse>> getCheckinsByPopup(
			Authentication authentication,
			@Parameter(description = "팝업 ID", example = "90000000-0000-0000-0000-000000000001")
			@PathVariable UUID popupId) {
		Long ownerId = getCurrentOwnerId(authentication);
		OwnerCheckinListResponse response = ownerCheckinService.getCheckinsByPopup(ownerId, popupId);
		return ok(response);
	}

	// 인증 정보에서 오너 ID를 추출하고 OWNER 권한을 확인합니다.
	private Long getCurrentOwnerId(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw OwnerCheckinException.unauthenticated();
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
			throw OwnerCheckinException.invalidPrincipal();
		}

		if (userId == null) {
			throw OwnerCheckinException.userIdRequired();
		}

		if (roleValue == null) {
			roleValue = authentication.getAuthorities().stream()
					.map(GrantedAuthority::getAuthority)
					.filter(auth -> auth != null && !auth.isBlank())
					.map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
					.findFirst()
					.orElseThrow(OwnerCheckinException::invalidRole);
		}

		UserRole role;
		try {
			role = UserRole.valueOf(roleValue);
		} catch (Exception e) {
			throw OwnerCheckinException.invalidRole();
		}

		if (role != UserRole.OWNER) {
			throw OwnerCheckinException.notOwner();
		}

		return userId;
	}
}
