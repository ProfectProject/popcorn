CREATE TABLE p_merch_variants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL REFERENCES p_products(id),
    sku VARCHAR(64),
    name VARCHAR(100) NOT NULL,
    price INT NOT NULL,
    stock INT NOT NULL,
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES p_users(id),
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT REFERENCES p_users(id),
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES p_users(id)
);