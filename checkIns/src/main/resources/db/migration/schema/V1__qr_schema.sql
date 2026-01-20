-- QR 관련 테이블 스키마
-- checkIns 모듈 전용 QR 도메인

-- QR 코드 테이블
CREATE TABLE IF NOT EXISTS qr_order_qr_codes (
    qr_id       UUID PRIMARY KEY,
    order_id    UUID NOT NULL,
    qr_code     VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    created_by  BIGINT
);

-- 체크인 테이블
CREATE TABLE IF NOT EXISTS qr_checkins (
    checkin_id       UUID PRIMARY KEY,
    order_id         UUID NOT NULL,
    order_qr_code_id UUID NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    created_by       BIGINT
);

-- Foreign Key 제약조건
DO $$ BEGIN
    ALTER TABLE qr_checkins
        ADD CONSTRAINT fk_checkins_qr
        FOREIGN KEY (order_qr_code_id) REFERENCES qr_order_qr_codes(qr_id);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_qr_codes_order_id ON qr_order_qr_codes(order_id);
CREATE INDEX IF NOT EXISTS idx_qr_codes_expires_at ON qr_order_qr_codes(expires_at);
CREATE INDEX IF NOT EXISTS idx_checkins_order_id ON qr_checkins(order_id);
CREATE INDEX IF NOT EXISTS idx_checkins_created_at ON qr_checkins(created_at);