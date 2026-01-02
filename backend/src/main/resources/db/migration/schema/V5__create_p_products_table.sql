CREATE TABLE p_products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    store_id UUID NOT NULL REFERENCES p_stores(id),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category product_category NOT NULL,
    status product_status NOT NULL,
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES p_users(id),
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT REFERENCES p_users(id),
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES p_users(id)
);

CREATE TABLE p_product_locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL REFERENCES p_products(id),
    name VARCHAR(100),
    address1 VARCHAR(255),
    address2 VARCHAR(255),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES p_users(id),
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT REFERENCES p_users(id),
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES p_users(id)
);



