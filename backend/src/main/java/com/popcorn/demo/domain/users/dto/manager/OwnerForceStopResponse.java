package com.popcorn.demo.domain.users.dto.manager;

import com.popcorn.demo.domain.users.entity.enums.UserRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerForceStopResponse {

	private Long userId;
	private UserRole role;
	private boolean active;
}
