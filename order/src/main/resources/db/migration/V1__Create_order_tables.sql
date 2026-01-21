-- 주문 관련 테이블 생성
-- backend v1.sql의 order 스키마를 기반으로 작성

CREATE SCHEMA IF NOT EXISTS orders;
SET search_path TO orders;

-- 주문 상태 ENUM 타입 생성
CREATE TYPE order_status AS ENUM (
    'REQUESTED',        -- 주문 요청됨
    'ACCEPTED',         -- 주문 수락됨
    'REJECTED',         -- 주문 거절됨
    'RESERVED',         -- 예약 확정됨
    'PAYMENT_PENDING',  -- 결제 대기
    'PAID',             -- 결제 완료됨
    'COMPLETED',        -- 완료
    'CANCELLED'         -- 취소됨
);

-- 메인 주문 테이블 생성
CREATE TABLE orders (
    order_id UUID PRIMARY KEY,                  -- 주문 ID
    order_no VARCHAR(32) UNIQUE NOT NULL,       -- 주문 번호 (사용자용 식별자)
    user_id BIGINT NOT NULL,                    -- 주문한 사용자 ID
    store_id UUID NOT NULL,                     -- 팝업스토어 ID
    status order_status NOT NULL DEFAULT 'REQUESTED',  -- 주문 상태
    cancelable_until TIMESTAMP,                 -- 취소 가능 시한
    total_price INTEGER NOT NULL,               -- 총 주문 금액
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_by BIGINT
);

-- 주문 상품 테이블 (주문 항목)
CREATE TABLE order_goods (
    order_goods_id UUID PRIMARY KEY,            -- 주문 상품 ID
    order_id UUID NOT NULL,                     -- 주문 ID (FK)
    popup_id UUID NOT NULL,                     -- 팝업 ID
    item_type VARCHAR(10) NOT NULL,             -- 항목 타입 (RESERVATION/GOODS)
    schedule_id UUID,                           -- 스케줄 ID (예약형 상품용)
    goods_variant_id UUID,                      -- 굿즈 변형 ID (구매형 상품용)
    qty INTEGER NOT NULL,                       -- 수량
    unit_price INTEGER NOT NULL,                -- 단가
    price INTEGER NOT NULL,                     -- 라인 금액 (단가 × 수량)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_by BIGINT
);

-- 주문 상태 변경 이력 테이블
CREATE TABLE order_status_histories (
    order_status_id UUID PRIMARY KEY,           -- 상태 이력 ID
    order_id UUID NOT NULL,                     -- 주문 ID (FK)
    from_status order_status,                   -- 변경 전 상태
    to_status order_status NOT NULL,            -- 변경 후 상태
    reason VARCHAR(255),                        -- 변경 사유
    changed_at TIMESTAMP NOT NULL,              -- 변경 일시
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_by BIGINT
);

-- 외래 키 제약 조건
ALTER TABLE order_goods
    ADD CONSTRAINT fk_order_goods_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id);

ALTER TABLE order_status_histories
    ADD CONSTRAINT fk_order_status_histories_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id);

-- 인덱스 생성 (성능 최적화)
-- 주문 테이블 인덱스
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_store_id ON orders(store_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX idx_orders_order_no ON orders(order_no);

-- 주문 상품 테이블 인덱스
CREATE INDEX idx_order_goods_order_id ON order_goods(order_id);
CREATE INDEX idx_order_goods_popup_id ON order_goods(popup_id);
CREATE INDEX idx_order_goods_item_type ON order_goods(item_type);
CREATE INDEX idx_order_goods_schedule_id ON order_goods(schedule_id);
CREATE INDEX idx_order_goods_goods_variant_id ON order_goods(goods_variant_id);

-- 주문 상태 이력 테이블 인덱스
CREATE INDEX idx_order_status_histories_order_id ON order_status_histories(order_id);
CREATE INDEX idx_order_status_histories_changed_at ON order_status_histories(changed_at DESC);

-- 체크 제약 조건
ALTER TABLE order_goods ADD CONSTRAINT chk_order_goods_qty
    CHECK (qty > 0);

ALTER TABLE order_goods ADD CONSTRAINT chk_order_goods_unit_price
    CHECK (unit_price > 0);

ALTER TABLE order_goods ADD CONSTRAINT chk_order_goods_price
    CHECK (price > 0);

ALTER TABLE orders ADD CONSTRAINT chk_orders_total_price
    CHECK (total_price > 0);

-- 항목 타입 체크 제약 조건
ALTER TABLE order_goods ADD CONSTRAINT chk_order_goods_item_type
    CHECK (item_type IN ('RESERVATION', 'GOODS'));

-- 논리적 제약 조건 (애플리케이션에서 검증하지만 DB 레벨에서도 체크)
-- 예약형 상품은 schedule_id가 필수, goods_variant_id는 null
-- 구매형 상품은 goods_variant_id가 필수, schedule_id는 null
-- 이는 CHECK 제약으로는 복잡하므로 애플리케이션 레벨에서 검증

-- 테이블 및 컬럼 코멘트
COMMENT ON TABLE orders IS '주문 정보 테이블';
COMMENT ON COLUMN orders.order_id IS '주문 고유 ID';
COMMENT ON COLUMN orders.order_no IS '주문 번호 (사용자 표시용)';
COMMENT ON COLUMN orders.user_id IS '주문한 사용자 ID';
COMMENT ON COLUMN orders.store_id IS '팝업스토어 ID';
COMMENT ON COLUMN orders.status IS '주문 상태';
COMMENT ON COLUMN orders.cancelable_until IS '취소 가능 시한';
COMMENT ON COLUMN orders.total_price IS '총 주문 금액 (원)';

COMMENT ON TABLE order_goods IS '주문 상품 정보 테이블';
COMMENT ON COLUMN order_goods.order_goods_id IS '주문 상품 고유 ID';
COMMENT ON COLUMN order_goods.order_id IS '주문 ID';
COMMENT ON COLUMN order_goods.popup_id IS '팝업 ID';
COMMENT ON COLUMN order_goods.item_type IS '상품 타입 (RESERVATION/GOODS)';
COMMENT ON COLUMN order_goods.schedule_id IS '스케줄 ID (예약형 상품용)';
COMMENT ON COLUMN order_goods.goods_variant_id IS '굿즈 변형 ID (구매형 상품용)';
COMMENT ON COLUMN order_goods.qty IS '주문 수량';
COMMENT ON COLUMN order_goods.unit_price IS '단가 (원)';
COMMENT ON COLUMN order_goods.price IS '라인 금액 (원)';

COMMENT ON TABLE order_status_histories IS '주문 상태 변경 이력 테이블';
COMMENT ON COLUMN order_status_histories.order_status_id IS '상태 변경 이력 고유 ID';
COMMENT ON COLUMN order_status_histories.order_id IS '주문 ID';
COMMENT ON COLUMN order_status_histories.from_status IS '변경 전 주문 상태';
COMMENT ON COLUMN order_status_histories.to_status IS '변경 후 주문 상태';
COMMENT ON COLUMN order_status_histories.reason IS '상태 변경 사유';
COMMENT ON COLUMN order_status_histories.changed_at IS '상태 변경 일시';
