package com.popcorn.demo.domain.order.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPaymentCreateResponse {

	private UUID paymentId;
	private String status;
	private String orderStatus;
}
