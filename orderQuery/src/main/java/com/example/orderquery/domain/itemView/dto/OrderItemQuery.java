package com.example.orderquery.domain.itemView.dto;

import java.time.LocalDateTime;


import com.example.orderquery.domain.itemView.entity.ItemType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class OrderItemQuery {

    // 필터
    private String orderStatus;   // e.g. PAID, CANCELLED ... (스냅샷 문자열)
    private String paymentStatus; // e.g. PAID, FAILED ...
    private ItemType itemType;    // SCHEDULE / GOODS
    private Boolean checkedIn;    // 예약 필터링에 사용 (null이면 무시)

    // 기간
    private LocalDateTime from;   // ordered_at >= from
    private LocalDateTime to;     // ordered_at < to

    // 페이징
    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(200)
    private int size = 20;
}