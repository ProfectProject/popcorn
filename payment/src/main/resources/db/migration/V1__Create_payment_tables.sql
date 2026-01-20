-- 결제 테이블 생성
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    amount INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'READY',
    approved_at TIMESTAMP,
    raw_payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX idx_payment_order_id ON payments(order_id);
CREATE INDEX idx_payment_status ON payments(status);
CREATE INDEX idx_payment_approved_at ON payments(approved_at);
CREATE INDEX idx_payment_created_at ON payments(created_at);

-- 결제 실패 재시도 큐 테이블 (선택적)
CREATE TABLE payment_cancel_failure_queue (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    order_id UUID NOT NULL,
    order_no VARCHAR(100) NOT NULL,
    cancel_reason TEXT NOT NULL,
    failure_reason TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

-- 재시도 큐 인덱스
CREATE INDEX idx_cancel_failure_queue_next_retry ON payment_cancel_failure_queue(next_retry_at);
CREATE INDEX idx_cancel_failure_queue_payment_id ON payment_cancel_failure_queue(payment_id);

-- 결제 상태 체크 제약 조건
ALTER TABLE payments ADD CONSTRAINT chk_payment_status
    CHECK (status IN ('READY', 'PAID', 'CANCELLED', 'FAILED'));

-- 결제 수단 체크 제약 조건
ALTER TABLE payments ADD CONSTRAINT chk_payment_method
    CHECK (payment_method IN ('CARD', 'TRANSFER', 'VIRTUAL_ACCOUNT', 'MOBILE_PHONE', 'GIFT_CERTIFICATE'));

-- 결제 금액 체크 제약 조건
ALTER TABLE payments ADD CONSTRAINT chk_payment_amount
    CHECK (amount > 0);

-- 코멘트 추가
COMMENT ON TABLE payments IS '결제 정보 테이블';
COMMENT ON COLUMN payments.id IS '결제 고유 ID';
COMMENT ON COLUMN payments.order_id IS '주문 ID';
COMMENT ON COLUMN payments.payment_method IS '결제 수단';
COMMENT ON COLUMN payments.amount IS '결제 금액';
COMMENT ON COLUMN payments.status IS '결제 상태';
COMMENT ON COLUMN payments.approved_at IS '결제 승인 일시';
COMMENT ON COLUMN payments.raw_payload IS '토스페이먼츠 원본 응답 데이터';
COMMENT ON COLUMN payments.created_at IS '생성 일시';
COMMENT ON COLUMN payments.updated_at IS '수정 일시';
COMMENT ON COLUMN payments.deleted_at IS '논리 삭제 일시';

COMMENT ON TABLE payment_cancel_failure_queue IS '결제 취소 실패 재시도 큐';
COMMENT ON COLUMN payment_cancel_failure_queue.retry_count IS '재시도 횟수';
COMMENT ON COLUMN payment_cancel_failure_queue.next_retry_at IS '다음 재시도 일시';