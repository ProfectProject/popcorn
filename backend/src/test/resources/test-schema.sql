-- H2 Test Database Schema Setup
-- This file creates tables for testing to match production PostgreSQL schema
-- H2 doesn't fully support PostgreSQL domains, so we use VARCHAR with constraints

-- Users table
CREATE TABLE IF NOT EXISTS p_users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

-- Stores table
CREATE TABLE IF NOT EXISTS p_stores (
    store_id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    store_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_by BIGINT,
    FOREIGN KEY (user_id) REFERENCES p_users(user_id)
);

-- Popups table
CREATE TABLE IF NOT EXISTS p_popups (
    popup_id VARCHAR(36) PRIMARY KEY,
    store_id VARCHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (store_id) REFERENCES p_stores(store_id)
);

-- Popup schedules table
CREATE TABLE IF NOT EXISTS p_popup_schedules (
    schedule_id VARCHAR(36) PRIMARY KEY,
    popup_id VARCHAR(36) NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    price INTEGER,
    capacity INTEGER,
    remaining_capacity INTEGER,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (popup_id) REFERENCES p_popups(popup_id)
);

-- Orders table
CREATE TABLE IF NOT EXISTS p_orders (
    order_id VARCHAR(36) PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    store_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_price INTEGER NOT NULL,
    cancelable_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES p_users(user_id),
    FOREIGN KEY (store_id) REFERENCES p_stores(store_id)
);

-- Order goods table
CREATE TABLE IF NOT EXISTS p_order_goods (
    order_goods_id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    schedule_id VARCHAR(36),
    goods_variant_id VARCHAR(36),
    qty INTEGER NOT NULL DEFAULT 1,
    unit_price INTEGER NOT NULL,
    price INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES p_orders(order_id),
    FOREIGN KEY (schedule_id) REFERENCES p_popup_schedules(schedule_id)
);

-- Goods variants table (for test completeness)
CREATE TABLE IF NOT EXISTS p_goods_variants (
    goods_id VARCHAR(36) PRIMARY KEY,
    popup_id VARCHAR(36) NOT NULL,
    goods_name VARCHAR(200) NOT NULL,
    stock_unit VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (popup_id) REFERENCES p_popups(popup_id)
);

-- Payments table
CREATE TABLE IF NOT EXISTS p_payments (
    payment_id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount INTEGER NOT NULL,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES p_orders(order_id)
);

-- Customer addresses table
CREATE TABLE IF NOT EXISTS p_customer_addresses (
    address_id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    addr_name VARCHAR(100),
    address1 VARCHAR(500),
    address2 VARCHAR(500),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES p_users(user_id)
);

-- Order status histories table
CREATE TABLE IF NOT EXISTS p_order_status_histories (
    history_id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by BIGINT,
    reason VARCHAR(500),
    FOREIGN KEY (order_id) REFERENCES p_orders(order_id),
    FOREIGN KEY (changed_by) REFERENCES p_users(user_id)
);