package com.popcorn.demo.domain.payment.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "p_payment_cancel_failure_queue")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentCancelFailureQueue {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Column(name = "payment_id", nullable = false)
	private UUID paymentId;

	@Column(name = "payment_key", nullable = false)
	private String paymentKey;

	@Column(name = "cancel_reason", nullable = false)
	private String cancelReason;

	@Column(name = "failure_reason")
	private String failureReason;

	@Column(name = "amount", nullable = false)
	private Integer amount;

	@Builder.Default
	@Column(name = "attempt_count", nullable = false)
	private Integer attemptCount = 0;

	@Builder.Default
	@Column(name = "max_attempts", nullable = false)
	private Integer maxAttempts = 5;

	@Column(name = "next_retry_at")
	private LocalDateTime nextRetryAt;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private QueueStatus status = QueueStatus.PENDING;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Builder.Default
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	public enum QueueStatus {
		PENDING,    // 대기 중
		RETRYING,   // 재시도 중
		SUCCESS,    // 성공
		FAILED      // 최종 실패
	}

	public void incrementAttempt() {
		this.attemptCount++;

		if (this.attemptCount >= this.maxAttempts) {
			this.status = QueueStatus.FAILED;
			this.completedAt = LocalDateTime.now();
		} else {
			// 지수 백오프: 1분, 2분, 4분, 8분 간격으로 재시도
			int delayMinutes = (int) Math.pow(2, this.attemptCount - 1);
			this.nextRetryAt = LocalDateTime.now().plusMinutes(delayMinutes);
		}
	}

	public void markSuccess() {
		this.status = QueueStatus.SUCCESS;
		this.completedAt = LocalDateTime.now();
	}

	public void setRetrying() {
		this.status = QueueStatus.RETRYING;
	}

	public boolean canRetry() {
		return this.status == QueueStatus.PENDING
			&& this.attemptCount < this.maxAttempts
			&& (this.nextRetryAt == null || LocalDateTime.now().isAfter(this.nextRetryAt));
	}

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}