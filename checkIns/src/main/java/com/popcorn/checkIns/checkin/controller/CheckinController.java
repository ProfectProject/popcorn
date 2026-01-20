package com.popcorn.checkIns.checkin.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.common.controller.BaseController;
import com.popcorn.common.dto.BaseResponse;
import com.popcorn.common.versioning.ApiVersion;
import com.popcorn.checkIns.checkin.dto.response.CheckinDetailResponse;
import com.popcorn.checkIns.checkin.dto.response.CheckinListResponse;
import com.popcorn.checkIns.checkin.service.CheckinService;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Checkin", description = "체크인 관리 API")
@ApiVersion("v1")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class CheckinController extends BaseController {

	private final CheckinService checkinService;

	@Operation(
			summary = "체크인 목록 조회",
			description = "체크인 기록 목록을 조회합니다."
	)
	@GetMapping("/checkins")
	public ResponseEntity<BaseResponse<CheckinListResponse>> getCheckins(
			@Parameter(description = "조회 개수", example = "50")
			@RequestParam(defaultValue = "50") int size) {
		CheckinListResponse response = checkinService.getCheckins(normalizeLimit(size));
		return ok(response);
	}

	@Operation(
			summary = "체크인 상세 조회",
			description = "체크인 기록 단건을 조회합니다."
	)
	@GetMapping("/checkins/{checkinId}")
	public ResponseEntity<BaseResponse<CheckinDetailResponse>> getCheckin(
			@Parameter(description = "체크인 ID", example = "90000000-0000-0000-0000-000000000001")
			@PathVariable UUID checkinId) {
		CheckinDetailResponse response = checkinService.getCheckin(checkinId);
		return ok(response);
	}

	private int normalizeLimit(int size) {
		if (size < 1) {
			return 1;
		}
		return Math.min(size, 200);
	}
}
