package com.popcorn.demo.domain.popup.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.versioning.ApiVersion;
import com.popcorn.demo.domain.popup.service.PopupService;
import com.popcorn.demo.domain.popup.dto.query.PopupScheduleListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupScheduleListResponse;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Popup", description = "팝업 관련 API")
@RestController
@ApiVersion("v1")
@RequestMapping("/api/v1/popups")
@RequiredArgsConstructor
public class PopupScheduleController extends BaseController {

	private final PopupService popupService;

	@Operation(
			summary = "회차(슬롯) 조회",
			description = "상품 기준으로 예약 회차(슬롯) 목록을 조회합니다."
	)
	@ApiResponse(
			responseCode = "200",
			description = "회차 목록 조회 성공",
			content = @Content(
					schema = @Schema(implementation = PopupScheduleListResponse.class),
					examples = @ExampleObject(value = """
							{
							  "code": 200,
							  "message": "요청이 성공했습니다.",
							  "data": {
							    "items": [
							      {
							        "id": "00000000-0000-0000-0000-000000000201",
							        "startAt": "2025-01-01T10:00:00",
							        "endAt": "2025-01-05T18:00:00",
							        "price": 12000,
							        "capacity": 50,
							        "remainingCapacity": 50,
							        "isActive": true
							      }
							    ]
							  }
							}
							""")
			)
	)
	@GetMapping("/{popupId}/sessions")
	public ResponseEntity<BaseResponse<PopupScheduleListResponse>> getProductSessions(
			@Parameter(description = "상품 ID", required = true,
					example = "00000000-0000-0000-0000-000000000101")
			@PathVariable UUID popupId,
			@Parameter(description = "조회 시작 시각")
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@Parameter(description = "조회 종료 시각")
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

		PopupScheduleListResponse response = popupService.getProductSessions(
				PopupScheduleListQuery.builder()
						.popupId(popupId)
						.from(from)
						.to(to)
						.build());
		return ok(response);
	}
}
