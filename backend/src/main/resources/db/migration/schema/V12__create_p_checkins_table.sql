-- CHECK-IN
CREATE TYPE qr_status AS ENUM ( 'UNUSED','USED' );

CREATE TABLE p_order_qr_codes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES p_orders(id),
    qr_code VARCHAR(255) NOT NULL UNIQUE,
    status qr_status NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES p_users(id)
);
