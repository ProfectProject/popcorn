package com.popcorn.demo.domain.payment.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.popcorn.demo.common.entity.BaseEntity;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "p_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	@Column(name = "payment_id")
	private UUID id;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "method", nullable = false)
	private PaymentMethod method;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private PaymentStatus status;

	@Column(name = "amount", nullable = false)
	private Integer amount;

	@Column(name = "raw_payload")
	private String rawPayload;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Column(name = "deleted_by")
	private Long deletedBy;
}
