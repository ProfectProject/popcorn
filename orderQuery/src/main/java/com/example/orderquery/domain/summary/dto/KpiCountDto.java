package com.example.orderquery.domain.summary.dto;


import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class KpiCountDto {
    private final int total;
    private final int paid;
    private final int cancelled;
}
