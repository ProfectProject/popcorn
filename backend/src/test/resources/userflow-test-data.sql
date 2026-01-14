-- UserFlowIntegrationTest용 최소한의 테스트 데이터
-- 이 파일은 UserFlowIntegrationTest에서만 사용됩니다.

-- 0. 기존 테스트 데이터 정리 (중복 방지)
DELETE FROM p_popup_schedules WHERE schedule_id = '00000000-0000-0000-0000-000000000201';
DELETE FROM p_popups WHERE popup_id = '00000000-0000-0000-0000-000000000101';
DELETE FROM p_stores WHERE store_id = '00000000-0000-0000-0000-000000000001';
DELETE FROM p_users WHERE email IN ('testuser@popcorn.com', 'storeowner@popcorn.com');

-- 1. 기본 사용자 데이터 (auto-increment 사용)
INSERT INTO p_users (email, password, name, phone, role, is_active, created_at, updated_at)
VALUES
    ('testuser@popcorn.com', '$2a$10$YourEncodedPasswordHashHere1234567890123456789012', '테스트사용자', '01012345678', 'CUSTOMER', true, NOW(), NOW()),
    ('storeowner@popcorn.com', '$2a$10$dummy.password.hash.for.test.user.only', '스토어운영자', '01011111111', 'OWNER', true, NOW(), NOW());

-- 2. 스토어 테스트 데이터
INSERT INTO p_stores (store_id, user_id, store_name, status, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000001',
     (SELECT user_id FROM p_users WHERE email = 'storeowner@popcorn.com'),
     'UserFlow 테스트 스토어', 'ACTIVE', NOW(), NOW());

-- 3. 팝업 테스트 데이터
INSERT INTO p_popups (popup_id, store_id, title, description, category, status, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000001',
     'UserFlow 테스트 팝업', 'UserFlowIntegrationTest용 팝업입니다', 'FOOD', 'OPEN', NOW(), NOW());

-- 4. 팝업 세션(스케줄) 테스트 데이터
INSERT INTO p_popup_schedules (schedule_id, popup_id, start_at, end_at, price, capacity, remaining_capacity, is_active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000101',
     '2025-01-08 10:00:00', '2025-01-08 18:00:00', 15000, 50, 50, true, NOW(), NOW());

