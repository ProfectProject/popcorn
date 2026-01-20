package com.popcorn.demo.domain.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import com.popcorn.common.cache.IdempotencyService;
import com.popcorn.common.dto.BaseResponse;

class PaymentIdempotencyControllerTest {

    @Mock
    private IdempotencyService idempotencyService;

    private PaymentIdempotencyController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new PaymentIdempotencyController(idempotencyService);
    }

    @Test
    void clearByPrefix_withValidPrefix_returnsOk() {
        String prefix = "payment:order:";

        ResponseEntity<BaseResponse<String>> response = controller.clearByPrefix(prefix);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).contains(prefix);
        verify(idempotencyService).clearByPrefix(prefix);
    }

    @Test
    void clearByPrefix_withBlankPrefix_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> controller.clearByPrefix(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("올바른 멱등성 키 접두사");
    }

    @Test
    void clearByPrefix_whenServiceThrows_wrapsRuntimeException() {
        String prefix = "payment:fail:";
        doThrow(new IllegalStateException("boom")).when(idempotencyService).clearByPrefix(prefix);

        assertThatThrownBy(() -> controller.clearByPrefix(prefix))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("접두사 캐시 삭제 중 오류가 발생했습니다");
    }
}
