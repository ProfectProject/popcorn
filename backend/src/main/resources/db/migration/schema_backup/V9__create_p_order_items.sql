CREATE TABLE p_order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_item_type VARCHAR(20) NOT NULL,
    session_option_id BIGINT,
    merch_variant_id BIGINT,
    qty INT NOT NULL,
    unit_price INT NOT NULL,
    line_amount INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP,
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(50),
    CONSTRAINT fk_p_order_items_order FOREIGN KEY (order_id) REFERENCES p_orders(id),
    CONSTRAINT fk_p_order_items_session_option FOREIGN KEY (session_option_id) REFERENCES p_session_options(id),
    CONSTRAINT fk_p_order_items_merch_variant FOREIGN KEY (merch_variant_id) REFERENCES p_merch_variants(id)
);
