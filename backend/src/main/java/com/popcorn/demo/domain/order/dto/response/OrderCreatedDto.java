package com.popcorn.demo.domain.order.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({
		"orderId",
		"orderNo",
		"orderType",
		"status",
		"storeId",
		"popupId",
		"totalAmount",
		"cancelableUntil",
		"createdAt",
		"items",
		"paymentId",
		"paymentAmount",
		"checkoutUrl",
		"customerKey",
		"successUrl",
		"failUrl",
		"clientKey",
		"paymentKey",
		"readyForPayment"
})
public class OrderCreatedDto {

	@JsonIgnore
	private UUID id;

	@JsonProperty("orderId")
	public UUID getOrderId() {
		return id;
	}

	private String orderNo;
	private String orderType;
	private String status;
	private UUID storeId;
	private UUID popupId;
	private Integer totalAmount;
	private LocalDateTime cancelableUntil;
	private LocalDateTime createdAt;
	private List<OrderItemDto> items;
	private UUID paymentId;
	private Integer paymentAmount;
	private String checkoutUrl;
	private String customerKey;
	private String successUrl;
	private String failUrl;

	// 🎯 프론트엔드 토스 결제위젯용 추가 필드
	private String clientKey;      // 토스 클라이언트 키
	private String paymentKey;     // 토스 결제키
	private Boolean readyForPayment; // 즉시 결제 가능 여부

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OrderItemDto {
		private UUID id;
		private String orderItemType;
		private Integer qty;
		private Integer unitPrice;
		private Integer lineAmount;
	}
}
