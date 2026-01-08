-- UserFlowIntegrationTest용 최소한의 테스트 데이터
-- 이 파일은 UserFlowIntegrationTest에서만 사용됩니다.

-- 1. 기본 사용자 데이터
INSERT INTO p_users (user_id, email, password, name, phone, role, is_active, created_at, updated_at, created_by, updated_by)
VALUES
    (1, 'testuser@popcorn.com', '$2a$10$YourEncodedPasswordHashHere1234567890123456789012', '테스트사용자', '01012345678', 'CUSTOMER', true, NOW(), NOW(), 1, 1),
    (1000, 'storeowner@popcorn.com', '$2a$10$dummy.password.hash.for.test.user.only', '스토어운영자', '01011111111', 'OWNER', true, NOW(), NOW(), 1000, 1000);

-- 2. 스토어 테스트 데이터
INSERT INTO p_stores (store_id, user_id, store_name, status, created_at, updated_at, created_by, updated_by)
VALUES
    ('00000000-0000-0000-0000-000000000001', 1000, 'UserFlow 테스트 스토어', 'ACTIVE', NOW(), NOW(), 1000, 1000);

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

