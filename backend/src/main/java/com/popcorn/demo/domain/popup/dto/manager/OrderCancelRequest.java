package com.popcorn.demo.domain.popup.dto.manager;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCancelRequest {

    private UUID popupId;
    private Long managerId;
    private String reason;
}
