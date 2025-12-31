-- user role
CREATE TYPE user_role AS ENUM ('USER', 'MANAGER', 'OWNER', 'ADMIN');

-- store publish
CREATE TYPE store_publish_status AS ENUM ('DRAFT', 'PUBLISHED', 'CLOSED');

-- product category
CREATE TYPE product_category AS ENUM ('POPUP', 'MERCH', 'EVENT');

-- product status
CREATE TYPE product_status AS ENUM ('WAIT', 'OPEN', 'CLOSED', 'DELETED');

-- session status
CREATE TYPE session_status AS ENUM ('UPCOMING', 'OPEN', 'ENDED');

-- order status
CREATE TYPE order_status AS ENUM ('PENDING', 'PAID', 'CANCELED', 'REFUNDED', 'FAILED');

-- payment
CREATE TYPE payment_method AS ENUM ('CARD', 'CASH', 'VIRTUAL');
CREATE TYPE payment_status AS ENUM ('READY', 'APPROVED', 'FAILED', 'CANCELED');

-- CHECK-IN
CREATE TYPE checkin_status AS ENUM ('CHECKED_IN', 'CANCELED');
