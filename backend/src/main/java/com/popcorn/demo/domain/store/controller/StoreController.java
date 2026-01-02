package com.popcorn.demo.domain.store.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.store.dto.CreateStoreRequest;
import com.popcorn.demo.domain.store.dto.StoreCreatedDto;
import com.popcorn.demo.domain.store.service.StoreService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 스토어 관련 REST API를 처리하는 컨트롤러
 * BaseController를 상속받아 표준화된 응답 처리를 제공합니다.
 */
@Tag(name = "Stores", description = "스토어 관리 API")
@RestController
@RequestMapping("/api/v1/stores")
public class StoreController extends BaseController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }
    
    /**
     * 새로운 스토어를 생성합니다.
     *
     * @param request 스토어 생성 요청 데이터
     * @param userId 인증된 사용자 ID
     * @return 생성된 스토어 정보
     */
    @Operation(
        summary = "스토어 생성",
        description = "새로운 스토어를 생성합니다. 오너 권한이 필요합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "스토어 생성 성공",
            content = @Content(schema = @Schema(implementation = StoreCreatedDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (필수값 누락, 형식 오류, 비즈니스 검증 실패)"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "권한 없음 (오너 권한 필요)"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "중복된 스토어 이름"
        )
    })
    @PostMapping
    public ResponseEntity<BaseResponse<StoreCreatedDto>> createStore(
            @Parameter(description = "스토어 생성 요청 데이터", required = true)
            @Valid @RequestBody CreateStoreRequest request,
            
            @Parameter(description = "인증된 사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId) {

        // 스토어 생성
        StoreCreatedDto result = storeService.createStore(userId, request);
        
        // 성공 응답
        BaseResponse<StoreCreatedDto> response = BaseResponse.success(result);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /*
     * ==================== API 테스트용 Request Body 예시 ====================
     *
     * 스토어 생성 예시:
     * POST /api/v1/stores
     * Content-Type: application/json
     * X-User-Id: 123
     *
     * {
     *   "name": "맛있는 팝콘 스토어",
     *   "ownerId": 123
     * }
     *
     * ==================== 응답 예시 ====================
     *
     * 성공 응답 (201 Created):
     * {
     *   "code": "SUCCESS",
     *   "message": "요청이 성공했습니다.",
     *   "data": {
     *     "id": "550e8400-e29b-41d4-a716-446655440000",
     *     "name": "맛있는 팝콘 스토어",
     *     "ownerId": 123,
     *     "publishStatus": "DRAFT",
     *     "createdAt": "2025-01-02T10:15:30",
     *     "createdBy": 123
     *   }
     * }
     *
     * 오류 응답 (400 Bad Request):
     * {
     *   "code": "EMPTY_NAME",
     *   "message": "이름이 비어있습니다.",
     *   "data": null
     * }
     *
     * 권한 오류 (403 Forbidden):
     * {
     *   "code": "FORBIDDEN",
     *   "message": "스토어 생성 권한이 없습니다.",
     *   "data": null
     * }
     */
}