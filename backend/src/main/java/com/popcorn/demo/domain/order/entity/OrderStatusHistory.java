package com.popcorn.demo.domain.order.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.popcorn.demo.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "p_order_status_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory extends BaseEntity {

	// 상태 전이를 1건씩 기록해 감사 추적에 활용합니다.
	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@Column(name = "order_id")
	private UUID orderId;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status")
	private OrderStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status")
	private OrderStatus toStatus;

	@Column(name = "changed_by")
	private Long changedBy;

	@Column(name = "reason")
	private String reason;

	@Column(name = "changed_at")
	private LocalDateTime changedAt;
}
