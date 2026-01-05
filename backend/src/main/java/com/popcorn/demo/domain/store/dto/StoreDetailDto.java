package com.popcorn.demo.domain.store.dto;

import com.popcorn.demo.domain.store.entity.StorePublishStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreDetailDto {

    private UUID id;
    private String name;
    private Long ownerId;
    private String ownerName;
    private StorePublishStatus publishStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
