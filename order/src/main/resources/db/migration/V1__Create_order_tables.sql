-- Order Entity 구조에 맞춘 주문 관련 테이블 생성 (V1 - 완전 재작성)
-- Hibernate Entity와 100% 호환되는 구조

CREATE SCHEMA IF NOT EXISTS orders;
SET search_path TO orders;

-- 1. OrderStatus enum 타입 생성 (Hibernate가 찾는 정확한 이름)
CREATE TYPE orderstatus AS ENUM (
    'REQUESTED',        -- 주문 요청됨
    'ACCEPTED',         -- 주문 수락됨
    'REJECTED',         -- 주문 거절됨
    'RESERVED',         -- 예약 확정됨
    'PAYMENT_PENDING',  -- 결제 대기
    'PAID',             -- 결제 완료됨
    'COMPLETED',        -- 완료
    'CANCELLED'         -- 취소됨
);

-- 2. OrderType enum 타입 생성
CREATE TYPE ordertype AS ENUM (
    'RESERVATION',      -- 예약형
    'GOODS',           -- 굿즈형
    'MIXED'            -- 혼합형 (예약 + 굿즈)
);

-- 3. p_orders 테이블 생성 (Order Entity와 완전 일치)
CREATE TABLE p_orders (
    order_id UUID PRIMARY KEY,                     -- Order.id (@Id)
    order_no VARCHAR(32) UNIQUE NOT NULL,          -- Order.orderNo
    user_id BIGINT NOT NULL,                       -- Order.customerId
    popup_id UUID,                                 -- Order.popupId (이제 저장됨)
    order_type ordertype NOT NULL,                 -- Order.orderType (이제 저장됨)
    status orderstatus NOT NULL DEFAULT 'REQUESTED', -- Order.status (@JdbcTypeCode)
    cancelable_until TIMESTAMP,                    -- Order.cancelableUntil
    total_price INTEGER NOT NULL,                  -- Order.totalAmount
    paid_at TIMESTAMP,                             -- Order.paidAt
    confirmed_at TIMESTAMP,                        -- Order.confirmedAt
    canceled_at TIMESTAMP,                         -- Order.canceledAt
    cancel_reason VARCHAR(500),                    -- Order.cancelReason

    -- BaseEntity 필드들
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_by BIGINT
);

-- 4. p_order_status_histories 테이블 생성 (OrderStatusHistory Entity와 완전 일치)
CREATE TABLE p_order_status_histories (
    order_status_id UUID PRIMARY KEY,              -- OrderStatusHistory.id (@Id)
    order_id UUID NOT NULL,                        -- OrderStatusHistory.orderId
    from_status orderstatus,                       -- OrderStatusHistory.fromStatus (@JdbcTypeCode)
    to_status orderstatus NOT NULL,                -- OrderStatusHistory.toStatus (@JdbcTypeCode)
    reason VARCHAR(255),                           -- OrderStatusHistory.reason
    changed_at TIMESTAMP NOT NULL,                 -- OrderStatusHistory.changedAt

    -- BaseEntity 필드들
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_by BIGINT
);

-- 5. 외래 키 제약 조건
ALTER TABLE p_order_status_histories
    ADD CONSTRAINT fk_p_order_status_histories_order
        FOREIGN KEY (order_id) REFERENCES p_orders(order_id);

-- 6. 인덱스 생성 (성능 최적화 + Repository 쿼리 지원)
CREATE INDEX idx_p_orders_user_id ON p_orders(user_id);
CREATE INDEX idx_p_orders_popup_id ON p_orders(popup_id);
CREATE INDEX idx_p_orders_status ON p_orders(status);
CREATE INDEX idx_p_orders_order_type ON p_orders(order_type);
CREATE INDEX idx_p_orders_created_at ON p_orders(created_at DESC);
CREATE INDEX idx_p_orders_order_no ON p_orders(order_no);
CREATE INDEX idx_p_orders_paid_at ON p_orders(paid_at);
CREATE INDEX idx_p_orders_cancelable_until ON p_orders(cancelable_until);

-- 상태 이력 테이블 인덱스
CREATE INDEX idx_p_order_status_histories_order_id ON p_order_status_histories(order_id);
CREATE INDEX idx_p_order_status_histories_changed_at ON p_order_status_histories(changed_at DESC);

-- 7. 체크 제약 조건
ALTER TABLE p_orders
    ADD CONSTRAINT chk_p_orders_total_price
        CHECK (total_price > 0);

-- 8. 테이블 및 컬럼 코멘트
COMMENT ON TABLE p_orders IS '주문 정보 테이블 (Order Entity와 완전 일치)';
COMMENT ON COLUMN p_orders.order_id IS '주문 고유 ID (UUID) - Order.id';
COMMENT ON COLUMN p_orders.order_no IS '주문 번호 (사용자 표시용) - Order.orderNo';
COMMENT ON COLUMN p_orders.user_id IS '주문한 사용자 ID - Order.customerId';
COMMENT ON COLUMN p_orders.popup_id IS '팝업 ID - Order.popupId';
COMMENT ON COLUMN p_orders.order_type IS '주문 타입 (RESERVATION/GOODS/MIXED) - Order.orderType';
COMMENT ON COLUMN p_orders.status IS '주문 상태 - Order.status';
COMMENT ON COLUMN p_orders.cancelable_until IS '취소 가능 시한 - Order.cancelableUntil';
COMMENT ON COLUMN p_orders.total_price IS '총 주문 금액 (원) - Order.totalAmount';
COMMENT ON COLUMN p_orders.paid_at IS '결제 완료 시간 - Order.paidAt';
COMMENT ON COLUMN p_orders.confirmed_at IS '주문 확정 시간 - Order.confirmedAt';
COMMENT ON COLUMN p_orders.canceled_at IS '주문 취소 시간 - Order.canceledAt';
COMMENT ON COLUMN p_orders.cancel_reason IS '취소 사유 - Order.cancelReason';

COMMENT ON TABLE p_order_status_histories IS '주문 상태 변경 이력 테이블 (OrderStatusHistory Entity와 완전 일치)';
COMMENT ON COLUMN p_order_status_histories.order_status_id IS '상태 변경 이력 고유 ID - OrderStatusHistory.id';
COMMENT ON COLUMN p_order_status_histories.order_id IS '주문 ID - OrderStatusHistory.orderId';
COMMENT ON COLUMN p_order_status_histories.from_status IS '변경 전 주문 상태 - OrderStatusHistory.fromStatus';
COMMENT ON COLUMN p_order_status_histories.to_status IS '변경 후 주문 상태 - OrderStatusHistory.toStatus';
COMMENT ON COLUMN p_order_status_histories.reason IS '상태 변경 사유 - OrderStatusHistory.reason';
COMMENT ON COLUMN p_order_status_histories.changed_at IS '상태 변경 일시 - OrderStatusHistory.changedAt';
