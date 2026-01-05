CREATE TABLE p_orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_no VARCHAR(32) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES p_users(id),
    store_id UUID NOT NULL REFERENCES p_stores(id),
    status order_status NOT NULL,
    cancelable_until TIMESTAMP,
    total_amount INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES p_users(id),
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT REFERENCES p_users(id),
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES p_users(id)
);

CREATE TABLE p_order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES p_orders(id),
    session_option_id UUID REFERENCES p_session_options(id),
    merch_variant_id UUID REFERENCES p_merch_variants(id),
    qty INT NOT NULL,
    unit_price INT NOT NULL,
    line_amount INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES p_users(id),
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT REFERENCES p_users(id),
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES p_users(id)
);

CREATE TABLE p_order_status_histories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES p_orders(id),
    from_status order_status,
    to_status order_status NOT NULL,
    changed_by BIGINT REFERENCES p_users(id),
    reason VARCHAR(255),
    changed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES p_users(id),
    updated_at TIMESTAMP,
    updated_by BIGINT REFERENCES p_users(id),
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES p_users(id)
);


-- indexes
CREATE INDEX idx_orders_customer_created ON p_orders(customer_id, created_at);
CREATE INDEX idx_orders_store_created ON p_orders(store_id, created_at);
CREATE INDEX idx_orders_status ON p_orders(status);
