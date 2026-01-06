package com.popcorn.demo.domain.popup.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import com.popcorn.demo.domain.popup.exception.PopupException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * PopupController 통합 테스트
 *
 * PopupControllerTestBase를 상속받아 standaloneSetup 방식으로 테스트합니다.
 * - 팝업 목록 조회 (필터링, 페이징)
 * - 팝업 상세 조회
 * - 예외 상황 처리
 */
@DisplayName("PopupController 통합 테스트")
class PopupControllerIntegrationTest extends PopupControllerTestBase {

    // 테스트 데이터 상수
    private static final UUID TEST_POPUP_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

    @Test
    @DisplayName("팝업 목록 조회 - 전체 목록 (필터 없음)")
    void getPopups_withoutFilters_success() throws Exception {
        // Given: 팝업 목록 응답 데이터 준비
        when(popupService.getPopups(any())).thenReturn(createPopupListResponse());

        // When & Then: API 호출 및 검증
        mockMvc.perform(get("/api/v1/popups")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].id").value(TEST_POPUP_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].title").value("테스트 팝업"))
                .andExpect(jsonPath("$.data.items[0].category").value("FOOD"))
                .andExpect(jsonPath("$.data.items[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("팝업 목록 조회 - 필터링")
    void getPopups_withFilters_success() throws Exception {
        // Given: 필터링된 팝업 목록
        when(popupService.getPopups(any())).thenReturn(createPopupListResponse());

        // When & Then: 필터 적용한 API 호출
        mockMvc.perform(get("/api/v1/popups")
                        .param("category", "FOOD")
                        .param("keyword", "테스트")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("팝업 상세 조회 - 정상 케이스")
    void getPopupDetail_success() throws Exception {
        // Given: 팝업 상세 정보
        when(popupService.getPopupDetail(any())).thenReturn(createPopupDetailResponse(TEST_POPUP_ID));

        // When & Then: 팝업 상세 조회 API 호출
        mockMvc.perform(get("/api/v1/popups/{popupId}", TEST_POPUP_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TEST_POPUP_ID.toString()))
                .andExpect(jsonPath("$.data.title").value("테스트 팝업"))
                .andExpect(jsonPath("$.data.description").value("테스트 설명"))
                .andExpect(jsonPath("$.data.category").value("FOOD"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    @DisplayName("팝업 상세 조회 - 존재하지 않는 팝업")
    void getPopupDetail_notFound() throws Exception {
        // Given: 존재하지 않는 팝업 ID
        UUID nonExistentId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        when(popupService.getPopupDetail(any()))
                .thenThrow(PopupException.popupNotFound());

        // When & Then: 404 에러 응답 검증
        mockMvc.perform(get("/api/v1/popups/{popupId}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());
    }
}