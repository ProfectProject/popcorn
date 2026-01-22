package com.example.orderquery.domain.summary.entity;

import java.time.LocalDateTime;
import java.util.UUID;



import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "summary_applied_log",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_summary_applied_log_event", columnNames = {"event_id", "event_type"})
        },
        indexes = {
                @Index(name = "idx_summary_applied_log_popup", columnList = "popup_id"),
                @Index(name = "idx_summary_applied_log_popup_time", columnList = "popup_id, applied_at DESC"),
                @Index(name = "idx_summary_applied_log_order_time", columnList = "order_id, applied_at DESC")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SummaryAppliedLog {

    @Id
    @Column(name = "applied_id", nullable = false)
    private UUID appliedId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(name = "popup_id", nullable = false)
    private UUID popupId;

    @Column(name = "order_id")
    private UUID orderId;

    // 예약 delta
    @Column(name = "delta_reservation_total", nullable = false)
    private int deltaReservationTotal;

    @Column(name = "delta_reservation_paid", nullable = false)
    private int deltaReservationPaid;

    @Column(name = "delta_reservation_cancelled", nullable = false)
    private int deltaReservationCancelled;

    // 굿즈 delta
    @Column(name = "delta_goods_total", nullable = false)
    private int deltaGoodsTotal;

    @Column(name = "delta_goods_paid", nullable = false)
    private int deltaGoodsPaid;

    @Column(name = "delta_goods_cancelled", nullable = false)
    private int deltaGoodsCancelled;

    // 체크인 delta
    @Column(name = "delta_checked_in", nullable = false)
    private int deltaCheckedIn;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static SummaryAppliedLog of(UUID eventId,
                                       EventType eventType,
                                       UUID popupId,
                                       UUID orderId,
                                       int dResTotal, int dResPaid, int dResCancelled,
                                       int dGoodsTotal, int dGoodsPaid, int dGoodsCancelled,
                                       int dCheckedIn,
                                       LocalDateTime appliedAt) {

        return SummaryAppliedLog.builder()
                .appliedId(UUID.randomUUID())
                .eventId(eventId)
                .eventType(eventType)
                .popupId(popupId)
                .orderId(orderId)
                .deltaReservationTotal(dResTotal)
                .deltaReservationPaid(dResPaid)
                .deltaReservationCancelled(dResCancelled)
                .deltaGoodsTotal(dGoodsTotal)
                .deltaGoodsPaid(dGoodsPaid)
                .deltaGoodsCancelled(dGoodsCancelled)
                .deltaCheckedIn(dCheckedIn)
                .appliedAt(appliedAt != null ? appliedAt : LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
