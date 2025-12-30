CREATE TABLE p_order_status_histories (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    changed_by BIGINT,
    reason VARCHAR(255),
    changed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP,
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(50),
    CONSTRAINT fk_p_order_status_histories_order FOREIGN KEY (order_id) REFERENCES p_orders(id),
    CONSTRAINT fk_p_order_status_histories_changed_by FOREIGN KEY (changed_by) REFERENCES p_user(id)
);
