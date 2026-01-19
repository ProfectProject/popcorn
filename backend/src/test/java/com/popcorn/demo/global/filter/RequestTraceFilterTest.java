package com.popcorn.demo.global.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class RequestTraceFilterTest {

    private RequestTraceFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RequestTraceFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void doFilterInternal_GeneratesTraceId() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(request.getMethod()).thenReturn("GET");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        String traceId = MDC.get("traceId");
        assertThat(traceId).isNotNull();
        assertThat(traceId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithExistingTraceId_PreservesTraceId() throws ServletException, IOException {
        // given
        String existingTraceId = "existing-trace-123";
        MDC.put("traceId", existingTraceId);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(MDC.get("traceId")).isEqualTo(existingTraceId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_KeepsTraceIdAfterRequest() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/orders");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(MDC.get("traceId")).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithFilterChainException_KeepsTraceId() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        RuntimeException exception = new RuntimeException("Filter chain error");

        // Mock filterChain to throw exception
        org.mockito.Mockito.doThrow(exception).when(filterChain).doFilter(request, response);

        // when & then
        try {
            filter.doFilterInternal(request, response, filterChain);
        } catch (RuntimeException e) {
            // Expected exception - verify traceId is still available for error logging
            assertThat(MDC.get("traceId")).isNotNull();
        }
    }

    @Test
    void doFilterInternal_WithMultipleRequests_GeneratesUniqueTraceIds() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/orders");

        // when
        filter.doFilterInternal(request, response, filterChain);
        String firstTraceId = MDC.get("traceId");

        MDC.clear(); // Simulate new request

        filter.doFilterInternal(request, response, filterChain);
        String secondTraceId = MDC.get("traceId");

        // then
        assertThat(firstTraceId).isNotEqualTo(secondTraceId);
        verify(filterChain, org.mockito.Mockito.times(2)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithDifferentUriPaths_HandlesCorrectly() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/popups/123");
        when(request.getMethod()).thenReturn("POST");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        String traceId = MDC.get("traceId");
        assertThat(traceId).isNotNull();
        assertThat(traceId).hasSize(36); // UUID format length
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithHealthCheckEndpoint_GeneratesTraceId() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getMethod()).thenReturn("GET");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        String traceId = MDC.get("traceId");
        assertThat(traceId).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ConcurrentRequests_GeneratesUniqueTraceIds() throws ServletException, IOException {
        // Simulate concurrent execution
        Thread[] threads = new Thread[10];
        String[] traceIds = new String[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    HttpServletRequest mockReq = mock(HttpServletRequest.class);
                    HttpServletResponse mockResp = mock(HttpServletResponse.class);
                    FilterChain mockChain = mock(FilterChain.class);

                    when(mockReq.getRequestURI()).thenReturn("/api/v1/orders/" + index);

                    filter.doFilterInternal(mockReq, mockResp, mockChain);
                    traceIds[index] = MDC.get("traceId");

                } catch (Exception e) {
                    // Handle exception
                } finally {
                    MDC.clear();
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Verify all trace IDs are unique
        for (int i = 0; i < 10; i++) {
            assertThat(traceIds[i]).isNotNull();
            for (int j = i + 1; j < 10; j++) {
                assertThat(traceIds[i]).isNotEqualTo(traceIds[j]);
            }
        }
    }

    @Test
    void doFilterInternal_WithNullRequestUri_HandlesGracefully() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        String traceId = MDC.get("traceId");
        assertThat(traceId).isNotNull();
        verify(filterChain).doFilter(request, response);
    }
}