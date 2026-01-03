-- noinspection SqlResolve
CREATE TABLE IF NOT EXISTS p_users (
    id BIGINT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- noinspection SqlResolve
CREATE TABLE IF NOT EXISTS p_stores (
    id UUID PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    publish_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- noinspection SqlResolve
CREATE TABLE IF NOT EXISTS p_products (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    region_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- noinspection SqlResolve
CREATE TABLE IF NOT EXISTS p_product_locations (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    name VARCHAR(100),
    address1 VARCHAR(255),
    address2 VARCHAR(255),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- noinspection SqlResolve
CREATE TABLE IF NOT EXISTS p_product_sessions (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- noinspection SqlResolve
CREATE TABLE IF NOT EXISTS p_merch_variants (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    sku VARCHAR(64),
    name VARCHAR(100) NOT NULL,
    price INT NOT NULL,
    stock INT NOT NULL,
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
);

-- noinspection SqlResolve
INSERT INTO p_users (
    id, email, password, phone, name, role, is_active, created_at, updated_at
)
SELECT 1,
    'seed@popcorn.local',
    'test',
    '01000000000',
    'Seed User',
    'USER',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM p_users WHERE id = 1
);

-- noinspection SqlResolve
INSERT INTO p_stores (
    id, owner_id, name, publish_status, created_at, updated_at
)
SELECT
    '00000000-0000-0000-0000-000000000001',
    1,
    'Seed Store',
    'PUBLISHED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM p_stores WHERE id = '00000000-0000-0000-0000-000000000001'
);

-- noinspection SqlResolve
INSERT INTO p_products (
    id, store_id, title, description, category, status, is_hidden, region_id, created_at, updated_at, deleted_at
)
SELECT
    '00000000-0000-0000-0000-000000000101',
    '00000000-0000-0000-0000-000000000001',
    'Seed Popup 1',
    '예약형 팝업',
    'POPUP',
    'OPEN',
    FALSE,
    101,
    TIMESTAMP '2025-01-01 10:00:00',
    TIMESTAMP '2025-01-01 10:00:00',
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM p_products WHERE id = '00000000-0000-0000-0000-000000000101'
);

-- noinspection SqlResolve
INSERT INTO p_products (
    id, store_id, title, description, category, status, is_hidden, region_id, created_at, updated_at, deleted_at
)
SELECT
    '00000000-0000-0000-0000-000000000155',
    '00000000-0000-0000-0000-000000000001',
    'Popup Merch 55',
    '머치형 팝업',
    'POPUP',
    'OPEN',
    FALSE,
    101,
    TIMESTAMP '2025-01-02 10:00:00',
    TIMESTAMP '2025-01-02 10:00:00',
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM p_products WHERE id = '00000000-0000-0000-0000-000000000155'
);

-- noinspection SqlResolve
INSERT INTO p_product_locations (
    id, product_id, name, address1, address2, latitude, longitude, created_at, updated_at, deleted_at
)
SELECT
    '00000000-0000-0000-0000-000000009001',
    '00000000-0000-0000-0000-000000000101',
    '팝업 테스트 장소',
    '서울특별시 강남구 테헤란로 123',
    'ABC빌딩 12층',
    37.4980000,
    127.0270000,
    TIMESTAMP '2025-01-01 09:00:00',
    TIMESTAMP '2025-01-01 09:00:00',
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM p_product_locations WHERE id = '00000000-0000-0000-0000-000000009001'
);

-- noinspection SqlResolve
INSERT INTO p_product_sessions (
    id, product_id, start_at, end_at, status, created_at, updated_at, deleted_at
)
SELECT
    '00000000-0000-0000-0000-000000000201',
    '00000000-0000-0000-0000-000000000101',
    TIMESTAMP '2025-01-01 10:00:00',
    TIMESTAMP '2025-01-05 18:00:00',
    'OPEN',
    TIMESTAMP '2025-01-01 10:00:00',
    TIMESTAMP '2025-01-01 10:00:00',
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM p_product_sessions WHERE id = '00000000-0000-0000-0000-000000000201'
);

-- noinspection SqlResolve
INSERT INTO p_merch_variants (
    id, product_id, sku, name, price, stock, is_hidden, created_at, updated_at, deleted_at
)
SELECT
    '00000000-0000-0000-0000-000000000401',
    '00000000-0000-0000-0000-000000000155',
    'SEED-POPUP-MERCH-55',
    'Popup Merch 55',
    5000,
    100,
    FALSE,
    TIMESTAMP '2025-01-02 10:00:00',
    TIMESTAMP '2025-01-02 10:00:00',
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM p_merch_variants WHERE id = '00000000-0000-0000-0000-000000000401'
);
