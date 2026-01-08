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
			summary = "팝업 세션(회차) 조회",
			description = """
				특정 팝업의 예약 가능한 세션(회차) 목록을 조회합니다.

				**주요 기능:**
				- 팝업별 모든 활성 세션 조회
				- 날짜 범위 필터링 지원 (from, to 파라미터)
				- 캐시 적용으로 빠른 응답

				**사용 예시:**
				- 전체 세션: /api/v1/popups/{popupId}/sessions
				- 날짜 필터: /api/v1/popups/{popupId}/sessions?from=2025-01-15T00:00:00&to=2025-01-20T23:59:59
				"""
	)
	@ApiResponse(
			responseCode = "200",
			description = "세션 목록 조회 성공",
			content = @Content(
					schema = @Schema(implementation = PopupScheduleListResponse.class),
					examples = @ExampleObject(
							name = "성공 응답 예시",
							value = """
							{
							  "code": 200,
							  "message": "요청이 성공했습니다.",
							  "data": {
							    "items": [
							      {
							        "id": "00000000-0000-0000-0000-000000000201",
							        "startAt": "2025-01-15T10:00:00",
							        "endAt": "2025-01-15T12:00:00",
							        "price": 15000,
							        "capacity": 50,
							        "remainingCapacity": 50,
							        "isActive": true
							      },
							      {
							        "id": "00000000-0000-0000-0000-000000000202",
							        "startAt": "2025-01-15T14:00:00",
							        "endAt": "2025-01-15T16:00:00",
							        "price": 18000,
							        "capacity": 30,
							        "remainingCapacity": 25,
							        "isActive": true
							      }
							    ]
							  }
							}
							""")
			)
	)
	@ApiResponse(
			responseCode = "404",
			description = "팝업을 찾을 수 없음",
			content = @Content(
					schema = @Schema(implementation = BaseResponse.class),
					examples = @ExampleObject(
							name = "팝업 없음",
							value = """
							{
							  "code": 2101,
							  "message": "팝업 정보를 찾을 수 없습니다."
							}
							""")
			)
	)
	@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 파라미터",
			content = @Content(
					schema = @Schema(implementation = BaseResponse.class),
					examples = @ExampleObject(
							name = "날짜 범위 오류",
							value = """
							{
							  "code": 2100,
							  "message": "잘못된 요청입니다."
							}
							""")
			)
	)
	@GetMapping("/{popupId}/sessions")
	public ResponseEntity<BaseResponse<PopupScheduleListResponse>> getProductSessions(
			@Parameter(
					description = "팝업 ID (UUID 형식)",
					required = true,
					example = "00000000-0000-0000-0000-000000000101"
			)
			@PathVariable UUID popupId,
			@Parameter(
					description = "조회 시작 시각 (ISO 8601 형식, 선택사항)",
					example = "2025-01-15T00:00:00"
			)
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@Parameter(
					description = "조회 종료 시각 (ISO 8601 형식, 선택사항)",
					example = "2025-01-20T23:59:59"
			)
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
