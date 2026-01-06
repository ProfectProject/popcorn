package com.popcorn.demo.domain.order.repository.view;

import java.time.LocalDateTime;
import java.util.UUID;

public interface OrderItemDetailView {

	UUID getOrderItemId();

	String getOrderItemType();

	UUID getSessionOptionId();

	UUID getGoodsVariantId();

	Integer getQty();

	Integer getUnitPrice();

	Integer getLineAmount();

	UUID getSessionId();

	LocalDateTime getSessionStartAt();

	LocalDateTime getSessionEndAt();

	String getMerchVariantName();

	String getMerchSku();

	UUID getPopupId();

	String getProductTitle();

	String getProductCategory();

	String getProductStatus();
}
