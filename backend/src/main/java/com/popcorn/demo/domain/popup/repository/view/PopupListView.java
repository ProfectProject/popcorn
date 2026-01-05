package com.popcorn.demo.domain.popup.repository.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface PopupListView {

	String getId();

	String getStoreId();

	String getTitle();

	String getProductType();

	String getCategory();

	Long getRegionId();

	Boolean getIsHidden();

	LocalDateTime getEventStartAt();

	LocalDateTime getEventEndAt();

	String getLocationId();

	String getLocationName();

	String getLocationAddress1();

	String getLocationAddress2();

	BigDecimal getLocationLatitude();

	BigDecimal getLocationLongitude();
}
