CREATE TYPE qr_status AS ENUM ( 'UNUSED','USED' );

CREATE TABLE p_qr_codes_checkin (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES p_orders(id),
    qr_code VARCHAR(255) NOT NULL UNIQUE,
    status qr_status NOT NULL
);
