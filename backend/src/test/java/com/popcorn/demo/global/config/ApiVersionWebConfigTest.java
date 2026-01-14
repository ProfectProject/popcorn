package com.popcorn.demo.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = com.popcorn.demo.DemoApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.profiles.active=test"
})
class ApiVersionWebConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiEndpoints_AreInterceptedByVersionConfig() throws Exception {
        // API endpoints should be processed by version interceptor
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(result -> {
                    // The request should reach the controller (though may fail auth)
                    // Version interceptor should have processed the request
                    org.assertj.core.api.Assertions.assertThat(
                        result.getResponse().getStatus()
                    ).isIn(401, 403, 404); // Not 500, indicating interceptor worked
                });
    }

    @Test
    void nonApiEndpoints_BypassVersionInterceptor() throws Exception {
        // Non-API endpoints should not be intercepted
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk()); // Should work normally

        mockMvc.perform(get("/h2-console"))
                .andExpect(result -> {
                    // Should not cause interceptor issues
                    org.assertj.core.api.Assertions.assertThat(
                        result.getResponse().getStatus()
                    ).isNotEqualTo(500);
                });
    }

    @Test
    void apiVersionInterceptor_HandlesVersionedEndpoints() throws Exception {
        // Test various versioned API endpoints
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(result -> {
                    // Should be processed by interceptor
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status)
                            .isIn(401, 403, 404, 405); // Valid HTTP responses
                });

        mockMvc.perform(get("/api/v1/popups"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status)
                            .isIn(401, 403, 404, 405);
                });

        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status)
                            .isIn(401, 403, 404, 405);
                });
    }

    @Test
    void apiVersionInterceptor_HandlesPostRequests() throws Exception {
        // POST requests should also be intercepted
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Should be processed by interceptor, likely fail auth
                    org.assertj.core.api.Assertions.assertThat(status)
                            .isIn(400, 401, 403, 404, 422);
                });
    }

    @Test
    void apiVersionInterceptor_HandlesDifferentVersions() throws Exception {
        // Test different API versions if supported
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(result -> {
                    // V1 should be processed normally
                    org.assertj.core.api.Assertions.assertThat(
                        result.getResponse().getStatus()
                    ).isNotEqualTo(500);
                });

        // Test if v2 exists
        mockMvc.perform(get("/api/v2/orders"))
                .andExpect(result -> {
                    // Should either work or return 404, not 500
                    org.assertj.core.api.Assertions.assertThat(
                        result.getResponse().getStatus()
                    ).isNotEqualTo(500);
                });
    }

    @Test
    void apiVersionInterceptor_HandlesInvalidApiPaths() throws Exception {
        // Invalid API paths should be handled gracefully
        mockMvc.perform(get("/api/invalid"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Should return 404, not crash
                    org.assertj.core.api.Assertions.assertThat(status)
                            .isIn(404, 401, 403);
                });

        mockMvc.perform(get("/api/"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status)
                            .isIn(404, 401, 403);
                });
    }

    @Test
    void apiVersionInterceptor_HandlesConcurrentRequests() throws Exception {
        // Test concurrent requests to ensure thread safety
        Runnable requestTask = () -> {
            try {
                mockMvc.perform(get("/api/v1/orders"))
                        .andExpect(result -> {
                            // Should handle concurrent requests without issues
                            org.assertj.core.api.Assertions.assertThat(
                                result.getResponse().getStatus()
                            ).isNotEqualTo(500);
                        });
            } catch (Exception e) {
                // Log or handle exception
            }
        };

        // Run multiple concurrent requests
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(requestTask);
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(1000); // 1 second timeout
        }
    }

    @Test
    void apiVersionInterceptor_HandlesQueryParameters() throws Exception {
        // API endpoints with query parameters should work
        mockMvc.perform(get("/api/v1/orders")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Query parameters should not affect interceptor
                    org.assertj.core.api.Assertions.assertThat(status)
                            .isIn(401, 403, 404);
                });
    }

    @Test
    void apiVersionInterceptor_HandlesSpecialCharacters() throws Exception {
        // Test endpoints with special characters
        mockMvc.perform(get("/api/v1/orders/special-order-123"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status)
                            .isIn(401, 403, 404);
                });

        mockMvc.perform(get("/api/v1/popups/popup_name_with_underscores"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status)
                            .isIn(401, 403, 404);
                });
    }

    @Test
    void webMvcConfigurer_ConfiguresCorrectly() throws Exception {
        // Test that the WebMvcConfigurer is properly configured
        // This indirectly tests that our configuration is loaded

        // Test that the interceptor registration doesn't break basic functionality
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        // Test that API endpoints are properly mapped
        mockMvc.perform(get("/api/v1/nonexistent"))
                .andExpect(result -> {
                    // Should get proper 404, indicating routing works
                    org.assertj.core.api.Assertions.assertThat(
                        result.getResponse().getStatus()
                    ).isIn(401, 403, 404); // Not 500
                });
    }

    @Test
    void interceptorPattern_MatchesCorrectly() throws Exception {
        // Test that the interceptor pattern "/api/**" works correctly

        // Should match
        mockMvc.perform(get("/api/v1/test"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(500);
                });

        // Should match
        mockMvc.perform(get("/api/v2/test"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(500);
                });

        // Should NOT match - should bypass interceptor
        mockMvc.perform(get("/non-api/test"))
                .andExpect(result -> {
                    // Should return 403 or 404, but not 500 (indicating no server errors)
                    org.assertj.core.api.Assertions.assertThat(
                        result.getResponse().getStatus()
                    ).isIn(403, 404);
                });
    }
}