package com.popcorn.demo.domain.order.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

	@NotBlank(message = "주문 타입은 필수입니다.")
	private String orderType;

	@NotNull(message = "스토어 ID는 필수입니다.")
	private UUID storeId;

	@NotNull(message = "상품 ID는 필수입니다.")
	private UUID productId;

	private UUID reservationId;

	@NotEmpty(message = "주문 항목은 최소 1개 이상이어야 합니다.")
	@Valid
	private List<OrderItemRequest> items;

	@Valid
	private AddressRequest address;

	public boolean isReservationType() {
		return "RESERVATION".equals(orderType);
	}

	public boolean isPurchaseType() {
		return "PURCHASE".equals(orderType);
	}

	public boolean hasConsistentItemTypes() {
		if (items == null || items.isEmpty()) {
			return false;
		}

		String expectedItemType = isReservationType() ? "RESERVATION" : "MERCH";
		return items.stream()
				.allMatch(item -> expectedItemType.equals(item.getOrderItemType()));
	}

	public boolean isValidReservationRequest() {
		if (!isReservationType()) {
			return false;
		}

		return items.stream()
				.allMatch(item ->
						"RESERVATION".equals(item.getOrderItemType())
								&& item.getSessionId() != null
								&& item.getOptionId() != null
				);
	}

	public boolean isValidPurchaseRequest() {
		if (!isPurchaseType()) {
			return false;
		}

		boolean hasValidItems = items.stream()
				.allMatch(item ->
						"MERCH".equals(item.getOrderItemType())
								&& item.getMerchVariantId() != null
				);

		boolean hasValidAddress = address != null
				&& address.getAddress1() != null
				&& !address.getAddress1().trim().isEmpty();

		return hasValidItems && hasValidAddress;
	}

	public int getTotalQuantity() {
		return items.stream()
				.mapToInt(OrderItemRequest::getQty)
				.sum();
	}
}
