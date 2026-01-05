-- Order Cancel Test Seed Data
-- 주문 취소 API 테스트를 위한 시드 데이터

-- 1. 취소 가능한 주문들 (REQUESTED 상태) - 로컬 테스트용으로 충분히 많이 생성
INSERT INTO p_orders (
    id, order_no, customer_id, store_id, status, cancelable_until, total_amount, created_at, updated_at,
    product_id, order_type, idempotency_key, version
) VALUES
-- 기존 주문들 (ON CONFLICT로 항상 REQUESTED 상태로 초기화)
(
    '00000000-0000-0000-0000-000000001401',
    'O20260103-140001',
    1001,
    '00000000-0000-0000-0000-000000000001',
    'REQUESTED',
    NOW() + INTERVAL '30 minutes',
    10000,
    NOW() - INTERVAL '5 minutes',
    NOW() - INTERVAL '5 minutes',
    '00000000-0000-0000-0000-000000000101',
    'RESERVATION',
    'cancel-test-001',
    0
),
(
    '00000000-0000-0000-0000-000000001402',
    'O20260103-140002',
    1001,
    '00000000-0000-0000-0000-000000000001',
    'REQUESTED',
    NOW() + INTERVAL '25 minutes',
    25000,
    NOW() - INTERVAL '10 minutes',
    NOW() - INTERVAL '10 minutes',
    '00000000-0000-0000-0000-000000000101',
    'PURCHASE',
    'cancel-test-002',
    0
),
-- 추가 취소 가능한 주문들 (더 많은 테스트 데이터)
(
    '00000000-0000-0000-0000-000000001403',
    'O20260103-140003',
    1001,
    '00000000-0000-0000-0000-000000000001',
    'REQUESTED',
    NOW() + INTERVAL '45 minutes',
    15000,
    NOW() - INTERVAL '3 minutes',
    NOW() - INTERVAL '3 minutes',
    '00000000-0000-0000-0000-000000000101',
    'RESERVATION',
    'cancel-test-003',
    0
),
(
    '00000000-0000-0000-0000-000000001404',
    'O20260103-140004',
    1002,
    '00000000-0000-0000-0000-000000000001',
    'REQUESTED',
    NOW() + INTERVAL '40 minutes',
    18000,
    NOW() - INTERVAL '7 minutes',
    NOW() - INTERVAL '7 minutes',
    '00000000-0000-0000-0000-000000000101',
    'PURCHASE',
    'cancel-test-004',
    0
),
(
    '00000000-0000-0000-0000-000000001405',
    'O20260103-140005',
    1003,
    '00000000-0000-0000-0000-000000000001',
    'REQUESTED',
    NOW() + INTERVAL '35 minutes',
    22000,
    NOW() - INTERVAL '2 minutes',
    NOW() - INTERVAL '2 minutes',
    '00000000-0000-0000-0000-000000000101',
    'RESERVATION',
    'cancel-test-005',
    0
) ON CONFLICT (id) DO UPDATE SET
    status = 'REQUESTED',
    cancelable_until = NOW() + INTERVAL '30 minutes',
    updated_at = NOW(),
    version = 0;

-- 2. 이미 취소된 주문 (CANCELLED 상태) - 취소 불가 테스트용
INSERT INTO p_orders (
    id, order_no, customer_id, store_id, status, cancelable_until, total_amount, created_at, updated_at,
    product_id, order_type, idempotency_key, version
) VALUES
(
    '00000000-0000-0000-0000-000000001501',
    'O20260103-150001',
    1001,
    '00000000-0000-0000-0000-000000000001',
    'CANCELLED',
    NOW() + INTERVAL '20 minutes',
    15000,
    NOW() - INTERVAL '30 minutes',
    NOW() - INTERVAL '10 minutes',  -- 20분 전에 취소됨
    '00000000-0000-0000-0000-000000000101',
    'RESERVATION',
    'cancel-test-003',
    1
) ON CONFLICT (id) DO NOTHING;

-- 3. 완료된 주문 (COMPLETED 상태) - 취소 불가 테스트용
INSERT INTO p_orders (
    id, order_no, customer_id, store_id, status, cancelable_until, total_amount, created_at, updated_at,
    product_id, order_type, idempotency_key, version
) VALUES
(
    '00000000-0000-0000-0000-000000001502',
    'O20260103-150002',
    1001,
    '00000000-0000-0000-0000-000000000001',
    'COMPLETED',
    NOW() - INTERVAL '10 minutes',  -- 취소 시간 이미 경과
    20000,
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '30 minutes',
    '00000000-0000-0000-0000-000000000101',
    'PURCHASE',
    'cancel-test-004',
    0
),
(
    '00000000-0000-0000-0000-000000001503',
    'O20260103-150003',
    1001,
    '00000000-0000-0000-0000-000000000001',
    'PREPARING',
    NOW() + INTERVAL '15 minutes',  -- 아직 취소 시간은 남았지만 상태상 취소 불가
    12000,
    NOW() - INTERVAL '1 hour',
    NOW() - INTERVAL '30 minutes',
    '00000000-0000-0000-0000-000000000101',
    'RESERVATION',
    'cancel-test-005',
    0
) ON CONFLICT (id) DO NOTHING;

-- 주문 아이템 추가 (취소 테스트용 주문들)
INSERT INTO p_order_items (
    id, order_id, session_option_id, merch_variant_id, qty, unit_price, line_amount, created_at, updated_at,
    order_item_type
) VALUES
-- 1401 주문의 아이템 (예약형)
(
    '00000000-0000-0000-0000-000000003401',
    '00000000-0000-0000-0000-000000001401',
    '00000000-0000-0000-0000-000000000301',
    NULL,
    2,
    5000,
    10000,
    NOW() - INTERVAL '5 minutes',
    NOW() - INTERVAL '5 minutes',
    'RESERVATION'
),
-- 1402 주문의 아이템 (구매형)
(
    '00000000-0000-0000-0000-000000003402',
    '00000000-0000-0000-0000-000000001402',
    NULL,
    '00000000-0000-0000-0000-000000000401',
    1,
    25000,
    25000,
    NOW() - INTERVAL '10 minutes',
    NOW() - INTERVAL '10 minutes',
    'MERCH'
),
-- 1403 주문의 아이템 (예약형)
(
    '00000000-0000-0000-0000-000000003403',
    '00000000-0000-0000-0000-000000001403',
    '00000000-0000-0000-0000-000000000301',
    NULL,
    3,
    5000,
    15000,
    NOW() - INTERVAL '3 minutes',
    NOW() - INTERVAL '3 minutes',
    'RESERVATION'
),
-- 1404 주문의 아이템 (구매형)
(
    '00000000-0000-0000-0000-000000003404',
    '00000000-0000-0000-0000-000000001404',
    NULL,
    '00000000-0000-0000-0000-000000000401',
    2,
    9000,
    18000,
    NOW() - INTERVAL '7 minutes',
    NOW() - INTERVAL '7 minutes',
    'MERCH'
),
-- 1405 주문의 아이템 (예약형)
(
    '00000000-0000-0000-0000-000000003405',
    '00000000-0000-0000-0000-000000001405',
    '00000000-0000-0000-0000-000000000301',
    NULL,
    4,
    5500,
    22000,
    NOW() - INTERVAL '2 minutes',
    NOW() - INTERVAL '2 minutes',
    'RESERVATION'
),
-- 1501 주문의 아이템 (취소됨)
(
    '00000000-0000-0000-0000-000000003501',
    '00000000-0000-0000-0000-000000001501',
    '00000000-0000-0000-0000-000000000301',
    NULL,
    1,
    15000,
    15000,
    NOW() - INTERVAL '30 minutes',
    NOW() - INTERVAL '30 minutes',
    'RESERVATION'
) ON CONFLICT (id) DO NOTHING;

-- 주문 상태 이력 추가 (취소된 주문용)
INSERT INTO p_order_status_histories (
    id, order_id, from_status, to_status, reason, changed_at, created_at, updated_at
) VALUES
(
    '00000000-0000-0000-0000-000000004501',
    '00000000-0000-0000-0000-000000001501',
    'REQUESTED',
    'CANCELLED',
    '고객 요청에 의한 취소',
    NOW() - INTERVAL '10 minutes',
    NOW() - INTERVAL '10 minutes',
    NOW() - INTERVAL '10 minutes'
) ON CONFLICT (id) DO NOTHING;