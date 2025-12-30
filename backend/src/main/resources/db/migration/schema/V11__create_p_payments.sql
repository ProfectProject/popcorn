CREATE TABLE p_payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount INT NOT NULL,
    raw_payload TEXT,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP,
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(50),
    CONSTRAINT fk_p_payments_order FOREIGN KEY (order_id) REFERENCES p_orders(id)
);

CREATE UNIQUE INDEX idx_p_payments_order_id ON p_payments (order_id);
CREATE INDEX idx_p_payments_status_created_at ON p_payments (status, created_at);
