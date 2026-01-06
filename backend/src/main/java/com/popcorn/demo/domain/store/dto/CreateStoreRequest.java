package com.popcorn.demo.domain.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStoreRequest {

    @NotBlank(message = "스토어 이름은 필수입니다.")
    @Size(min = 1, max = 100, message = "스토어 이름은 1-100자 사이여야 합니다.")
    private String name;

    @NotNull(message = "오너 ID는 필수입니다.")
    @Positive(message = "오너 ID는 양수여야 합니다.")
    private Long ownerId;

}