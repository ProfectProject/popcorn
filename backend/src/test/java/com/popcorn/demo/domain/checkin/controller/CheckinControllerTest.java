package com.popcorn.demo.domain.checkin.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.checkin.dto.response.CheckinDetailResponse;
import com.popcorn.demo.domain.checkin.dto.response.CheckinListResponse;
import com.popcorn.demo.domain.checkin.service.CheckinService;
import com.popcorn.demo.global.config.CommonConfig;

class CheckinControllerTest {

	private MockMvc mockMvc;
	private CheckinService checkinService;

	@BeforeEach
	void setUp() {
		checkinService = Mockito.mock(CheckinService.class);
		ObjectMapper objectMapper = new CommonConfig().objectMapper();

		CheckinController controller = new CheckinController(checkinService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new CheckinExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	@DisplayName("체크인 목록 조회 성공")
	void getCheckins_success() throws Exception {
		UUID checkinId = UUID.fromString("90000000-0000-0000-0000-000000000101");
		UUID orderId = UUID.fromString("40000000-0000-0000-0000-000000000004");
		CheckinListResponse.Item item = CheckinListResponse.Item.builder()
				.checkinId(checkinId)
				.orderId(orderId)
				.qrCode("qr-list-001")
				.createdAt(LocalDateTime.now())
				.build();
		CheckinListResponse response = CheckinListResponse.builder()
				.count(1)
				.items(List.of(item))
				.build();

		Mockito.when(checkinService.getCheckins()).thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/checkins"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.count").value(1))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].checkinId").value(checkinId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].orderId").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].qrCode").value("qr-list-001"));
	}

	@Test
	@DisplayName("체크인 상세 조회 성공")
	void getCheckin_success() throws Exception {
		UUID checkinId = UUID.fromString("90000000-0000-0000-0000-000000000102");
		UUID orderId = UUID.fromString("40000000-0000-0000-0000-000000000005");
		UUID qrId = UUID.fromString("80000000-0000-0000-0000-000000000001");
		CheckinDetailResponse response = CheckinDetailResponse.builder()
				.checkinId(checkinId)
				.orderId(orderId)
				.orderQrCodeId(qrId)
				.qrCode("qr-detail-001")
				.createdAt(LocalDateTime.now())
				.createdBy(1001L)
				.build();

		Mockito.when(checkinService.getCheckin(checkinId)).thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/checkins/{checkinId}", checkinId))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.checkinId").value(checkinId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.orderQrCodeId").value(qrId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.qrCode").value("qr-detail-001"));
	}
}
