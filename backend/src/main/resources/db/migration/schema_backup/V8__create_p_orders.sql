CREATE TABLE p_orders (
    id BIGSERIAL PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    order_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    cancelable_until TIMESTAMP,
    total_amount INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP,
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(50),
    CONSTRAINT fk_p_orders_customer FOREIGN KEY (customer_id) REFERENCES p_user(id),
    CONSTRAINT fk_p_orders_store FOREIGN KEY (store_id) REFERENCES p_stores(id),
    CONSTRAINT fk_p_orders_product FOREIGN KEY (product_id) REFERENCES p_products(id)
);

CREATE INDEX idx_p_orders_customer_created_at ON p_orders (customer_id, created_at);
CREATE INDEX idx_p_orders_store_created_at ON p_orders (store_id, created_at);
CREATE INDEX idx_p_orders_status ON p_orders (status);
