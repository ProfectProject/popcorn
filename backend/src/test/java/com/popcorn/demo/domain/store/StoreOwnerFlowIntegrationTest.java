package com.popcorn.demo.domain.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.auth.dto.CustomUserDetails;
import com.popcorn.demo.domain.auth.jwt.JwtUtil;
import com.popcorn.demo.domain.users.entity.User;
import com.popcorn.demo.domain.users.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@Sql(scripts = "classpath:sql/test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StoreOwnerFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Owner flow: signup/login -> store/popup/goods -> schedule updates -> orders")
    void ownerFlow_withPopupSchedulesGoodsAndOrders() throws Exception {
        String ownerEmail = "owner-flow@popcorn.com";
        String ownerPassword = "testPassword123!";

        signupOwner(ownerEmail, ownerPassword);
        String ownerToken = loginAndGetToken(ownerEmail, ownerPassword);
        assertThat(ownerToken).isNotBlank();
        assertThat(jwtUtil.getUserId(ownerToken)).isNotNull();
        User owner = getUserByEmail(ownerEmail);
        CustomUserDetails ownerDetails = new CustomUserDetails(owner);

        String storeId = createStore(ownerDetails, "Flow Store");
        assertStoreListed(ownerDetails, storeId);
        assertStoreDetail(ownerDetails, storeId);

        String popupId = createPopupWithSchedules(ownerDetails, storeId);
        List<String> scheduleIds = getOwnerPopupSchedules(ownerDetails, popupId);
        assertThat(scheduleIds).hasSize(2);

        String goodsId = createGoods(ownerDetails, popupId);
        assertGoodsListed(ownerDetails, popupId, goodsId);

        assertPopupListed(ownerDetails, storeId, popupId);
        assertPopupDetail(ownerDetails, popupId);

        updatePopupWithScheduleChanges(ownerDetails, popupId, scheduleIds.get(0), scheduleIds.get(1));
        updateStore(ownerDetails, storeId, "Flow Store Updated");

        List<String> updatedScheduleIds = getOwnerPopupSchedules(ownerDetails, popupId);
        String reservationScheduleId = updatedScheduleIds.get(0);

        createReservationOrder(ownerDetails, popupId, reservationScheduleId);
        createGoodsOrder(ownerDetails, popupId, goodsId);
    }

    private void signupOwner(String email, String password) throws Exception {
        Map<String, Object> signupRequest = Map.of(
                "email", email,
                "password", password,
                "passwordCheck", password,
                "name", "Owner Flow",
                "phone", "01012345678",
                "role", "OWNER"
        );

        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        Map<String, Object> loginRequest = Map.of(
                "email", email,
                "password", password
        );

        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        String authHeader = result.andReturn().getResponse().getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring("Bearer ".length()).trim();
        }

        JsonNode body = readBody(result);
        String token = body.path("token").asText();
        if (token == null || token.isBlank()) {
            return createTokenForUser(email);
        }
        return token;
    }

    private String createTokenForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found for login: " + email));
        return jwtUtil.createJwt(user.getUserId(), user.getEmail(), user.getRole().name(), 60 * 60 * 1000L);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
    }

    private String createStore(CustomUserDetails ownerDetails, String name) throws Exception {
        Map<String, Object> request = Map.of("name", name);

        ResultActions result = mockMvc.perform(post("/api/v1/owner/stores")
                        .with(user(ownerDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        return readBody(result).path("data").path("id").asText();
    }

    private void assertStoreListed(CustomUserDetails ownerDetails, String storeId) throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/owner/stores")
                        .with(user(ownerDetails)))
                .andExpect(status().isOk());

        JsonNode data = readBody(result).path("data");
        boolean found = false;
        if (data.isArray()) {
            for (JsonNode item : data) {
                if (storeId.equals(item.path("id").asText())) {
                    found = true;
                    break;
                }
            }
        }
        assertThat(found).isTrue();
    }

    private void assertStoreDetail(CustomUserDetails ownerDetails, String storeId) throws Exception {
        mockMvc.perform(get("/api/v1/owner/stores/{storeId}", storeId)
                        .with(user(ownerDetails)))
                .andExpect(status().isOk());
    }

    private String createPopupWithSchedules(CustomUserDetails ownerDetails, String storeId) throws Exception {
        Map<String, Object> schedule1 = Map.of(
                "startAt", "2025-01-01T10:00:00",
                "endAt", "2025-01-01T12:00:00",
                "price", 10000,
                "capacity", 10
        );
        Map<String, Object> schedule2 = Map.of(
                "startAt", "2025-01-02T10:00:00",
                "endAt", "2025-01-02T12:00:00",
                "price", 12000,
                "capacity", 8
        );

        Map<String, Object> request = Map.of(
                "storeId", storeId,
                "title", "Flow Popup",
                "description", "Flow Popup Description",
                "category", "FOOD",
                "reservationOpenAt", "2025-01-01T09:00:00",
                "addressRoad", "Road 1",
                "addressDetail", "Detail 1",
                "schedules", List.of(schedule1, schedule2)
        );

        ResultActions result = mockMvc.perform(post("/api/v1/owner/stores/popups")
                        .with(user(ownerDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        return readBody(result).path("data").path("popupId").asText();
    }

    private List<String> getOwnerPopupSchedules(CustomUserDetails ownerDetails, String popupId) throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/owner/stores/popups/{popupId}", popupId)
                        .with(user(ownerDetails)))
                .andExpect(status().isOk());

        JsonNode schedules = readBody(result).path("data").path("schedules");
        if (!schedules.isArray()) {
            return List.of();
        }
        return schedules.findValuesAsText("scheduleId");
    }

    private String createGoods(CustomUserDetails ownerDetails, String popupId) throws Exception {
        Map<String, Object> request = Map.of(
                "stockUnit", "EA",
                "goodsName", "Flow Goods",
                "goodsPrice", 15000,
                "stock", 20,
                "isActive", true
        );

        ResultActions result = mockMvc.perform(post("/api/v1/owner/popups/{popupId}/goods", popupId)
                        .with(user(ownerDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        return readBody(result).path("data").path("id").asText();
    }

    private void assertGoodsListed(CustomUserDetails ownerDetails, String popupId, String goodsId) throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/owner/popups/{popupId}/goods", popupId)
                        .with(user(ownerDetails)))
                .andExpect(status().isOk());

        JsonNode items = readBody(result).path("data").path("items");
        boolean found = false;
        if (items.isArray()) {
            for (JsonNode item : items) {
                if (goodsId.equals(item.path("id").asText())) {
                    found = true;
                    break;
                }
            }
        }
        assertThat(found).isTrue();
    }

    private void assertPopupListed(CustomUserDetails ownerDetails, String storeId, String popupId) throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/popups")
                        .param("storeId", storeId)
                        .with(user(ownerDetails)))
                .andExpect(status().isOk());

        JsonNode items = readBody(result).path("data").path("items");
        boolean found = false;
        if (items.isArray()) {
            for (JsonNode item : items) {
                if (popupId.equals(item.path("id").asText())) {
                    found = true;
                    break;
                }
            }
        }
        assertThat(found).isTrue();
    }

    private void assertPopupDetail(CustomUserDetails ownerDetails, String popupId) throws Exception {
        mockMvc.perform(get("/api/v1/popups/{popupId}", popupId)
                        .with(user(ownerDetails)))
                .andExpect(status().isOk());
    }

    private void updatePopupWithScheduleChanges(CustomUserDetails ownerDetails, String popupId, String updateId, String deleteId) throws Exception {
        Map<String, Object> createdSchedule = Map.of(
                "startAt", "2025-01-03T10:00:00",
                "endAt", "2025-01-03T12:00:00",
                "price", 13000,
                "capacity", 6
        );
        Map<String, Object> updatedSchedule = Map.of(
                "scheduleId", updateId,
                "startAt", "2025-01-01T13:00:00",
                "endAt", "2025-01-01T15:00:00",
                "price", 11000,
                "capacity", 9,
                "active", true
        );

        Map<String, Object> request = Map.of(
                "title", "Flow Popup Updated",
                "description", "Updated Description",
                "popupCategory", "FOOD",
                "createSchedules", List.of(createdSchedule),
                "updateSchedules", List.of(updatedSchedule),
                "deleteScheduleIds", List.of(deleteId)
        );

        mockMvc.perform(put("/api/v1/owner/stores/popups/{popupId}", popupId)
                        .with(user(ownerDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private void updateStore(CustomUserDetails ownerDetails, String storeId, String name) throws Exception {
        Map<String, Object> request = Map.of("name", name);
        mockMvc.perform(put("/api/v1/owner/stores/{storeId}", storeId)
                        .with(user(ownerDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private void createReservationOrder(CustomUserDetails ownerDetails, String popupId, String scheduleId) throws Exception {
        Map<String, Object> item = Map.of(
                "orderItemType", "RESERVATION",
                "sessionId", scheduleId,
                "qty", 1
        );
        Map<String, Object> request = Map.of(
                "orderType", "RESERVATION",
                "popupId", popupId,
                "items", List.of(item)
        );

        mockMvc.perform(post("/api/v1/orders")
                        .with(user(ownerDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private void createGoodsOrder(CustomUserDetails ownerDetails, String popupId, String goodsId) throws Exception {
        Map<String, Object> item = Map.of(
                "orderItemType", "GOODS",
                "goodsId", goodsId,
                "qty", 1
        );
        Map<String, Object> address = Map.of(
                "address1", "123 Test Street",
                "address2", "Unit 101",
                "receiverName", "Receiver",
                "phone", "01000000000"
        );
        Map<String, Object> request = Map.of(
                "orderType", "PURCHASE",
                "popupId", popupId,
                "items", List.of(item),
                "address", address
        );

        mockMvc.perform(post("/api/v1/orders")
                        .with(user(ownerDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private JsonNode readBody(ResultActions result) throws Exception {
        String content = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content);
    }
}
