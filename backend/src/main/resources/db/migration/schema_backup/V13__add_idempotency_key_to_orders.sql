-- p_orders 테이블에 idempotency_key 컬럼 추가
-- 멱등성 보장을 위한 중복 주문 방지 목적

-- idempotency_key 컬럼 추가
ALTER TABLE p_orders
ADD COLUMN idempotency_key VARCHAR(128);

-- 유니크 인덱스 추가 (null 값 허용)
-- 멱등성 키가 있는 경우에만 중복 방지 제약 적용
CREATE UNIQUE INDEX idx_p_orders_idempotency_key
ON p_orders (idempotency_key)
WHERE idempotency_key IS NOT NULL;

-- 인덱스 추가 (조회 성능 향상)
CREATE INDEX idx_p_orders_idempotency_key_lookup
ON p_orders (idempotency_key);

-- 컬럼 및 인덱스 코멘트 추가
COMMENT ON COLUMN p_orders.idempotency_key IS '멱등성 키: API 호출 시 동일한 키로 중복 주문 방지';
COMMENT ON INDEX idx_p_orders_idempotency_key IS '멱등성 키 유니크 제약 (NULL 값 제외)';
COMMENT ON INDEX idx_p_orders_idempotency_key_lookup IS '멱등성 키 조회 성능 향상용 인덱스';