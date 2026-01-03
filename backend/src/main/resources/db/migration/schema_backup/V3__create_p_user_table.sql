CREATE TABLE p_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    role user_role NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP,
    updated_by BIGINT,
    deleted_at TIMESTAMP,
    deleted_by BIGINT
);

CREATE TABLE p_user_addresses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id BIGINT NOT NULL REFERENCES p_users(id),
    name VARCHAR(50) NOT NULL,
    address1 VARCHAR(255) NOT NULL,
    address2 VARCHAR(255),
    postal_code VARCHAR(10),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT REFERENCES p_users(id),
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT REFERENCES p_users(id),
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES p_users(id)
);