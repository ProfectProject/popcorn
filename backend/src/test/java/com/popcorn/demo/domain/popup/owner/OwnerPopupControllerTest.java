package com.popcorn.demo.domain.popup.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.auth.dto.CustomUserDetails;
import com.popcorn.demo.domain.popup.controller.owner.OwnerPopupController;
import com.popcorn.demo.domain.popup.dto.owner.request.CreatePopupRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.UpdatePopupRequest;
import com.popcorn.demo.domain.popup.dto.owner.request.UpdatePopupStatusRequest;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupCreatedDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupDetailDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupDeletedDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupListDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupStatusUpdatedDto;
import com.popcorn.demo.domain.popup.dto.owner.response.PopupUpdatedDto;
import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
import com.popcorn.demo.domain.popup.exception.owner.OwnerPopupException;
import com.popcorn.demo.domain.popup.service.owner.OwnerPopupService;
import com.popcorn.demo.domain.users.entity.User;
import com.popcorn.demo.domain.users.entity.enums.UserRole;

class OwnerPopupControllerTest {

	@Test
	@DisplayName("오너 팝업 CRUD 컨트롤러는 인증 정보를 사용한다")
	void ownerPopupCrudEndpoints() {
		OwnerPopupService service = Mockito.mock(OwnerPopupService.class);
		OwnerPopupController controller = new OwnerPopupController(service);

		Authentication auth = buildAuthenticationWithUserDetails(1001L, UserRole.OWNER);
		UUID popupId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();

		when(service.createPopup(eq(1001L), any(CreatePopupRequest.class)))
				.thenReturn(PopupCreatedDto.builder().popupId(popupId).status(PopupStatus.REQUEST).build());
		ResponseEntity<BaseResponse<PopupCreatedDto>> created = controller.createPopup(
				auth,
				CreatePopupRequest.builder()
						.storeId(storeId)
						.title("title")
						.category(PopupCategory.FOOD)
						.build()
		);
		assertThat(created.getStatusCode().value()).isEqualTo(201);

		when(service.getPopupByStoreId(1001L, storeId, 1, 10, null)).thenReturn(List.of());
		controller.getPopupList(auth, storeId, 1, 10, null);
		verify(service).getPopupByStoreId(1001L, storeId, 1, 10, null);

		when(service.getPopupDetail(1001L, popupId)).thenReturn(new PopupDetailDto());
		controller.getPopupDetail(auth, popupId);

		when(service.updatePopup(eq(1001L), eq(popupId), any(UpdatePopupRequest.class)))
				.thenReturn(new PopupUpdatedDto());
		controller.updatePopup(auth, popupId, UpdatePopupRequest.builder().build());

		when(service.updatePopupStatus(eq(1001L), eq(popupId), any(UpdatePopupStatusRequest.class)))
				.thenReturn(new PopupStatusUpdatedDto());
		controller.updatePopupStatus(auth, popupId, UpdatePopupStatusRequest.builder().status(PopupStatus.CLOSED).build());

		when(service.deletePopup(1001L, popupId)).thenReturn(new PopupDeletedDto());
		controller.deletePopup(auth, popupId);
	}

	@Test
	@DisplayName("인증 정보가 없거나 역할이 잘못되면 예외가 발생한다")
	void ownerPopupAuthValidation() {
		OwnerPopupService service = Mockito.mock(OwnerPopupService.class);
		OwnerPopupController controller = new OwnerPopupController(service);
		UUID popupId = UUID.randomUUID();

		assertThatThrownBy(() -> controller.getPopupDetail(null, popupId))
				.isInstanceOf(OwnerPopupException.class);

		Authentication auth = Mockito.mock(Authentication.class);
		when(auth.isAuthenticated()).thenReturn(true);
		when(auth.getName()).thenReturn("not-a-number");
		when(auth.getPrincipal()).thenReturn("principal");

		assertThatThrownBy(() -> controller.getPopupDetail(auth, popupId))
				.isInstanceOf(OwnerPopupException.class);

		Authentication authWithBadRole = Mockito.mock(Authentication.class);
		when(authWithBadRole.isAuthenticated()).thenReturn(true);
		when(authWithBadRole.getName()).thenReturn("1001");
		when(authWithBadRole.getPrincipal()).thenReturn("principal");
		when(authWithBadRole.getAuthorities())
				.thenAnswer(inv -> java.util.List.of());

		assertThatThrownBy(() -> controller.getPopupDetail(authWithBadRole, popupId))
				.isInstanceOf(OwnerPopupException.class);
	}

	@Test
	@DisplayName("인증 이름이 숫자인 경우 권한에서 역할을 가져온다")
	void ownerPopupAuthWithAuthorities() {
		OwnerPopupService service = Mockito.mock(OwnerPopupService.class);
		OwnerPopupController controller = new OwnerPopupController(service);
		UUID storeId = UUID.randomUUID();

		Authentication auth = Mockito.mock(Authentication.class);
		when(auth.isAuthenticated()).thenReturn(true);
		when(auth.getName()).thenReturn("2001");
		when(auth.getPrincipal()).thenReturn("principal");
		when(auth.getAuthorities())
				.thenAnswer(inv -> java.util.List.of(new SimpleGrantedAuthority("ROLE_OWNER")));

		when(service.getPopupByStoreId(2001L, storeId, 1, 10, null)).thenReturn(List.of());
		controller.getPopupList(auth, storeId, 1, 10, null);
		verify(service).getPopupByStoreId(2001L, storeId, 1, 10, null);
	}

	private Authentication buildAuthenticationWithUserDetails(Long userId, UserRole role) {
		User user = new User();
		user.setUserId(userId);
		user.setRole(role);
		user.setEmail("test@popcorn.com");
		user.setPassword("password");
		CustomUserDetails details = new CustomUserDetails(user);

		Authentication auth = Mockito.mock(Authentication.class);
		when(auth.isAuthenticated()).thenReturn(true);
		when(auth.getPrincipal()).thenReturn(details);
		when(auth.getAuthorities())
				.thenAnswer(inv -> java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
		return auth;
	}
}
