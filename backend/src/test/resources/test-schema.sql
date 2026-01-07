-- H2 Test Database Schema Setup
-- This file creates custom enum types for testing to match production PostgreSQL schema

-- Create user_role enum type as a domain (H2 way to handle enums)
CREATE DOMAIN IF NOT EXISTS user_role AS VARCHAR(20)
CHECK (VALUE IN ('CUSTOMER', 'MANAGER', 'ADMIN'));

-- Create other enum types that might be used
CREATE DOMAIN IF NOT EXISTS order_status AS VARCHAR(20)
CHECK (VALUE IN ('PENDING', 'REQUESTED', 'APPROVED', 'REJECTED', 'COMPLETED', 'CANCELED', 'REFUNDED', 'ACCEPTED', 'CANCELLED', 'PAID', 'PAYMENT_PENDING', 'RESERVED'));

CREATE DOMAIN IF NOT EXISTS order_type AS VARCHAR(20)
CHECK (VALUE IN ('RESERVATION', 'PURCHASE'));

CREATE DOMAIN IF NOT EXISTS order_item_type AS VARCHAR(20)
CHECK (VALUE IN ('RESERVATION', 'GOODS'));

CREATE DOMAIN IF NOT EXISTS payment_status AS VARCHAR(20)
CHECK (VALUE IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELED'));

CREATE DOMAIN IF NOT EXISTS payment_method AS VARCHAR(20)
CHECK (VALUE IN ('CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'MOBILE_PAY'));

CREATE DOMAIN IF NOT EXISTS store_publish_status AS VARCHAR(20)
CHECK (VALUE IN ('DRAFT', 'PENDING', 'ACTIVE', 'SUSPENDED', 'CLOSED', 'HIDDEN'));

-- Popup related enums if needed
CREATE DOMAIN IF NOT EXISTS popup_category AS VARCHAR(50)
CHECK (VALUE IN ('FOOD', 'FASHION', 'BEAUTY', 'LIFESTYLE', 'ART', 'TECH', 'OTHER'));

-- Users table
CREATE TABLE IF NOT EXISTS p_users (
    user_id BIGINT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    phone VARCHAR(20),
    role user_role NOT NULL DEFAULT 'CUSTOMER',
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
    status store_publish_status NOT NULL DEFAULT 'DRAFT',
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
    category popup_category,
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
    status order_status NOT NULL DEFAULT 'PENDING',
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
    method payment_method NOT NULL,
    status payment_status NOT NULL DEFAULT 'PENDING',
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
    from_status order_status,
    to_status order_status NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by BIGINT,
    reason VARCHAR(500),
    FOREIGN KEY (order_id) REFERENCES p_orders(order_id),
    FOREIGN KEY (changed_by) REFERENCES p_users(user_id)
);