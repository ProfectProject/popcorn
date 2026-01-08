package com.popcorn.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(scripts = "classpath:sql/test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class SimpleUserFlowTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    @DisplayName("간단한 회원가입-로그인 플로우 테스트")
    void simpleSignupLoginFlow() throws Exception {
        // 1. 회원가입
        Map<String, Object> signupRequest = new HashMap<>();
        signupRequest.put("email", "simple@popcorn.com");
        signupRequest.put("password", "testPassword123!");
        signupRequest.put("passwordCheck", "testPassword123!");
        signupRequest.put("name", "간단테스트");
        signupRequest.put("phone", "01012345678");
        signupRequest.put("role", "CUSTOMER");

        ResultActions signupResult = mockMvc.perform(post("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)));

        signupResult.andDo(print())
                .andExpect(status().isOk());

        System.out.println("✅ 회원가입 완료");

        // 2. 로그인 시도
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "simple@popcorn.com");
        loginRequest.put("password", "testPassword123!");

        ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));

        loginResult.andDo(print());
        // 일단 상태 확인 없이 로그만 출력해서 실제 응답 확인

        System.out.println("✅ 로그인 시도 완료");
    }
}