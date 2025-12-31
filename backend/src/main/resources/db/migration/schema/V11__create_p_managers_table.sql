CREATE TABLE p_managers_store (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id BIGINT NOT NULL REFERENCES p_users(id),
    store_id UUID NOT NULL REFERENCES p_stores(id),
    order_id UUID,
    role VARCHAR(10) NOT NULL,
    is_cancel BOOLEAN,
    pending_list VARCHAR(255),
    is_user_stop BOOLEAN NOT NULL DEFAULT FALSE,
    is_owner_stop BOOLEAN NOT NULL DEFAULT FALSE,
    is_reject BOOLEAN NOT NULL DEFAULT FALSE,
    is_force_stop BOOLEAN NOT NULL DEFAULT FALSE
);
