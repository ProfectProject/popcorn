-- V10 popup seed data (actual schema)
-- Clean existing seed data (모든 관련 데이터 완전 정리)
-- 1. 모든 popup_schedules 삭제 (store_id 기준)
DELETE FROM p_popup_schedules
WHERE popup_id IN (
    SELECT popup_id FROM p_popups
    WHERE store_id = '00000000-0000-0000-0000-000000000001'
);

-- 2. 모든 popups 삭제 (store_id 기준)
DELETE FROM p_popups
WHERE store_id = '00000000-0000-0000-0000-000000000001';

-- 3. stores 삭제
DELETE FROM p_stores
WHERE store_id = '00000000-0000-0000-0000-000000000001';

-- 4. users 삭제
DELETE FROM p_users WHERE user_id IN (1, 10);

INSERT INTO p_users (
    user_id, email, password, phone, name, role, is_active, created_at, updated_at
) VALUES
    (1, 'seed@popcorn.local', 'test', '01000000000', 'Seed Customer', 'CUSTOMER', TRUE, NOW(), NOW()),
    (10, 'owner@popcorn.local', 'test', '01011112222', 'Seed Owner', 'OWNER', TRUE, NOW(), NOW());

INSERT INTO p_stores (
    store_id, user_id, store_name, status, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    10,
    'Seed Store',
    'ACTIVE',
    NOW(),
    NOW()
);

INSERT INTO p_popups (
    popup_id, store_id, title, description, category, status, created_at, updated_at
) VALUES
    ('00000000-0000-0000-0000-000000000180', '00000000-0000-0000-0000-000000000001',
     'Seed Popup Reservation', '예약형 팝업', 'FOOD', 'OPEN', NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000181', '00000000-0000-0000-0000-000000000001',
     'Seed Popup Merch', '굿즈형 팝업', 'FOOD', 'OPEN', NOW(), NOW());

INSERT INTO p_popup_schedules (
    schedule_id, popup_id, start_at, end_at, price, capacity, remaining_capacity, is_active, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000880',
    '00000000-0000-0000-0000-000000000180',
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '3 days',
    10000,
    50,
    50,
    TRUE,
    NOW(),
    NOW()
);
