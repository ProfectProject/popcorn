package com.popcorn.demo.domain.qr.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.versioning.ApiVersion;
import com.popcorn.demo.domain.qr.dto.request.QrVerifyRequest;
import com.popcorn.demo.domain.qr.dto.response.QrCodeResponse;
import com.popcorn.demo.domain.qr.dto.response.QrVerifyResponse;
import com.popcorn.demo.domain.qr.service.QrCodeService;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@Tag(name = "QR", description = "QR 코드 관리 API")
@ApiVersion("v1")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class QrController extends BaseController {

	private final QrCodeService qrCodeService;

	@Operation(
			summary = "QR 발급",
			description = "PAID 상태의 주문에 대해 QR 코드를 발급합니다."
	)
	@PostMapping("/orders/{orderId}/qr")
	public ResponseEntity<BaseResponse<QrCodeResponse>> issueQr(
			@Parameter(description = "주문 ID", example = "00000000-0000-0000-0000-000000001001")
			@PathVariable UUID orderId) {
		QrCodeResponse response = qrCodeService.issue(orderId);
		return ok(response);
	}

	@Operation(
			summary = "QR 조회",
			description = "PAID 상태의 주문에 연결된 QR 코드를 조회합니다."
	)
	@GetMapping("/orders/{orderId}/qr")
	public ResponseEntity<BaseResponse<QrCodeResponse>> getQr(
			@Parameter(description = "주문 ID", example = "00000000-0000-0000-0000-000000001001")
			@PathVariable UUID orderId) {
		QrCodeResponse response = qrCodeService.get(orderId);
		return ok(response);
	}

	@Operation(
			summary = "QR 검증",
			description = "스캐너 전용 QR 코드 검증 API입니다."
	)
	@PostMapping("/qr/verify")
	public ResponseEntity<BaseResponse<QrVerifyResponse>> verifyQr(
			@Valid @RequestBody QrVerifyRequest request) {
		QrVerifyResponse response = qrCodeService.verify(request.getQrCode());
		return ok(response);
	}
}
