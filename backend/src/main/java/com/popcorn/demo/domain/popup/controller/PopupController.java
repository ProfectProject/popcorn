package com.popcorn.demo.domain.popup.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.versioning.ApiVersion;
import com.popcorn.demo.domain.popup.dto.query.PopupDetailQuery;
import com.popcorn.demo.domain.popup.dto.query.PopupListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupDetailResponse;
import com.popcorn.demo.domain.popup.dto.query.response.PopupListResponse;
import com.popcorn.demo.domain.popup.service.PopupService;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Popup", description = "팝업/상품 조회 API")
@ApiVersion("v1")
@RequestMapping("/api/v1/popups")
@RequiredArgsConstructor
public class PopupController extends BaseController {

	private final PopupService popupService;

	@Operation(
			summary = "팝업 목록 조회",
			description = "조건에 따라 팝업/상품 목록을 조회합니다."
	)
	@ApiResponse(
			responseCode = "200",
			description = "팝업 목록 조회 성공",
			content = @Content(
					schema = @Schema(implementation = PopupListResponse.class),
					examples = @ExampleObject(value = """
							{
							  "code": 200,
							  "message": "요청이 성공했습니다.",
							  "data": {
							    "items": [
							      {
							        "id": "00000000-0000-0000-0000-000000000101",
							        "storeId": "00000000-0000-0000-0000-000000000001",
							        "title": "Seed Popup 1",
							        "category": "FOOD",
							        "status": "OPEN",
							        "eventStartAt": "2025-01-01T10:00:00",
							        "eventEndAt": "2025-01-05T18:00:00"
							      }
							    ],
							    "page": 1,
							    "size": 20,
							    "total": 1
							  }
							}
							""")
			)
	)
	@GetMapping
	public ResponseEntity<BaseResponse<PopupListResponse>> getPopups(
			@Parameter(description = "지역 필터", example = "101")
			@org.springframework.web.bind.annotation.RequestParam(required = false) Long regionId,
			@Parameter(description = "카테고리(FOOD/IDOL/EXHIBITION/WORKSHOP/FASHION/BEAUTY/LIFESTYLE/ART/GAME/TECH/SPORTS/BOOK/PET/ETC)",
					example = "FOOD")
			@org.springframework.web.bind.annotation.RequestParam(required = false) String category,
			@Parameter(description = "검색어", example = "팝업")
			@org.springframework.web.bind.annotation.RequestParam(required = false) String keyword,
			@Parameter(description = "가게 필터", example = "00000000-0000-0000-0000-000000000001")
			@org.springframework.web.bind.annotation.RequestParam(required = false) java.util.UUID storeId,
			@Parameter(description = "페이지(기본 1)", example = "1")
			@org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "1") Integer page,
			@Parameter(description = "사이즈(기본 20, 최대 100)", example = "20")
			@org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "20") Integer size,
			@Parameter(description = "전체 개수 포함 여부(기본 true)", example = "true")
			@org.springframework.web.bind.annotation.RequestParam(required = false) Boolean withTotal) {

		PopupListQuery requestQuery = PopupListQuery.builder()
				.regionId(regionId)
				.category(category)
				.keyword(keyword)
				.storeId(storeId)
				.page(page)
				.size(size)
				.withTotal(withTotal)
				.build();

		PopupListResponse response = popupService.getPopups(requestQuery);
		return ok(response);
	}

	@Operation(
			summary = "팝업 상세 조회",
			description = "팝업/상품 상세 정보를 조회합니다."
	)
	@ApiResponse(
			responseCode = "200",
			description = "팝업 상세 조회 성공",
			content = @Content(
					schema = @Schema(implementation = PopupDetailResponse.class),
					examples = @ExampleObject(value = """
							{
							  "code": 200,
							  "message": "요청이 성공했습니다.",
							  "data": {
							    "id": "00000000-0000-0000-0000-000000000101",
							    "storeId": "00000000-0000-0000-0000-000000000001",
							    "title": "Seed Popup 1",
							    "description": "예약형 팝업",
							    "category": "FOOD",
							    "status": "OPEN",
							    "eventStartAt": "2025-01-01T10:00:00",
							    "eventEndAt": "2025-01-05T18:00:00"
							  }
							}
							""")
			)
	)
	@ApiResponse(
			responseCode = "404",
			description = "팝업 정보를 찾을 수 없음",
			content = @Content(examples = @ExampleObject(value = """
					{
					  "code": 2101,
					  "message": "팝업 정보를 찾을 수 없습니다.",
					  "data": {
					    "code": 2101,
					    "message": "팝업 정보를 찾을 수 없습니다."
					  }
					}
					"""))
	)
	@GetMapping("/{productId}")
	public ResponseEntity<BaseResponse<PopupDetailResponse>> getPopupDetail(
			@Parameter(description = "상품 ID", required = true,
					example = "00000000-0000-0000-0000-000000000101")
			@PathVariable UUID productId) {

		PopupDetailResponse response = popupService.getPopupDetail(PopupDetailQuery.of(productId));
		return ok(response);
	}
}
