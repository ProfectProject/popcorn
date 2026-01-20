package com.popcorn.checkIns.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.checkIns.dto.response.QrCodeResponse;
import com.popcorn.checkIns.dto.response.QrVerifyResponse;
import com.popcorn.checkIns.service.QrCodeService;
import com.popcorn.demo.common.config.CommonConfig;

class QrControllerTest {

	private MockMvc mockMvc;
	private QrCodeService qrCodeService;

	@BeforeEach
	void setUp() {
		qrCodeService = Mockito.mock(QrCodeService.class);
		ObjectMapper objectMapper = new CommonConfig().objectMapper();

		QrController controller = new QrController(qrCodeService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new QrExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	@DisplayName("QR 발급 성공")
	void issueQr_success() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001001");
		QrCodeResponse response = QrCodeResponse.builder()
				.orderId(orderId)
				.qrCode("qr-test-001")
				.expiresAt(LocalDateTime.now().plusMinutes(10))
				.build();

		when(qrCodeService.issue(orderId)).thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders/{orderId}/qr", orderId))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.qrCode").value("qr-test-001"));
	}

	@Test
	@DisplayName("QR 조회 성공")
	void getQr_success() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001002");
		QrCodeResponse response = QrCodeResponse.builder()
				.orderId(orderId)
				.qrCode("qr-test-002")
				.expiresAt(LocalDateTime.now().plusMinutes(10))
				.build();

		when(qrCodeService.get(orderId)).thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/{orderId}/qr", orderId))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.qrCode").value("qr-test-002"));
	}

	@Test
	@DisplayName("QR 검증 성공")
	void verifyQr_success() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001003");
		UUID checkinId = UUID.fromString("90000000-0000-0000-0000-000000000001");
		QrVerifyResponse response = QrVerifyResponse.builder()
				.valid(true)
				.checkinId(checkinId)
				.orderId(orderId)
				.qrCode("qr-test-003")
				.expiresAt(LocalDateTime.now().plusMinutes(10))
				.build();

		when(qrCodeService.verify(any())).thenReturn(response);

		String requestJson = """
				{
					"qrCode": "qr-test-003"
				}
				""";

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/qr/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.valid").value(true))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.checkinId").value(checkinId.toString()));
	}

	@Test
	@DisplayName("QR 검증 실패 - 요청값 누락")
	void verifyQr_fail_blank() throws Exception {
		String requestJson = """
				{
					"qrCode": ""
				}
				""";

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/qr/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400));
	}
}
