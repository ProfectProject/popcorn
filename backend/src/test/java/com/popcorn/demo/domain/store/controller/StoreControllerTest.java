package com.popcorn.demo.domain.store.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.time.LocalDateTime;
import java.util.*;

import com.popcorn.demo.domain.store.dto.StoreDetailDto;
import com.popcorn.demo.domain.store.dto.StoreListDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.popcorn.demo.domain.store.dto.StoreCreatedDto;
import com.popcorn.demo.domain.store.entity.StorePublishStatus;
import com.popcorn.demo.domain.store.exception.StoreException;
import com.popcorn.demo.domain.store.service.StoreService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StoreService storeService;

    @Test
    @DisplayName("스토어 생성 성공")
    void 스토어_생성_성공() throws Exception {
        UUID storeId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Long userId = 123L;
        
        StoreCreatedDto response = StoreCreatedDto.builder()
                .id(storeId)
                .name("맛있는 팝콘 스토어")
                .ownerId(userId)
                .publishStatus(StorePublishStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .createdBy(userId)
                .build();

        when(storeService.createStore(eq(userId), any())).thenReturn(response);

        String jsonRequest = """
                {
                    "name": "맛있는 팝콘 스토어",
                    "ownerId": 123
                }
                """;

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/owner/stores")
                        .with(user(userId.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(storeId.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.name").value("맛있는 팝콘 스토어"));
    }

    @Test
    @DisplayName("스토어 생성 실패 - 중복된 스토어 이름")
    void 스토어_생성_실패_중복된_이름() throws Exception {
        Long userId = 123L;
        
        when(storeService.createStore(eq(userId), any()))
                .thenThrow(StoreException.duplicateStoreName("맛있는 팝콘 스토어"));

        String jsonRequest = """
                {
                    "name": "맛있는 팝콘 스토어",
                    "ownerId": 123
                }
                """;

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/owner/stores")
                        .with(user(userId.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }

    @Test
    @DisplayName("스토어 생성 요청 시 서비스 호출 검증")
    void 스토어_생성_서비스_호출_검증() throws Exception {
        Long userId = 123L;
        UUID storeId = UUID.randomUUID();
        
        StoreCreatedDto response = StoreCreatedDto.builder()
                .id(storeId)
                .name("테스트 스토어")
                .ownerId(userId)
                .publishStatus(StorePublishStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .createdBy(userId)
                .build();

        when(storeService.createStore(eq(userId), any())).thenReturn(response);

        String jsonRequest = """
                {
                    "name": "테스트 스토어",
                    "ownerId": 123
                }
                """;

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/owner/stores")
                        .with(user(userId.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        verify(storeService).createStore(eq(userId), any());
    }

    // === 내 스토어 목록 조회 테스트 ===

    @Test
    @DisplayName("내 스토어 목록 조회 성공")
    void 내_스토어_목록_조회_성공() throws Exception {

        Long userId = 123L;

        List<StoreListDto> storeList = Arrays.asList(
                StoreListDto.builder()
                        .id(UUID.randomUUID())
                        .name("스토어1")
                        .publishStatus(StorePublishStatus.ACTIVE)
                        .build(),

                StoreListDto.builder()
                        .id(UUID.randomUUID())
                        .name("스토어2")
                        .publishStatus(StorePublishStatus.DRAFT)
                        .build(),

                StoreListDto.builder()
                        .id(UUID.randomUUID())
                        .name("스토어3")
                        .publishStatus(StorePublishStatus.PENDING)
                        .build()
        );

        when(storeService.getStoresByOwnerId(userId)).thenReturn(storeList);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/owner/stores")
                        .with(user(userId.toString())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].name").value("스토어1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].name").value("스토어2"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[2].name").value("스토어3"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].publishStatus").value("ACTIVE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].publishStatus").value("DRAFT"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[2].publishStatus").value("PENDING"));

    }

    @Test
    @DisplayName("내 스토어 목록 조회 성공 - 빈 목록")
    void 내_스토어_목록_조회_성공_빈_목록() throws Exception {

        Long userId = 123L;

        when(storeService.getStoresByOwnerId(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/owner/stores")
                        .with(user(userId.toString())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(0));

    }

    // === 스토어 상세 조회 테스트 ===

    @Test
    @DisplayName("스토어 상세 조회 성공")
    void 스토어_상세_조회_성공() throws Exception {
        // TODO: Given
        Long userId = 123L;
        UUID storeId = UUID.randomUUID();

        StoreDetailDto store = StoreDetailDto.builder()
                .id(storeId)
                .name("맛있는 팝콘 스토어")
                .ownerId(userId)
                .publishStatus(StorePublishStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(storeService.getStoreDetail(userId, storeId)).thenReturn(store);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/owner/stores/{storeId}", storeId)
                        .with(user(userId.toString())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(storeId.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.name").value("맛있는 팝콘 스토어"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.ownerId").value(userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.publishStatus").value("DRAFT"));

    }

    @Test
    @DisplayName("스토어 상세 조회 실패 - 존재하지 않는 스토어")
    void 스토어_상세_조회_실패_존재하지_않는_스토어() throws Exception {

        Long userId = 123L;
        UUID storeId = UUID.randomUUID();

        when(storeService.getStoreDetail(userId, storeId))
                .thenThrow(StoreException.storeNotFound(storeId));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/owner/stores/{storeId}", storeId)
                        .with(user(userId.toString())))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("스토어 상세 조회 실패 - 권한 없는 사용자")
    void 스토어_상세_조회_실패_권한_없는_사용자() throws Exception {

        Long userId = 123L;
        UUID storeId = UUID.randomUUID();

        when(storeService.getStoreDetail(userId, storeId))
                .thenThrow(StoreException.accessDenied(userId,storeId));

    }

    @Test
    @DisplayName("스토어 상세 조회 실패 - 잘못된 UUID 형식")
    void 스토어_상세_조회_실패_잘못된_UUID() throws Exception {
        // TODO: Path Variable에 "invalid-uuid-format" 전달
        Long userId = 123L;
        String storeId = "invalid-uuid-format";

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/owner/stores/{storeId}", storeId)
                        .with(user(userId.toString())))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

    }

    // === 서비스 호출 검증 테스트 ===

    @Test
    @DisplayName("내 스토어 목록 조회 시 서비스 호출 검증")
    void 내_스토어_목록_조회_서비스_호출_검증() throws Exception {

        Long userId = 123L;

        when(storeService.getStoresByOwnerId(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/owner/stores")
                        .with(user(userId.toString())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(0));

        verify(storeService).getStoresByOwnerId(eq(userId));

    }

    @Test
    @DisplayName("스토어 상세 조회 시 서비스 호출 검증")
    void 스토어_상세_조회_서비스_호출_검증() throws Exception {

        Long userId = 123L;
        UUID storeId = UUID.randomUUID();

        StoreDetailDto store = StoreDetailDto.builder()
                .id(storeId)
                .name("테스트 스토어")
                .ownerId(userId)
                .publishStatus(StorePublishStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .build();

        when(storeService.getStoreDetail(userId, storeId)).thenReturn(store);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/owner/stores/{storeId}", storeId)
                        .with(user(userId.toString())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(storeId.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.name").value("테스트 스토어"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.ownerId").value(userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.publishStatus").value("DRAFT"));

        verify(storeService).getStoreDetail(eq(userId), eq(storeId));
    }
}