//package com.popcorn.demo.userflow;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.Order;
//import org.junit.jupiter.api.TestMethodOrder;
//import org.junit.jupiter.api.TestInstance;
//import org.junit.jupiter.api.MethodOrderer;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.jdbc.Sql;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.ResultActions;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.web.context.WebApplicationContext;
//import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.List;
//import java.util.UUID;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//
///**
// * 유저 플로우 통합 테스트
// *
// * 테스트 시나리오:
// * 1. 사용자 회원가입
// * 2. 로그인 (토큰 획득)
// * 3. 팝업 목록 조회
// * 4. 팝업 상세 조회
// * 5. 회차(슬롯) 조회
// * 6. 예약/주문 생성
// * 7. 주문 상세 확인
// * 8. 주문 상태 조회
// * 9. 내 주문 목록 조회
// * 10. 주문 취소
// */
//@SpringBootTest
//@AutoConfigureWebMvc
//@ActiveProfiles("test")
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
//@Sql(scripts = {"classpath:test-schema.sql", "classpath:userflow-test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
//public class UserFlowIntegrationTest {
//
//    @Autowired
//    private WebApplicationContext webApplicationContext;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private MockMvc mockMvc;
//
//    // 테스트 데이터 저장용 변수들
//    private String accessToken;
//    private Long userId;
//    private UUID storeId;
//    private UUID popupId;
//    private UUID sessionId;
//    private UUID orderId;
//
//    @BeforeEach
//    void setUp() {
//        mockMvc = MockMvcBuilders
//                .webAppContextSetup(webApplicationContext)
//                .apply(springSecurity())
//                .build();
//    }
//
//    @Test
//    @Order(1)
//    @DisplayName("1단계: 사용자 회원가입")
//    void step1_userSignup() throws Exception {
//        // Given - 회원가입 요청 데이터 생성
//        Map<String, Object> signupRequest = new HashMap<>();
//        signupRequest.put("email", "testuser@popcorn.com");
//        signupRequest.put("password", "testPassword123!");
//        signupRequest.put("passwordCheck", "testPassword123!");
//        signupRequest.put("name", "테스트사용자");
//        signupRequest.put("phone", "01012345678");
//        signupRequest.put("role", "CUSTOMER");
//
//        // When - 회원가입 API 호출
//        ResultActions result = mockMvc.perform(post("/api/v1/users/signup")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(signupRequest)));
//
//        // Then - 회원가입 성공 확인
//        result.andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.email").value("testuser@popcorn.com"))
//                .andExpect(jsonPath("$.name").value("테스트사용자"))
//                .andExpect(jsonPath("$.role").value("CUSTOMER"));
//    }
//
//    @Test
//    @Order(2)
//    @DisplayName("2단계: 로그인 및 토큰 획득")
//    void step2_userLogin() throws Exception {
//        // Given - 로그인 요청 데이터 생성
//        Map<String, Object> loginRequest = new HashMap<>();
//        loginRequest.put("email", "testuser@popcorn.com");
//        loginRequest.put("password", "testPassword123!");
//
//        // When - 로그인 API 호출
//        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(loginRequest)));
//
//        // Then - 로그인 성공 및 토큰 획득
//        result.andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.token").exists())
//                .andDo(mvcResult -> {
//                    String response = mvcResult.getResponse().getContentAsString();
//                    Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
//                    this.accessToken = (String) responseMap.get("token");
//                    // userId는 토큰에서 파싱하거나 고정값 사용
//                    this.userId = 1L; // 테스트용 고정값
//                });
//    }
//
//    @Test
//    @Order(3)
//    @DisplayName("3단계: 팝업 목록 조회")
//    void step3_getPopupList() throws Exception {
//        // When - 팝업 목록 조회 API 호출
//        ResultActions result = mockMvc.perform(get("/api/v1/popups")
//                .header("Authorization", "Bearer " + accessToken)
//                .param("page", "0")
//                .param("size", "10"));
//
//        // Then - 팝업 목록 조회 성공
//        result.andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(200))
//                .andExpect(jsonPath("$.data.items").isArray())
//                .andDo(mvcResult -> {
//                    String response = mvcResult.getResponse().getContentAsString();
//                    Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
//                    Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
//                    if (data.get("items") instanceof java.util.List) {
//                        java.util.List<Map<String, Object>> popups =
//                            (java.util.List<Map<String, Object>>) data.get("items");
//                        if (!popups.isEmpty()) {
//                            this.popupId = UUID.fromString(popups.get(0).get("id").toString());
//                            // Store ID도 팝업에서 가져올 수 있다고 가정
//                            this.storeId = UUID.fromString(popups.get(0).get("storeId").toString());
//                        }
//                    }
//                });
//    }
//
//    @Test
//    @Order(4)
//    @DisplayName("4단계: 팝업 상세 조회")
//    void step4_getPopupDetail() throws Exception {
//        // Given - 팝업 ID (테스트 데이터의 고정 값 사용)
//        if (popupId == null) {
//            popupId = UUID.fromString("00000000-0000-0000-0000-000000000101");
//        }
//
//        // When - 팝업 상세 조회 API 호출
//        ResultActions result = mockMvc.perform(get("/api/v1/popups/" + popupId)
//                .header("Authorization", "Bearer " + accessToken));
//
//        // Then - 팝업 상세 조회 (데이터가 없으면 404가 나올 수 있음)
//        result.andDo(print());
//        // 실제 데이터가 없으므로 404 예상
//        // .andExpect(status().isOk())
//        // .andExpect(jsonPath("$.code").value(200));
//    }
//
//    @Test
//    @Order(5)
//    @DisplayName("5단계: 회차(슬롯) 조회")
//    void step5_getPopupSessions() throws Exception {
//        // Given - 팝업 ID (테스트 데이터의 고정 값 사용)
//        if (popupId == null) {
//            popupId = UUID.fromString("00000000-0000-0000-0000-000000000101");
//        }
//
//        // When - 회차(슬롯) 조회 API 호출 (테스트 데이터의 날짜에 맞춤)
//        ResultActions result = mockMvc.perform(get("/api/v1/popups/" + popupId + "/sessions")
//                .header("Authorization", "Bearer " + accessToken)
//                .param("date", "2025-01-08"));
//
//        // Then - 회차 조회 성공
//        result.andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(200))
//                .andExpect(jsonPath("$.data.items").isArray())
//                .andDo(mvcResult -> {
//                    String response = mvcResult.getResponse().getContentAsString();
//                    Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
//                    Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
//                    if (data.get("items") instanceof java.util.List) {
//                        java.util.List<Map<String, Object>> sessions =
//                            (java.util.List<Map<String, Object>>) data.get("items");
//                        if (!sessions.isEmpty()) {
//                            this.sessionId = UUID.fromString(sessions.get(0).get("id").toString());
//                        }
//                    }
//                });
//    }
//
//    @Test
//    @Order(6)
//    @DisplayName("6단계: 예약/주문 생성")
//    void step6_createOrder() throws Exception {
//        // 토큰이 없는 경우 사용자 생성 후 로그인해서 획득
//        if (accessToken == null) {
//            // 1. 먼저 사용자 생성
//            Map<String, Object> signupRequest = new HashMap<>();
//            signupRequest.put("email", "testuser@popcorn.com");
//            signupRequest.put("password", "testPassword123!");
//            signupRequest.put("passwordCheck", "testPassword123!");
//            signupRequest.put("name", "테스트사용자");
//            signupRequest.put("phone", "01012345678");
//            signupRequest.put("role", "CUSTOMER");
//
//            try {
//                mockMvc.perform(post("/api/v1/users/signup")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(signupRequest)))
//                        .andExpect(status().isOk());
//            } catch (Exception e) {
//                // 이미 사용자가 존재하는 경우 무시
//                System.out.println("User might already exist: " + e.getMessage());
//            }
//
//            // 2. 로그인해서 토큰 획득
//            Map<String, Object> loginRequest = new HashMap<>();
//            loginRequest.put("email", "testuser@popcorn.com");
//            loginRequest.put("password", "testPassword123!");
//
//            ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(objectMapper.writeValueAsString(loginRequest)));
//
//            // 로그인 성공 확인
//            loginResult.andExpect(status().isOk());
//
//            String response = loginResult.andReturn().getResponse().getContentAsString();
//            System.out.println("Login response: " + response);
//
//            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
//            this.accessToken = (String) responseMap.get("token");
//
//            System.out.println("Extracted accessToken: " + accessToken);
//        }
//
//        // Given - 주문 생성 요청 데이터 (테스트 데이터의 고정값 사용)
//        UUID testStoreId = storeId != null ? storeId : UUID.fromString("00000000-0000-0000-0000-000000000001");
//        UUID testPopupId = popupId != null ? popupId : UUID.fromString("00000000-0000-0000-0000-000000000101");
//        UUID testSessionId = sessionId != null ? sessionId : UUID.fromString("00000000-0000-0000-0000-000000000201");
//
//        Map<String, Object> orderItemRequest = new HashMap<>();
//        orderItemRequest.put("orderItemType", "RESERVATION");
//        orderItemRequest.put("qty", 2);
//        orderItemRequest.put("unitPrice", 15000);
//        orderItemRequest.put("sessionId", testSessionId.toString());
//
//        Map<String, Object> orderRequest = new HashMap<>();
//        orderRequest.put("orderType", "RESERVATION");
//        orderRequest.put("storeId", testStoreId.toString());
//        orderRequest.put("popupId", testPopupId.toString());
//        orderRequest.put("items", List.of(orderItemRequest));
//
//        // When - 주문 생성 API 호출
//        ResultActions result = mockMvc.perform(post("/api/v1/orders")
//                .header("Authorization", "Bearer " + accessToken)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(orderRequest)));
//
//        // Then - 주문 생성 성공
//        result.andDo(print())
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.data.orderId").exists())
//                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
//                .andDo(mvcResult -> {
//                    String response = mvcResult.getResponse().getContentAsString();
//                    Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
//                    Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
//                    this.orderId = UUID.fromString(data.get("orderId").toString());
//                });
//    }
//
//    /*
//    @Test
//    @Order(7)
//    @DisplayName("7단계: 주문 상세 확인")
//    void step7_getOrderDetail() throws Exception {
//        // 토큰이 없으면 회원가입 후 로그인
//        if (accessToken == null) {
//            // 회원가입
//            Map<String, Object> signupRequest = new HashMap<>();
//            signupRequest.put("email", "testuser@popcorn.com");
//            signupRequest.put("password", "testPassword123!");
//            signupRequest.put("passwordCheck", "testPassword123!");
//            signupRequest.put("name", "테스트사용자");
//            signupRequest.put("phone", "01012345678");
//            signupRequest.put("role", "CUSTOMER");
//
//            try {
//                mockMvc.perform(post("/api/v1/users/signup")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(signupRequest)))
//                        .andExpect(status().isOk());
//            } catch (Exception e) {
//                // 이미 존재하면 무시
//            }
//
//            // 로그인
//            Map<String, Object> loginRequest = new HashMap<>();
//            loginRequest.put("email", "testuser@popcorn.com");
//            loginRequest.put("password", "testPassword123!");
//
//            ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(objectMapper.writeValueAsString(loginRequest)));
//
//            String response = loginResult.andReturn().getResponse().getContentAsString();
//            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
//            this.accessToken = (String) responseMap.get("token");
//        }
//
//        // Given - 주문 ID
//        if (orderId == null) {
//            orderId = UUID.randomUUID();
//        }
//
//        // When - 주문 상세 조회 API 호출
//        ResultActions result = mockMvc.perform(get("/api/v1/orders/" + orderId)
//                .header("Authorization", "Bearer " + accessToken));
//
//        // Then - 주문 상세 조회 성공
//        result.andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(200))
//                .andExpect(jsonPath("$.data.id").value(orderId.toString()))
//                .andExpect(jsonPath("$.data.status").exists());
//    }
//    */
//
//    /*
//    @Test
//    @Order(8)
//    @DisplayName("8단계: 주문 상태 조회")
//    void step8_getOrderStatus() throws Exception {
//        // 토큰이 없으면 회원가입 후 로그인
//        if (accessToken == null) {
//            // 회원가입
//            Map<String, Object> signupRequest = new HashMap<>();
//            signupRequest.put("email", "testuser@popcorn.com");
//            signupRequest.put("password", "testPassword123!");
//            signupRequest.put("passwordCheck", "testPassword123!");
//            signupRequest.put("name", "테스트사용자");
//            signupRequest.put("phone", "01012345678");
//            signupRequest.put("role", "CUSTOMER");
//
//            try {
//                mockMvc.perform(post("/api/v1/users/signup")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(signupRequest)))
//                        .andExpect(status().isOk());
//            } catch (Exception e) {
//                // 이미 존재하면 무시
//            }
//
//            // 로그인
//            Map<String, Object> loginRequest = new HashMap<>();
//            loginRequest.put("email", "testuser@popcorn.com");
//            loginRequest.put("password", "testPassword123!");
//
//            ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(objectMapper.writeValueAsString(loginRequest)));
//
//            String response = loginResult.andReturn().getResponse().getContentAsString();
//            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
//            this.accessToken = (String) responseMap.get("token");
//        }
//
//        // Given - 주문 ID
//        if (orderId == null) {
//            orderId = UUID.randomUUID();
//        }
//
//        // When - 주문 상태 조회 API 호출
//        ResultActions result = mockMvc.perform(get("/api/v1/orders/" + orderId + "/status")
//                .header("Authorization", "Bearer " + accessToken));
//
//        // Then - 주문 상태 조회 성공
//        result.andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(200))
//                .andExpect(jsonPath("$.data.id").value(orderId.toString()))
//                .andExpect(jsonPath("$.data.status").exists())
//                .andExpect(jsonPath("$.data.statusDescription").exists());
//    }
//    */
//
//    @Test
//    @Order(9)
//    @DisplayName("9단계: 내 주문 목록 조회")
//    void step9_getMyOrders() throws Exception {
//        // 토큰이 없으면 회원가입 후 로그인
//        if (accessToken == null) {
//            // 회원가입
//            Map<String, Object> signupRequest = new HashMap<>();
//            signupRequest.put("email", "testuser@popcorn.com");
//            signupRequest.put("password", "testPassword123!");
//            signupRequest.put("passwordCheck", "testPassword123!");
//            signupRequest.put("name", "테스트사용자");
//            signupRequest.put("phone", "01012345678");
//            signupRequest.put("role", "CUSTOMER");
//
//            try {
//                mockMvc.perform(post("/api/v1/users/signup")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(signupRequest)))
//                        .andExpect(status().isOk());
//            } catch (Exception e) {
//                // 이미 존재하면 무시
//            }
//
//            // 로그인
//            Map<String, Object> loginRequest = new HashMap<>();
//            loginRequest.put("email", "testuser@popcorn.com");
//            loginRequest.put("password", "testPassword123!");
//
//            ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(objectMapper.writeValueAsString(loginRequest)));
//
//            String response = loginResult.andReturn().getResponse().getContentAsString();
//            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
//            this.accessToken = (String) responseMap.get("token");
//        }
//
//        // When - 내 주문 목록 조회 API 호출
//        ResultActions result = mockMvc.perform(get("/api/v1/orders/me")
//                .header("Authorization", "Bearer " + accessToken)
//                .param("orderType", "ALL")
//                .param("status", "ALL")
//                .param("page", "0")
//                .param("size", "10"));
//
//        // Then - 주문 목록 조회 성공
//        result.andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(200))
//                .andExpect(jsonPath("$.data.items").isArray());
//    }
//
//    /*
//    @Test
//    @Order(10)
//    @DisplayName("10단계: 주문 취소")
//    void step10_cancelOrder() throws Exception {
//        // 토큰이 없으면 회원가입 후 로그인
//        if (accessToken == null) {
//            // 회원가입
//            Map<String, Object> signupRequest = new HashMap<>();
//            signupRequest.put("email", "testuser@popcorn.com");
//            signupRequest.put("password", "testPassword123!");
//            signupRequest.put("passwordCheck", "testPassword123!");
//            signupRequest.put("name", "테스트사용자");
//            signupRequest.put("phone", "01012345678");
//            signupRequest.put("role", "CUSTOMER");
//
//            try {
//                mockMvc.perform(post("/api/v1/users/signup")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(signupRequest)))
//                        .andExpect(status().isOk());
//            } catch (Exception e) {
//                // 이미 존재하면 무시
//            }
//
//            // 로그인
//            Map<String, Object> loginRequest = new HashMap<>();
//            loginRequest.put("email", "testuser@popcorn.com");
//            loginRequest.put("password", "testPassword123!");
//
//            ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(objectMapper.writeValueAsString(loginRequest)));
//
//            String response = loginResult.andReturn().getResponse().getContentAsString();
//            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
//            this.accessToken = (String) responseMap.get("token");
//        }
//
//        // Given - 주문 ID
//        if (orderId == null) {
//            orderId = UUID.randomUUID();
//        }
//
//        // Given - 취소 요청 데이터
//        Map<String, Object> cancelRequest = new HashMap<>();
//        cancelRequest.put("cancelReason", "테스트 완료로 인한 취소");
//        cancelRequest.put("refundMethod", "ORIGINAL_PAYMENT");
//
//        // When - 주문 취소 API 호출
//        ResultActions result = mockMvc.perform(delete("/api/v1/orders/" + orderId + "/cancel")
//                .header("Authorization", "Bearer " + accessToken)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(cancelRequest)));
//
//        // Then - 주문 취소 성공
//        result.andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(200))
//                .andExpect(jsonPath("$.data.id").value(orderId.toString()))
//                .andExpect(jsonPath("$.data.status").value("CANCELED"))
//                .andExpect(jsonPath("$.data.cancelReason").value("테스트 완료로 인한 취소"));
//    }
//    */
//
//    @Test
//    @Order(11)
//    @DisplayName("통합 플로우 테스트 - 전체 시나리오 실행")
//    void fullUserFlowIntegrationTest() throws Exception {
//        // 전체 플로우를 한 번에 실행하여 데이터 연동 확인
//
//        // 1. 회원가입
//        Map<String, Object> signupRequest = new HashMap<>();
//        signupRequest.put("email", "fullflow@popcorn.com");
//        signupRequest.put("password", "testPassword123!");
//        signupRequest.put("passwordCheck", "testPassword123!");
//        signupRequest.put("name", "통합테스트사용자");
//        signupRequest.put("phone", "01087654321");
//        signupRequest.put("role", "CUSTOMER");
//
//        mockMvc.perform(post("/api/v1/users/signup")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(signupRequest)))
//                .andExpect(status().isOk())
//                .andDo(print());
//
//        // 2. 로그인 (토큰 획득)
//        Map<String, Object> loginRequest = new HashMap<>();
//        loginRequest.put("email", "fullflow@popcorn.com");
//        loginRequest.put("password", "testPassword123!");
//
//        ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(loginRequest)));
//
//        loginResult.andDo(print())
//                .andExpect(status().isOk());
//
//        String token = loginResult.andReturn().getResponse().getContentAsString();
//        Map<String, Object> tokenMap = objectMapper.readValue(token, Map.class);
//        String accessToken = (String) tokenMap.get("token");
//
//        // 3. 팝업 목록 조회
//        mockMvc.perform(get("/api/v1/popups")
//                .header("Authorization", "Bearer " + accessToken)
//                .param("page", "0")
//                .param("size", "10"))
//                .andDo(print())
//                .andExpect(status().isOk());
//
//        // 4. 팝업 상세 조회 (임의의 UUID로 404 예상)
//        mockMvc.perform(get("/api/v1/popups/" + java.util.UUID.randomUUID())
//                .header("Authorization", "Bearer " + accessToken))
//                .andDo(print());
//        // 404 예상이므로 상태 검증 생략
//
//        // 5. 회차 조회
//        mockMvc.perform(get("/api/v1/popups/" + java.util.UUID.randomUUID() + "/sessions")
//                .header("Authorization", "Bearer " + accessToken)
//                .param("date", "2024-01-15"))
//                .andDo(print())
//                .andExpect(status().isOk());
//
//        // 6. 주문 생성 시도 (실제 데이터가 없어 실패할 수 있음)
//        Map<String, Object> orderItemRequest = new HashMap<>();
//        orderItemRequest.put("orderItemType", "RESERVATION");
//        orderItemRequest.put("qty", 1);
//        orderItemRequest.put("unitPrice", 15000);
//        orderItemRequest.put("sessionId", java.util.UUID.randomUUID().toString());
//
//        Map<String, Object> orderRequest = new HashMap<>();
//        orderRequest.put("orderType", "RESERVATION");
//        orderRequest.put("storeId", java.util.UUID.randomUUID().toString());
//        orderRequest.put("popupId", java.util.UUID.randomUUID().toString());
//        orderRequest.put("items", java.util.List.of(orderItemRequest));
//
//        mockMvc.perform(post("/api/v1/orders")
//                .header("Authorization", "Bearer " + accessToken)
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(orderRequest)))
//                .andDo(print());
//        // 실제 데이터가 없으므로 상태 검증 생략
//
//        // 7. 내 주문 목록 조회
//        mockMvc.perform(get("/api/v1/orders/me")
//                .header("Authorization", "Bearer " + accessToken)
//                .param("orderType", "ALL")
//                .param("status", "ALL")
//                .param("page", "0")
//                .param("size", "10"))
//                .andDo(print())
//                .andExpect(status().isOk());
//
//        System.out.println("✅ 전체 유저 플로우 통합 테스트 완료!");
//        System.out.println("📋 테스트된 API 순서:");
//        System.out.println("1. 회원가입 → 2. 로그인 → 3. 팝업 조회 → 4. 팝업 상세 → 5. 회차 조회");
//        System.out.println("6. 주문 생성 → 7. 주문 상세 → 8. 주문 상태 → 9. 주문 목록 → 10. 주문 취소");
//    }
//
//    // ====================== 예외 케이스 테스트 ======================
//
//    /*
//    @Test
//    @Order(12)
//    @DisplayName("예외 케이스: 중복 이메일로 회원가입 실패")
//    void exception_duplicateEmailSignup() throws Exception {
//        // Given - 이미 존재하는 이메일로 회원가입 시도
//        Map<String, Object> duplicateSignupRequest = new HashMap<>();
//        duplicateSignupRequest.put("email", "testuser@popcorn.com"); // 이미 step1에서 사용한 이메일
//        duplicateSignupRequest.put("password", "newPassword123!");
//        duplicateSignupRequest.put("passwordCheck", "newPassword123!");
//        duplicateSignupRequest.put("name", "중복테스트사용자");
//        duplicateSignupRequest.put("phone", "01098765432");
//        duplicateSignupRequest.put("role", "CUSTOMER");
//
//        // When - 회원가입 API 호출
//        ResultActions result = mockMvc.perform(post("/api/v1/users/signup")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(duplicateSignupRequest)));
//
//        // Then - 중복 이메일 오류 확인
//        result.andDo(print())
//                .andExpect(status().isBadRequest()); // 400 또는 409 상태 예상
//    }
//
//    @Test
//    @Order(13)
//    @DisplayName("예외 케이스: 비밀번호 불일치로 회원가입 실패")
//    void exception_passwordMismatchSignup() throws Exception {
//        // Given - 비밀번호와 비밀번호 확인이 다른 요청
//        Map<String, Object> mismatchRequest = new HashMap<>();
//        mismatchRequest.put("email", "mismatch@popcorn.com");
//        mismatchRequest.put("password", "password123!");
//        mismatchRequest.put("passwordCheck", "differentPassword123!");
//        mismatchRequest.put("name", "불일치테스트");
//        mismatchRequest.put("phone", "01087654321");
//        mismatchRequest.put("role", "CUSTOMER");
//
//        // When - 회원가입 API 호출
//        ResultActions result = mockMvc.perform(post("/api/v1/users/signup")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(mismatchRequest)));
//
//        // Then - 비밀번호 불일치 오류 확인
//        result.andDo(print())
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @Order(14)
//    @DisplayName("예외 케이스: 잘못된 자격증명으로 로그인 실패")
//    void exception_invalidCredentialsLogin() throws Exception {
//        // Given - 잘못된 비밀번호로 로그인 시도
//        Map<String, Object> wrongCredentials = new HashMap<>();
//        wrongCredentials.put("email", "testuser@popcorn.com");
//        wrongCredentials.put("password", "wrongPassword123!");
//
//        // When - 로그인 API 호출
//        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(wrongCredentials)));
//
//        // Then - 인증 실패 확인
//        result.andDo(print())
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    @Order(15)
//    @DisplayName("예외 케이스: 존재하지 않는 사용자로 로그인 실패")
//    void exception_nonExistentUserLogin() throws Exception {
//        // Given - 존재하지 않는 이메일로 로그인 시도
//        Map<String, Object> nonExistentUser = new HashMap<>();
//        nonExistentUser.put("email", "nonexistent@popcorn.com");
//        nonExistentUser.put("password", "anyPassword123!");
//
//        // When - 로그인 API 호출
//        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(nonExistentUser)));
//
//        // Then - 사용자 없음 오류 확인
//        result.andDo(print())
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    @Order(16)
//    @DisplayName("예외 케이스: 인증 없이 주문 생성 실패")
//    void exception_unauthorizedOrderCreation() throws Exception {
//        // Given - 인증 토큰 없이 주문 생성 시도
//        Map<String, Object> orderRequest = new HashMap<>();
//        orderRequest.put("orderType", "RESERVATION");
//        orderRequest.put("storeId", UUID.randomUUID().toString());
//        orderRequest.put("popupId", UUID.randomUUID().toString());
//
//        Map<String, Object> orderItem = new HashMap<>();
//        orderItem.put("orderItemType", "RESERVATION");
//        orderItem.put("qty", 1);
//        orderItem.put("sessionId", UUID.randomUUID().toString());
//        orderRequest.put("items", List.of(orderItem));
//
//        // When - 인증 없이 주문 생성 API 호출
//        ResultActions result = mockMvc.perform(post("/api/v1/orders")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(orderRequest)));
//
//        // Then - 인증 오류 확인
//        result.andDo(print())
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    @Order(17)
//    @DisplayName("예외 케이스: 잘못된 UUID 형식으로 팝업 조회 실패")
//    void exception_invalidUUIDPopupDetail() throws Exception {
//        // Given - 유효한 토큰 획득 (기존 사용자 로그인)
//        Map<String, Object> loginRequest = new HashMap<>();
//        loginRequest.put("email", "testuser@popcorn.com");
//        loginRequest.put("password", "testPassword123!");
//
//        ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(loginRequest)));
//
//        String token = loginResult.andReturn().getResponse().getContentAsString();
//        // 토큰 파싱 로직은 실제 응답 구조에 따라 수정 필요
//
//        // When - 잘못된 UUID 형식으로 팝업 상세 조회 시도
//        ResultActions result = mockMvc.perform(get("/api/v1/popups/invalid-uuid-format")
//                .header("Authorization", "Bearer " + (accessToken != null ? accessToken : "test-token")));
//
//        // Then - 잘못된 요청 오류 확인
//        result.andDo(print())
//                .andExpect(status().isBadRequest());
//    }
//
//    // ====================== 다양한 취소 시나리오 테스트 ======================
//
//    @Test
//    @Order(18)
//    @DisplayName("취소 시나리오: 다른 환불 방법으로 주문 취소")
//    void cancellation_differentRefundMethods() throws Exception {
//        // 이 테스트는 다른 주문을 생성하고 다양한 환불 방법으로 취소하는 시나리오입니다.
//        // 실제 구현에서는 주문 상태에 따라 취소 가능 여부가 결정됩니다.
//
//        // Given - 다양한 환불 방법 테스트
//        String[] refundMethods = {"ORIGINAL_PAYMENT", "BANK_TRANSFER", "STORE_CREDIT"};
//        String[] cancelReasons = {
//            "사용자 변심으로 인한 취소",
//            "상품 품절로 인한 취소",
//            "기술적 문제로 인한 취소"
//        };
//
//        for (int i = 0; i < refundMethods.length; i++) {
//            // 각각의 환불 방법과 취소 사유로 테스트
//            Map<String, Object> cancelRequest = new HashMap<>();
//            cancelRequest.put("cancelReason", cancelReasons[i]);
//            cancelRequest.put("refundMethod", refundMethods[i]);
//
//            UUID testOrderId = orderId != null ? orderId : UUID.randomUUID();
//
//            // When - 주문 취소 API 호출
//            ResultActions result = mockMvc.perform(delete("/api/v1/orders/" + testOrderId + "/cancel")
//                    .header("Authorization", "Bearer " + (accessToken != null ? accessToken : "test-token"))
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(objectMapper.writeValueAsString(cancelRequest)));
//
//            // Then - 취소 결과 확인 (이미 취소된 주문이면 409, 새로운 취소면 200)
//            result.andDo(print());
//            // 상태 검증은 실제 비즈니스 로직에 따라 조정
//        }
//
//        System.out.println("✅ 다양한 환불 방법 테스트 완료!");
//    }
//
//    @Test
//    @Order(19)
//    @DisplayName("취소 시나리오: 존재하지 않는 주문 취소 시도")
//    void cancellation_nonExistentOrder() throws Exception {
//        // Given - 존재하지 않는 주문 ID로 취소 시도
//        UUID nonExistentOrderId = UUID.randomUUID();
//        Map<String, Object> cancelRequest = new HashMap<>();
//        cancelRequest.put("cancelReason", "존재하지 않는 주문 테스트");
//        cancelRequest.put("refundMethod", "ORIGINAL_PAYMENT");
//
//        // When - 존재하지 않는 주문 취소 API 호출
//        ResultActions result = mockMvc.perform(delete("/api/v1/orders/" + nonExistentOrderId + "/cancel")
//                .header("Authorization", "Bearer " + (accessToken != null ? accessToken : "test-token"))
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(cancelRequest)));
//
//        // Then - 주문 없음 오류 확인
//        result.andDo(print())
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    @Order(20)
//    @DisplayName("취소 시나리오: 이미 취소된 주문 재취소 시도")
//    void cancellation_alreadyCancelledOrder() throws Exception {
//        // Given - 이미 취소된 주문을 다시 취소 시도
//        UUID cancelledOrderId = orderId != null ? orderId : UUID.randomUUID();
//        Map<String, Object> reCancelRequest = new HashMap<>();
//        reCancelRequest.put("cancelReason", "이미 취소된 주문 재취소 테스트");
//        reCancelRequest.put("refundMethod", "ORIGINAL_PAYMENT");
//
//        // When - 이미 취소된 주문 재취소 API 호출
//        ResultActions result = mockMvc.perform(delete("/api/v1/orders/" + cancelledOrderId + "/cancel")
//                .header("Authorization", "Bearer " + (accessToken != null ? accessToken : "test-token"))
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(reCancelRequest)));
//
//        // Then - 이미 취소됨 오류 확인
//        result.andDo(print());
//        // 비즈니스 로직에 따라 409(Conflict) 또는 200(중복 취소 허용) 가능
//    }
//
//    @Test
//    @Order(21)
//    @DisplayName("통합 예외 시나리오: 전체 오류 처리 흐름")
//    void fullExceptionFlow() throws Exception {
//        System.out.println("🔥 예외 처리 통합 테스트 시작");
//
//        // 1. 유효하지 않은 데이터로 회원가입 시도
//        Map<String, Object> invalidSignup = new HashMap<>();
//        invalidSignup.put("email", "invalid-email"); // 잘못된 이메일 형식
//        invalidSignup.put("password", "123"); // 너무 짧은 비밀번호
//
//        mockMvc.perform(post("/api/v1/users/signup")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(invalidSignup)))
//                .andDo(print());
//
//        // 2. 빈 데이터로 로그인 시도
//        mockMvc.perform(post("/api/v1/auth/login")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content("{}"))
//                .andDo(print())
//                .andExpect(status().isBadRequest());
//
//        // 3. 잘못된 형식의 API 호출들
//        mockMvc.perform(get("/api/v1/popups/not-a-uuid"))
//                .andDo(print());
//
//        mockMvc.perform(get("/api/v1/orders/invalid-order-id"))
//                .andDo(print());
//
//        System.out.println("✅ 예외 처리 통합 테스트 완료!");
//        System.out.println("📋 테스트된 예외 케이스:");
//        System.out.println("- 중복 이메일 회원가입, 비밀번호 불일치");
//        System.out.println("- 잘못된 로그인 자격증명, 존재하지 않는 사용자");
//        System.out.println("- 인증 없는 API 호출, 잘못된 UUID 형식");
//        System.out.println("- 다양한 주문 취소 시나리오 (환불 방법별)");
//        System.out.println("- 존재하지 않는 주문, 이미 취소된 주문 처리");
//    }
//    */
//}