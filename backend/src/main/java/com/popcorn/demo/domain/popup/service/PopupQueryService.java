package com.popcorn.demo.domain.popup.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.popcorn.demo.domain.popup.dto.query.PopupDetailQuery;
import com.popcorn.demo.domain.popup.dto.query.PopupListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupDetailResponse;
import com.popcorn.demo.domain.popup.dto.query.response.PopupListResponse;
import com.popcorn.demo.domain.popup.repository.PopupQueryRepository;
import com.popcorn.demo.domain.popup.repository.view.PopupListView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PopupQueryService {

	private final PopupQueryRepository popupQueryRepository;

	public PopupListResponse getPopups(PopupListQuery query) {
		Long regionId = query.getRegionId();
		String category = query.getCategory();
		String keyword = query.getKeyword();
		UUID storeId = query.getStoreId();
		Integer page = query.getPage();
		Integer size = query.getSize();

		int normalizedPage = page == null ? 1 : page;
		int normalizedSize = size == null ? 20 : size;
		long offset = (long) (normalizedPage - 1) * normalizedSize;

		long total = popupQueryRepository.countPopups(regionId, category, keyword, storeId);
		List<PopupListView> views = popupQueryRepository.findPopups(
				regionId, category, keyword, storeId, normalizedSize, offset);

		List<PopupListResponse.ItemDto> items = views.stream()
				.map(view -> PopupListResponse.ItemDto.builder()
						.id(toUuid(view.getId()))
						.storeId(toUuid(view.getStoreId()))
						.title(view.getTitle())
						.productType(view.getProductType())
						.category(view.getCategory())
						.regionId(view.getRegionId())
						.isHidden(Boolean.TRUE.equals(view.getIsHidden()))
						.eventStartAt(view.getEventStartAt())
						.eventEndAt(view.getEventEndAt())
						.location(buildListLocation(
								toUuid(view.getLocationId()),
								view.getLocationName(),
								view.getLocationAddress1(),
								view.getLocationAddress2(),
								view.getLocationLatitude(),
								view.getLocationLongitude(),
								null
						))
						.build())
				.toList();

		return PopupListResponse.builder()
				.items(items)
				.page(normalizedPage)
				.size(normalizedSize)
				.total(total)
				.build();
	}

	public PopupDetailResponse getPopupDetail(PopupDetailQuery query) {
		PopupListView view = popupQueryRepository.findPopupDetail(query.getProductId())
				.orElseThrow(com.popcorn.demo.domain.popup.exception.PopupException::popupNotFound);

		return PopupDetailResponse.builder()
				.id(toUuid(view.getId()))
				.storeId(toUuid(view.getStoreId()))
				.title(view.getTitle())
				.productType(view.getProductType())
				.category(view.getCategory())
				.regionId(view.getRegionId())
				.isHidden(Boolean.TRUE.equals(view.getIsHidden()))
				.eventStartAt(view.getEventStartAt())
				.eventEndAt(view.getEventEndAt())
				.location(buildDetailLocation(
						toUuid(view.getLocationId()),
						view.getLocationName(),
						view.getLocationAddress1(),
						view.getLocationAddress2(),
						view.getLocationLatitude(),
						view.getLocationLongitude(),
						null
				))
				.build();
	}

	private PopupListResponse.LocationDto buildListLocation(UUID id, String name, String address1, String address2,
			BigDecimal latitude, BigDecimal longitude, String placeNote) {
		if (id == null && name == null && address1 == null && address2 == null && latitude == null && longitude == null) {
			return null;
		}
		return PopupListResponse.LocationDto.builder()
				.id(id)
				.name(name)
				.address1(address1)
				.address2(address2)
				.latitude(toDouble(latitude))
				.longitude(toDouble(longitude))
				.placeNote(placeNote)
				.build();
	}

	private PopupDetailResponse.LocationDto buildDetailLocation(UUID id, String name, String address1, String address2,
			BigDecimal latitude, BigDecimal longitude, String placeNote) {
		if (id == null && name == null && address1 == null && address2 == null && latitude == null && longitude == null) {
			return null;
		}
		return PopupDetailResponse.LocationDto.builder()
				.id(id)
				.name(name)
				.address1(address1)
				.address2(address2)
				.latitude(toDouble(latitude))
				.longitude(toDouble(longitude))
				.placeNote(placeNote)
				.build();
	}

	private Double toDouble(BigDecimal value) {
		return value == null ? null : value.doubleValue();
	}

	private UUID toUuid(String value) {
		return value == null ? null : UUID.fromString(value);
	}
}
