package com.popcorn.demo.domain.popup.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.popup.dto.query.PopupSessionListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupSessionListResponse;
import com.popcorn.demo.domain.popup.repository.PopupSessionQueryRepository;
import com.popcorn.demo.domain.popup.repository.view.PopupSessionView;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PopupSessionQueryService {

	private final PopupSessionQueryRepository popupSessionQueryRepository;

	@Cacheable(value = "popupSessions", key = "#query.productId + '_' + #query.from + '_' + #query.to")
	public PopupSessionListResponse getProductSessions(PopupSessionListQuery query) {
		List<PopupSessionView> views = popupSessionQueryRepository.findProductSessions(
				query.getProductId(), query.getFrom(), query.getTo());

		List<PopupSessionListResponse.ItemDto> items = views.stream()
				.map(view -> PopupSessionListResponse.ItemDto.builder()
						.id(toUuid(view.getId()))
						.startAt(view.getStartAt())
						.endAt(view.getEndAt())
						.status(mapStatus(view.getStatus()))
						.location(buildLocation(
								toUuid(view.getLocationId()),
								view.getLocationName(),
								view.getLocationAddress1(),
								view.getLocationAddress2(),
								view.getLocationLatitude(),
								view.getLocationLongitude()
						))
						.build())
				.toList();

		return PopupSessionListResponse.builder()
				.items(items)
				.build();
	}

	private PopupSessionListResponse.LocationDto buildLocation(UUID id, String name, String address1, String address2,
			BigDecimal latitude, BigDecimal longitude) {
		if (id == null && name == null && address1 == null && address2 == null && latitude == null && longitude == null) {
			return null;
		}
		return PopupSessionListResponse.LocationDto.builder()
				.id(id)
				.name(name)
				.address1(address1)
				.address2(address2)
				.latitude(toDouble(latitude))
				.longitude(toDouble(longitude))
				.build();
	}

	private String mapStatus(String rawStatus) {
		if (rawStatus == null) {
			return null;
		}
		// API 스펙의 OPEN/CLOSED에 맞춰 세션 상태를 정규화한다.
		return switch (rawStatus) {
			case "UPCOMING" -> "OPEN";
			case "ENDED" -> "CLOSED";
			default -> rawStatus;
		};
	}

	private UUID toUuid(String value) {
		return value == null ? null : UUID.fromString(value);
	}

	private Double toDouble(BigDecimal value) {
		return value == null ? null : value.doubleValue();
	}
}
