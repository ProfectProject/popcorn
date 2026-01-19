package com.popcorn.demo.domain.qr.checkin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.qr.checkin.dto.response.CheckinDetailResponse;
import com.popcorn.demo.domain.qr.checkin.dto.response.CheckinListResponse;
import com.popcorn.demo.domain.qr.checkin.exception.CheckinException;
import com.popcorn.demo.domain.qr.checkin.repository.CheckinRepository;
import com.popcorn.demo.domain.qr.checkin.repository.CheckinRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CheckinService {

	private final CheckinRepository checkinRepository;

	@Transactional(readOnly = true)
	public CheckinListResponse getCheckins(int limit) {
		List<CheckinRow> rows = checkinRepository.findAll(limit);
		List<CheckinListResponse.Item> items = rows.stream()
				.map(row -> CheckinListResponse.Item.builder()
						.checkinId(row.checkinId())
						.orderId(row.orderId())
						.qrCode(row.qrCode())
						.createdAt(row.createdAt())
						.build())
				.toList();

		return CheckinListResponse.builder()
				.count(items.size())
				.items(items)
				.build();
	}

	@Transactional(readOnly = true)
	public CheckinDetailResponse getCheckin(UUID checkinId) {
		CheckinRow row = checkinRepository.findById(checkinId)
				.orElseThrow(CheckinException::notFound);

		return CheckinDetailResponse.builder()
				.checkinId(row.checkinId())
				.orderId(row.orderId())
				.orderQrCodeId(row.orderQrCodeId())
				.qrCode(row.qrCode())
				.createdAt(row.createdAt())
				.createdBy(row.createdBy())
				.build();
	}
}
