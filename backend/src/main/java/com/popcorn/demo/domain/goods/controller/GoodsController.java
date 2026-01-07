package com.popcorn.demo.domain.goods.controller;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.domain.goods.dto.GoodsCreateRequest;
import com.popcorn.demo.domain.goods.dto.GoodsIdResponse;
import com.popcorn.demo.domain.goods.dto.GoodsItemResponse;
import com.popcorn.demo.domain.goods.dto.GoodsListResponse;
import com.popcorn.demo.domain.goods.dto.GoodsStatusResponse;
import com.popcorn.demo.domain.goods.dto.GoodsStatusUpdateRequest;
import com.popcorn.demo.domain.goods.dto.GoodsUpdateRequest;
import com.popcorn.demo.domain.goods.service.GoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "Goods", description = "굿즈 관리 API")
@RequestMapping("/api/v1/owner/popups/{popupId}/goods")
public class GoodsController extends BaseController {
    private final GoodsService goodsService;

    @GetMapping
    @Operation(summary = "굿즈 목록 조회", description = "팝업에 등록된 굿즈 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "굿즈 목록 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = GoodsListResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "요청이 성공했습니다.",
                                      "data": {
                                        "items": [
                                          {
                                            "id": "00000000-0000-0000-0000-000000000401",
                                            "popupId": "00000000-0000-0000-0000-000000000101",
                                            "stockUnit": "개",
                                            "goodsName": "팝콘 키링",
                                            "goodsPrice": 12000,
                                            "stock": 100,
                                            "isActive": true,
                                            "createdAt": "2025-01-01T10:00:00",
                                            "updatedAt": "2025-01-02T12:30:00"
                                          }
                                        ]
                                      }
                                    }
                                    """)
                    )
            )
    })
    public ResponseEntity<BaseResponse<GoodsListResponse>> list(
            @Parameter(description = "팝업 ID", required = true)
            @PathVariable UUID popupId
    ) {
        return ok(goodsService.list(popupId));
    }

    @PostMapping
    @Operation(summary = "굿즈 등록", description = "팝업에 굿즈를 등록합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "굿즈 등록 성공",
                    content = @Content(
                            schema = @Schema(implementation = GoodsIdResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "요청이 성공했습니다.",
                                      "data": {
                                        "id": "00000000-0000-0000-0000-000000000401"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<BaseResponse<GoodsIdResponse>> create(
            @Parameter(description = "팝업 ID", required = true)
            @PathVariable UUID popupId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "굿즈 등록 요청",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = GoodsCreateRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "stockUnit": "개",
                                      "goodsName": "팝콘 키링",
                                      "goodsPrice": 12000,
                                      "stock": 100,
                                      "isActive": true
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody GoodsCreateRequest request
    ) {
        return ok(goodsService.create(popupId, request));
    }

    @GetMapping("/{goodsId}")
    @Operation(summary = "굿즈 상세 조회", description = "팝업의 특정 굿즈 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "굿즈 상세 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = GoodsItemResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "요청이 성공했습니다.",
                                      "data": {
                                        "id": "00000000-0000-0000-0000-000000000401",
                                        "popupId": "00000000-0000-0000-0000-000000000101",
                                        "stockUnit": "개",
                                        "goodsName": "팝콘 키링",
                                        "goodsPrice": 12000,
                                        "stock": 100,
                                        "isActive": true,
                                        "createdAt": "2025-01-01T10:00:00",
                                        "updatedAt": "2025-01-02T12:30:00"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "굿즈 정보를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": 2404,
                              "message": "굿즈 정보를 찾을 수 없습니다.",
                              "data": {
                                "code": 2404,
                                "message": "굿즈 정보를 찾을 수 없습니다."
                              }
                            }
                            """))
            )
    })
    public ResponseEntity<BaseResponse<GoodsItemResponse>> get(
            @Parameter(description = "팝업 ID", required = true)
            @PathVariable UUID popupId,
            @Parameter(description = "굿즈 ID", required = true)
            @PathVariable UUID goodsId
    ) {
        return ok(goodsService.get(popupId, goodsId));
    }

    @PutMapping("/{goodsId}")
    @Operation(summary = "굿즈 기본 정보 수정", description = "굿즈 이름/가격/재고를 수정합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "굿즈 수정 성공",
                    content = @Content(
                            schema = @Schema(implementation = GoodsIdResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "요청이 성공했습니다.",
                                      "data": {
                                        "id": "00000000-0000-0000-0000-000000000401"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(
                    responseCode = "404",
                    description = "굿즈 정보를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": 2404,
                              "message": "굿즈 정보를 찾을 수 없습니다.",
                              "data": {
                                "code": 2404,
                                "message": "굿즈 정보를 찾을 수 없습니다."
                              }
                            }
                            """))
            )
    })
    public ResponseEntity<BaseResponse<GoodsIdResponse>> update(
            @Parameter(description = "팝업 ID", required = true)
            @PathVariable UUID popupId,
            @Parameter(description = "굿즈 ID", required = true)
            @PathVariable UUID goodsId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "굿즈 수정 요청",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = GoodsUpdateRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "goodsName": "팝콘 키링",
                                      "goodsPrice": 12000,
                                      "stock": 100
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody GoodsUpdateRequest request
    ) {
        return ok(goodsService.update(popupId, goodsId, request));
    }

    @PatchMapping("/{goodsId}/status")
    @Operation(summary = "굿즈 상태 변경", description = "굿즈 활성/비활성 상태를 변경합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "굿즈 상태 변경 성공",
                    content = @Content(
                            schema = @Schema(implementation = GoodsStatusResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "변경되었습니다",
                                      "data": {
                                        "id": "00000000-0000-0000-0000-000000000401",
                                        "isActive": true
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(
                    responseCode = "404",
                    description = "굿즈 정보를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": 2404,
                              "message": "굿즈 정보를 찾을 수 없습니다.",
                              "data": {
                                "code": 2404,
                                "message": "굿즈 정보를 찾을 수 없습니다."
                              }
                            }
                            """))
            )
    })
    public ResponseEntity<BaseResponse<GoodsStatusResponse>> updateStatus(
            @Parameter(description = "팝업 ID", required = true)
            @PathVariable UUID popupId,
            @Parameter(description = "굿즈 ID", required = true)
            @PathVariable UUID goodsId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "굿즈 상태 변경 요청",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = GoodsStatusUpdateRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isActive": false
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody GoodsStatusUpdateRequest request
    ) {
        GoodsStatusResponse response = goodsService.updateStatus(popupId, goodsId, request);
        return ResponseEntity.ok(
                BaseResponse.of(
                        CommonResponseCode.SUCCESS.getCode(),
                        "변경되었습니다",
                        response
                )
        );
    }

    @DeleteMapping("/{goodsId}")
    @Operation(summary = "굿즈 삭제", description = "굿즈를 삭제(소프트 삭제)합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "굿즈 삭제 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": 200,
                              "message": "요청이 성공했습니다.",
                              "data": null
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "굿즈 정보를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": 2404,
                              "message": "굿즈 정보를 찾을 수 없습니다.",
                              "data": {
                                "code": 2404,
                                "message": "굿즈 정보를 찾을 수 없습니다."
                              }
                            }
                            """))
            )
    })
    public ResponseEntity<BaseResponse<Void>> delete(
            @Parameter(description = "팝업 ID", required = true)
            @PathVariable UUID popupId,
            @Parameter(description = "굿즈 ID", required = true)
            @PathVariable UUID goodsId
    ) {
        goodsService.delete(popupId, goodsId);
        return ok(null);
    }
}
