package com.popcorn.demo.domain.merch.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MerchStatusResponse {
    private UUID id;
    private Boolean isHidden;
}
