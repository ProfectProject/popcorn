-- Store/order list testing seed data
INSERT INTO p_product_locations (
    id, product_id, name, address1, address2, latitude, longitude, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000009001',
    '00000000-0000-0000-0000-000000000101',
    '팝업 테스트 장소',
    '서울특별시 강남구 테헤란로 123',
    'ABC빌딩 12층',
    37.4980000,
    127.0270000,
    NOW(),
    NOW()
) ON CONFLICT (id) DO NOTHING;

-- CUSTOMER(1001) 타임라인용 주문 데이터
INSERT INTO p_orders (
    id, order_no, customer_id, store_id, status, cancelable_until, total_amount, created_at, updated_at,
    product_id, order_type, idempotency_key, version
) VALUES
(
    '00000000-0000-0000-0000-000000001301',
    'O20260102-130101',
    1001,
    '00000000-0000-0000-0000-000000000001',
    'REQUESTED',
    NOW() + INTERVAL '30 minutes',
    2000,
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '2 hours',
    '00000000-0000-0000-0000-000000000101',
    'RESERVATION',
    NULL,
    0
),
(
    '00000000-0000-0000-0000-000000001302',
    'O20260102-130102',
    1001,
    '00000000-0000-0000-0000-000000000001',
    'CONFIRMED',
    NOW() + INTERVAL '10 minutes',
    15000,
    NOW() - INTERVAL '1 hours',
    NOW() - INTERVAL '1 hours',
    '00000000-0000-0000-0000-000000000101',
    'PURCHASE',
    NULL,
    0
) ON CONFLICT (id) DO NOTHING;

-- 매장(OWNER/MANAGER) 목록용 주문 데이터
INSERT INTO p_orders (
    id, order_no, customer_id, store_id, status, cancelable_until, total_amount, created_at, updated_at,
    product_id, order_type, idempotency_key, version
) VALUES
(
    '00000000-0000-0000-0000-000000001201',
    'O20260102-120001',
    1002,
    '00000000-0000-0000-0000-000000000001',
    'REQUESTED',
    NOW() + INTERVAL '30 minutes',
    5000,
    NOW() - INTERVAL '3 hours',
    NOW() - INTERVAL '3 hours',
    '00000000-0000-0000-0000-000000000101',
    'RESERVATION',
    NULL,
    0
),
(
    '00000000-0000-0000-0000-000000001202',
    'O20260102-120002',
    1003,
    '00000000-0000-0000-0000-000000000001',
    'READY',
    NOW() + INTERVAL '30 minutes',
    8000,
    NOW() - INTERVAL '4 hours',
    NOW() - INTERVAL '4 hours',
    '00000000-0000-0000-0000-000000000101',
    'RESERVATION',
    NULL,
    0
) ON CONFLICT (id) DO NOTHING;

-- 주문 아이템 (RESERVATION / PURCHASE)
INSERT INTO p_order_items (
    id, order_id, session_option_id, merch_variant_id, qty, unit_price, line_amount, created_at, updated_at,
    order_item_type
) VALUES
(
    '00000000-0000-0000-0000-000000002301',
    '00000000-0000-0000-0000-000000001301',
    '00000000-0000-0000-0000-000000000301',
    NULL,
    1,
    2000,
    2000,
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '2 hours',
    'RESERVATION'
),
(
    '00000000-0000-0000-0000-000000002302',
    '00000000-0000-0000-0000-000000001302',
    NULL,
    '00000000-0000-0000-0000-000000000401',
    1,
    15000,
    15000,
    NOW() - INTERVAL '1 hours',
    NOW() - INTERVAL '1 hours',
    'MERCH'
) ON CONFLICT (id) DO NOTHING;
