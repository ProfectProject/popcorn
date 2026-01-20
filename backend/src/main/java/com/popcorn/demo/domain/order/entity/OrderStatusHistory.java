package com.popcorn.demo.domain.order.entity;

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

@Entity
@Table(name = "order_status_histories", schema = "\"order\"")
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
	@Column(name = "order_status_id")
	private UUID id;

	@Column(name = "order_id")
	private UUID orderId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "from_status")
	private OrderStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "to_status")
	private OrderStatus toStatus;

	@Transient
	private Long changedBy;

	@Column(name = "reason")
	private String reason;

	@Column(name = "changed_at")
	private LocalDateTime changedAt;
}
