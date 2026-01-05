package com.popcorn.demo.domain.popup.dto.query;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopupDetailQuery {

	private UUID productId;

	public static PopupDetailQuery of(UUID productId) {
		return PopupDetailQuery.builder()
				.productId(productId)
				.build();
	}
}
