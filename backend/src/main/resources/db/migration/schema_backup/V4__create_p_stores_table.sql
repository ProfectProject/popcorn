CREATE TABLE p_stores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id BIGINT NOT NULL REFERENCES p_users(id),
    name VARCHAR(100) NOT NULL,
    publish_status store_publish_status NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES p_users(id),
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT REFERENCES p_users(id),
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES p_users(id)
);
