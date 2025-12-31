ALTER TABLE p_orders
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN p_orders.version IS '낙관적 락 버전';
