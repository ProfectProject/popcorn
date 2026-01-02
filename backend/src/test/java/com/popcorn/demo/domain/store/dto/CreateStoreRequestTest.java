package com.popcorn.demo.domain.store.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class CreateStoreRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("유효한 스토어 생성 요청")
    void 유효한_스토어_생성_요청() {
        // Given
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("유효한 스토어 이름")
                .ownerId(123L)
                .build();

        // When
        Set<ConstraintViolation<CreateStoreRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("스토어 이름이 빈 문자열인 경우 검증 실패")
    void 스토어_이름이_빈_문자열인_경우_검증_실패() {
        // Given
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("")
                .ownerId(123L)
                .build();

        // When
        Set<ConstraintViolation<CreateStoreRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("오너 ID가 null인 경우 검증 실패")
    void 오너_ID가_null인_경우_검증_실패() {
        // Given
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("테스트 스토어")
                .ownerId(null)
                .build();

        // When
        Set<ConstraintViolation<CreateStoreRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("오너 ID가 음수인 경우 검증 실패")
    void 오너_ID가_음수인_경우_검증_실패() {
        // Given
        CreateStoreRequest request = CreateStoreRequest.builder()
                .name("테스트 스토어")
                .ownerId(-1L)
                .build();

        // When
        Set<ConstraintViolation<CreateStoreRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isNotEmpty();
    }
}