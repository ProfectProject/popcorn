package com.popcorn.demo.domain.payment.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.versioning.ApiVersion;

@RestController
@ApiVersion("v1")
@RequestMapping("/api/v1/payments/webhooks")
public class TossWebhookController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(TossWebhookController.class);

	@PostMapping("/toss")
	public ResponseEntity<BaseResponse<String>> handleTossWebhook(@RequestBody Map<String, Object> payload) {
		log.info("🔔 토스 웹훅 수신 - payload: {}", payload);
		return ok("ok");
	}
}
