package com.popcorn.users.users.dto.manager;

import com.popcorn.users.users.entity.enums.UserRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerApproveResponse {

	private Long userId;
	private UserRole role;
	private boolean active;
}
