CREATE TABLE p_order_qr_codes (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    qr_code VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP,
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(50),
    CONSTRAINT fk_p_order_qr_codes_order FOREIGN KEY (order_id) REFERENCES p_orders(id)
);

CREATE INDEX idx_p_order_qr_codes_expires_at ON p_order_qr_codes (expires_at);
CREATE INDEX idx_p_order_qr_codes_is_active ON p_order_qr_codes (is_active);
