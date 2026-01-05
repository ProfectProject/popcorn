package com.popcorn.demo.domain.order.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {

	@NotBlank(message = "주문 항목 타입은 필수입니다.")
	private String orderItemType;

	@NotNull(message = "수량은 필수입니다.")
	@Min(value = 1, message = "수량은 1 이상이어야 합니다.")
	private Integer qty;

	private Integer unitPrice;

	private UUID sessionId;

	private UUID optionId;

	private UUID merchVariantId;

	public boolean isReservationType() {
		return "RESERVATION".equals(orderItemType);
	}

	public boolean isMerchType() {
		return "MERCH".equals(orderItemType);
	}

	public boolean isValidReservationItem() {
		return isReservationType()
				&& sessionId != null
				&& qty != null
				&& qty > 0;
	}

	public boolean isValidMerchItem() {
		return isMerchType()
				&& merchVariantId != null
				&& qty != null
				&& qty > 0;
	}

	public boolean hasRequiredFields() {
		if (isReservationType()) {
			return isValidReservationItem();
		}
		if (isMerchType()) {
			return isValidMerchItem();
		}
		return false;
	}

	public boolean hasUnnecessaryFields() {
		if (isReservationType()) {
			return merchVariantId != null;
		}
		if (isMerchType()) {
			return sessionId != null || optionId != null;
		}
		return false;
	}

	public String getSessionOptionKey() {
		if (isReservationType() && sessionId != null) {
			return sessionId.toString();
		}
		return null;
	}

	public String getStockIdentifier() {
		if (isReservationType()) {
			return getSessionOptionKey();
		}
		if (isMerchType()) {
			return "VARIANT_" + merchVariantId;
		}
		return null;
	}

	public String getDisplayDescription() {
		if (isReservationType()) {
			return String.format("예약 (스케줄: %s) x %d개", sessionId, qty);
		}
		if (isMerchType()) {
			return String.format("굿즈 (변형: %s) x %d개", merchVariantId, qty);
		}
		return String.format("항목 (%s) x %d개", orderItemType, qty);
	}
}
