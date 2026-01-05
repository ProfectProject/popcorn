package com.popcorn.demo.domain.popup.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.versioning.ApiVersion;
import com.popcorn.demo.domain.popup.application.PopupApplicationService;
import com.popcorn.demo.domain.popup.dto.query.PopupOptionListQuery;
import com.popcorn.demo.domain.popup.dto.query.response.PopupOptionListResponse;

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
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class PopupOptionController extends BaseController {

	private final PopupApplicationService popupApplicationService;

	@Operation(
			summary = "옵션 조회",
			description = "상품 기준으로 옵션 목록을 조회합니다."
	)
	@ApiResponse(
			responseCode = "200",
			description = "옵션 목록 조회 성공",
			content = @Content(
					schema = @Schema(implementation = PopupOptionListResponse.class),
					examples = @ExampleObject(value = """
							{
							  "code": 200,
							  "message": "요청이 성공했습니다.",
							  "data": {
							    "items": [
							      {
							        "id": "00000000-0000-0000-0000-000000000301",
							        "name": "일반 좌석",
							        "price": 10000,
							        "capacity": 20,
							        "isHidden": false
							      }
							    ]
							  }
							}
							""")
			)
	)
	@GetMapping("/{productId}/options")
	public ResponseEntity<BaseResponse<PopupOptionListResponse>> getProductOptions(
			@Parameter(description = "상품 ID", required = true,
					example = "00000000-0000-0000-0000-000000000101")
			@PathVariable UUID productId) {

		PopupOptionListResponse response = popupApplicationService.getProductOptions(
				PopupOptionListQuery.builder()
						.productId(productId)
						.build());
		return ok(response);
	}
}
