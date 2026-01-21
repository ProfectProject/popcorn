package com.popcorn.store.domain.store.dto;

import com.popcorn.store.domain.store.entity.StorePublishStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStoreStatusRequest {

    @NotNull(message = "발행 상태는 필수입니다")
    private StorePublishStatus publishStatus;

}