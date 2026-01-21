-- payment_key 컬럼 추가 (멱등성 키)
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS payment_key VARCHAR(200);

-- payment_key 유니크 인덱스
CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_payment_key
    ON payments (payment_key);

COMMENT ON COLUMN payments.payment_key IS '토스페이먼츠 결제 키 (멱등성 키)';
