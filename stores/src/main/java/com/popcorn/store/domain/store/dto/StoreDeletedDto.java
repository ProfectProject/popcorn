package com.popcorn.store.domain.store.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreDeletedDto {

    private UUID id;
    private String name;
    private LocalDateTime deletedAt;
    private Long deletedBy;

}