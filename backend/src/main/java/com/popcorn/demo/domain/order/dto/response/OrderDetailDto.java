package com.popcorn.demo.domain.order.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailDto {

	private UUID id;
	private String orderNo;
	private String orderType;
	private String status;
	private Long customerId;
	private CustomerDto customer;
	private UUID storeId;
	private UUID productId;
	private Integer totalAmount;
	private LocalDateTime cancelableUntil;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private List<ItemDto> items;
	private AddressDto address;
	private PaymentDto payment;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CustomerDto {
		private Long id;
		private String role;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ItemDto {
		private UUID id;
		private String orderItemType;
		private UUID productId;
		private String productTitle;
		private String productCategory;
		private String productStatus;
		private UUID sessionId;
		private UUID optionId;
		private LocalDateTime sessionStartAt;
		private LocalDateTime sessionEndAt;
		private UUID merchVariantId;
		private String merchVariantName;
		private String merchSku;
		private Integer qty;
		private Integer unitPrice;
		private Integer lineAmount;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AddressDto {
		private String address1;
		private String address2;
		private String receiverName;
		private String phone;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PaymentDto {
		private UUID id;
		private String method;
		private String status;
		private Integer amount;
		private LocalDateTime approvedAt;
	}
}
