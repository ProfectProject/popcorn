package com.popcorn.demo.domain.order.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.popcorn.demo.common.entity.BaseEntity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table("p_order_status_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory extends BaseEntity {

	// 상태 전이를 1건씩 기록해 감사 추적에 활용합니다.
	@Id
	private UUID id;

	@Column("order_id")
	private UUID orderId;

	@Column("from_status")
	private OrderStatus fromStatus;

	@Column("to_status")
	private OrderStatus toStatus;

	@Column("changed_by")
	private Long changedBy;

	@Column("reason")
	private String reason;

	@Column("changed_at")
	private LocalDateTime changedAt;
}
