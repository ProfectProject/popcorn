package com.popcorn.demo.domain.popup.repository.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PopupSessionView {

	String getId();

	LocalDateTime getStartAt();

	LocalDateTime getEndAt();

	String getStatus();

	String getLocationId();

	String getLocationName();

	String getLocationAddress1();

	String getLocationAddress2();

	BigDecimal getLocationLatitude();

	BigDecimal getLocationLongitude();
}
