package com.popcorn.demo.domain.store.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.time.LocalDateTime;
import java.util.UUID;

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
}