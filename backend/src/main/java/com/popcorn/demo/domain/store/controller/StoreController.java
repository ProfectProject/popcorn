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

@Tag(name = "Stores", description = "스토어 관리 API")
@RestController
@RequestMapping("/api/v1/stores")
public class StoreController extends BaseController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }
    
    @Operation(summary = "스토어 생성", description = "새로운 스토어를 생성합니다. 오너 권한이 필요합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "스토어 생성 성공", 
                    content = @Content(schema = @Schema(implementation = StoreCreatedDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "409", description = "중복된 스토어 이름")
    })
    @PostMapping
    public ResponseEntity<BaseResponse<StoreCreatedDto>> createStore(
            @Parameter(description = "스토어 생성 요청 데이터", required = true) @Valid @RequestBody CreateStoreRequest request,
            @Parameter(description = "인증된 사용자 ID", required = true) @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(storeService.createStore(userId, request)));
    }
}