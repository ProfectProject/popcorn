CREATE TABLE IF NOT EXISTS p_session_options (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    price INT NOT NULL,
    capacity INT NOT NULL,
    remaining INT NOT NULL,
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP,
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(50),
    CONSTRAINT fk_p_session_options_session FOREIGN KEY (session_id) REFERENCES p_product_sessions(id)
);
