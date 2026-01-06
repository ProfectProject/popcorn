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
		"items"
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
