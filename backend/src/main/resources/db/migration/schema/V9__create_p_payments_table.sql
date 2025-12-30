CREATE TABLE p_payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES p_orders(id),
    method payment_method NOT NULL,
    status payment_status NOT NULL,
    amount INT NOT NULL,
    raw_payload TEXT,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES p_users(id),
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT REFERENCES p_users(id),
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES p_users(id)
);

-- indexes
CREATE UNIQUE INDEX idx_payment_order_id ON p_payments(order_id);
CREATE INDEX idx_payment_status_created ON p_payments(status, created_at);
