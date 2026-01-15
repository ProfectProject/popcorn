-- 결제 취소 실패 큐 테이블
CREATE TABLE p_payment_cancel_failure_queue (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    payment_key VARCHAR(255) NOT NULL,
    cancel_reason TEXT NOT NULL,
    failure_reason TEXT,
    amount INT NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, RETRYING, SUCCESS, FAILED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    completed_at TIMESTAMP
);

-- 인덱스
CREATE INDEX idx_payment_cancel_failure_queue_status_next_retry ON p_payment_cancel_failure_queue (status, next_retry_at);
CREATE INDEX idx_payment_cancel_failure_queue_order_id ON p_payment_cancel_failure_queue (order_id);
CREATE INDEX idx_payment_cancel_failure_queue_payment_id ON p_payment_cancel_failure_queue (payment_id);