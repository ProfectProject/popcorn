package com.popcorn.order.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.popcorn.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 주문 상태 변경 이력 엔티티
 * p_order_status_histories 테이블과 매핑
 *
 * [초보자 가이드]
 * 이 클래스는 주문 상태가 언제, 왜 바뀌었는지를 기록합니다.
 * 예: "주문 요청됨" → "결제 대기" 로 상태 변경시 그 이력을 저장
 * 감사(Audit) 목적으로 모든 상태 전이를 1건씩 기록합니다.
 */
@Entity
@Table(name = "p_order_status_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory extends BaseEntity {

    /** 상태 이력 ID (Primary Key) */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "order_status_id")
    private UUID id;

    /** 주문 ID - 상태가 변경된 주문의 ID */
    @Column(name = "order_id")
    private UUID orderId;

    /** 변경 전 상태 - 어떤 상태에서 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "from_status")
    private OrderStatus fromStatus;

    /** 변경 후 상태 - 어떤 상태로 바뀌었는지 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "to_status")
    private OrderStatus toStatus;

    /** 변경한 사용자 ID (메모리상에만 존재) */
    @Transient
    private Long changedBy;

    /** 변경 사유 - 왜 상태가 바뀌었는지 설명 */
    @Column(name = "reason")
    private String reason;

    /** 변경 일시 - 정확히 언제 바뀌었는지 */
    @Column(name = "changed_at")
    private LocalDateTime changedAt;

}