-- H2 compatible test schema
CREATE TABLE IF NOT EXISTS p_users (
    user_id     BIGSERIAL NOT NULL PRIMARY KEY,
    password    varchar(255) NOT NULL,
    name        varchar(100) NOT NULL,
    phone       varchar(11),
    email       varchar(255) NOT NULL UNIQUE,
    role        varchar(20) NOT NULL,
    is_active   boolean NOT NULL DEFAULT true,
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_customer_addresses (
    addr_id     UUID NOT NULL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    addr_name   varchar(50) NOT NULL,
    address1    varchar(255) NOT NULL,
    address2    varchar(255),
    postal_code varchar(10),
    is_default  boolean NOT NULL DEFAULT false,
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_stores (
    store_id    UUID NOT NULL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    store_name  varchar(100) NOT NULL,
    status      varchar(20) NOT NULL DEFAULT 'DRAFT',
    reason      varchar(500),
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_popups (
    popup_id    UUID NOT NULL PRIMARY KEY,
    store_id    UUID NOT NULL,
    title       varchar(200) NOT NULL,
    description text,
    category    varchar(20) NOT NULL,
    status      varchar(20) NOT NULL,
    reservation_open_at timestamp,
    address_road text,
    address_detail text,
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_popup_schedules (
    schedule_id UUID NOT NULL PRIMARY KEY,
    popup_id    UUID NOT NULL,
    start_at    timestamp NOT NULL,
    end_at      timestamp NOT NULL,
    price       int NOT NULL,
    capacity    int NOT NULL,
    remaining_capacity int NOT NULL,
    is_active   boolean NOT NULL DEFAULT false,
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_goods_variants (
    goods_id    UUID NOT NULL PRIMARY KEY,
    popup_id    UUID NOT NULL,
    stock_unit  varchar(64),
    goods_name  varchar(100) NOT NULL,
    goods_price int NOT NULL,
    stock       int NOT NULL,
    is_active   boolean DEFAULT true,
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_orders (
    order_id          UUID NOT NULL PRIMARY KEY,
    order_no          varchar(32) UNIQUE,
    user_id           BIGINT NOT NULL,
    store_id          UUID NOT NULL,
    status            varchar(20) NOT NULL,
    cancelable_until  timestamp,
    total_price       int NOT NULL,
    created_at        timestamp NOT NULL,
    updated_at        timestamp NOT NULL,
    deleted_at        timestamp,
    created_by        BIGINT,
    updated_by        BIGINT,
    deleted_by        BIGINT
);

CREATE TABLE IF NOT EXISTS p_order_goods (
    order_goods_id     UUID NOT NULL PRIMARY KEY,
    order_id           UUID NOT NULL,
    schedule_id        UUID,
    goods_variant_id   UUID,
    qty                int NOT NULL,
    unit_price         int NOT NULL,
    price              int NOT NULL,
    created_at         timestamp NOT NULL,
    updated_at         timestamp NOT NULL,
    deleted_at         timestamp,
    created_by         BIGINT,
    updated_by         BIGINT,
    deleted_by         BIGINT
);

CREATE TABLE IF NOT EXISTS p_payments (
    payment_id  UUID NOT NULL PRIMARY KEY,
    order_id    UUID NOT NULL,
    method      varchar(20) NOT NULL,
    status      varchar(20) NOT NULL,
    amount      int NOT NULL,
    raw_payload text,
    approved_at timestamp,
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_order_status_histories (
    order_status_id UUID NOT NULL PRIMARY KEY,
    order_id        UUID NOT NULL,
    from_status     varchar(20) NOT NULL,
    to_status       varchar(20) NOT NULL,
    reason          varchar(255),
    changed_at      timestamp NOT NULL,
    created_at      timestamp NOT NULL,
    updated_at      timestamp NOT NULL,
    deleted_at      timestamp,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted_by      BIGINT
);

CREATE TABLE IF NOT EXISTS p_order_qr_codes (
    qr_id      UUID NOT NULL PRIMARY KEY,
    order_id   UUID NOT NULL,
    qr_code    varchar(255) NOT NULL,
    expires_at timestamp,
    created_at timestamp NOT NULL,
    created_by BIGINT
);

CREATE TABLE IF NOT EXISTS p_checkins (
    checkin_id       UUID NOT NULL PRIMARY KEY,
    order_id         UUID NOT NULL,
    order_qr_code_id UUID NOT NULL,
    created_at       timestamp NOT NULL,
    created_by       BIGINT
);

-- Unique constraints already included in table definitions

-- Foreign Keys (will be created by Hibernate/JPA automatically in test environment)
-- QR 코드와 체크인 관련 외래키 제약조건은 테스트에서 제외 (참조 무결성 문제 방지)
-- ALTER TABLE p_order_qr_codes         ADD CONSTRAINT fk_qr_order            FOREIGN KEY (order_id) REFERENCES p_orders(order_id);
-- ALTER TABLE p_checkins               ADD CONSTRAINT fk_checkins_order      FOREIGN KEY (order_id) REFERENCES p_orders(order_id);
-- ALTER TABLE p_checkins               ADD CONSTRAINT fk_checkins_qr         FOREIGN KEY (order_qr_code_id) REFERENCES p_order_qr_codes(qr_id);
