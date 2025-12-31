package com.popcorn.demo.domain.order.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusResponse {

	private Long id;
	private String status;
	private LocalDateTime updatedAt;
}
