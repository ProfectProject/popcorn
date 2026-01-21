package com.popcorn.store.domain.store.dto;

import com.popcorn.store.domain.store.entity.StorePublishStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreListDto {

    private UUID id;
    private String name;
    private StorePublishStatus publishStatus;
    private LocalDateTime createdAt;

}
