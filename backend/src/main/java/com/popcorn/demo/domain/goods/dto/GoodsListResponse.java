package com.popcorn.demo.domain.merch.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MerchListResponse {
    private List<MerchItemResponse> items;
}
