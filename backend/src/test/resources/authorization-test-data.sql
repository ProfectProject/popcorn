-- 권한 엣지 케이스 테스트용 사용자 데이터
-- AuthorizationEdgeCaseTest에서 사용하는 JWT 토큰의 사용자 정보와 일치해야 함

-- 기존 테이블 클리어 (테스트 격리를 위해) - 외래키 순서 고려
DELETE FROM p_order_goods;
DELETE FROM p_orders;
DELETE FROM p_popup_schedules;
DELETE FROM p_popups;
DELETE FROM p_stores;
DELETE FROM p_users;

-- JWT 토큰에서 사용하는 테스트 사용자들 추가
INSERT INTO p_users (user_id, email, password, name, phone, role, is_active, created_at, updated_at, created_by, updated_by)
VALUES
    -- 기본 테스트 사용자 (기존)
    (1, 'testuser@popcorn.com', '$2a$10$dummy.password.hash.for.test.user.only', '테스트사용자', '01012345678', 'CUSTOMER', true, NOW(), NOW(), 1, 1),

    -- customerToken (1001L, "customer@test.com", "CUSTOMER")
    (1001, 'customer@test.com', '$2a$10$dummy.password.hash.for.test.customer', '테스트고객', '01011111111', 'CUSTOMER', true, NOW(), NOW(), 1, 1),

    -- ownerToken (1002L, "owner@test.com", "OWNER")
    (1002, 'owner@test.com', '$2a$10$dummy.password.hash.for.test.owner', '테스트사장', '01022222222', 'OWNER', true, NOW(), NOW(), 1, 1),

    -- managerToken (1003L, "manager@test.com", "MANAGER")
    (1003, 'manager@test.com', '$2a$10$dummy.password.hash.for.test.manager', '테스트매니저', '01033333333', 'MANAGER', true, NOW(), NOW(), 1, 1),

    -- expiredToken (1004L, "expired@test.com", "CUSTOMER")
    (1004, 'expired@test.com', '$2a$10$dummy.password.hash.for.test.expired', '만료된고객', '01044444444', 'CUSTOMER', true, NOW(), NOW(), 1, 1),

    -- testMultipleTokensForSameUser (2001L, "same@user.com", "CUSTOMER")
    (2001, 'same@user.com', '$2a$10$dummy.password.hash.for.test.same', '동일한유저', '01055555555', 'CUSTOMER', true, NOW(), NOW(), 1, 1);

-- 스토어 데이터 (오너와 기본 테스트용)
INSERT INTO p_stores (store_id, user_id, store_name, status, created_at, updated_at, created_by, updated_by)
VALUES
    -- 기존 테스트 스토어
    ('00000000-0000-0000-0000-000000000001', 1, '테스트 스토어 1', 'ACTIVE', NOW(), NOW(), 1, 1),
    -- 오너 사용자를 위한 스토어 데이터 (OWNER 역할의 사용자가 스토어 주문에 접근할 수 있도록)
    ('00000000-0000-0000-0000-000000000002', 1002, '테스트 오너 스토어', 'ACTIVE', NOW(), NOW(), 1002, 1002);

-- 팝업 데이터 (기본 + 오너용)
INSERT INTO p_popups (popup_id, store_id, title, description, category, status, created_at, updated_at)
VALUES
    -- 기존 테스트 팝업
    ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000001',
     '테스트 팝업 1', '테스트용 팝업 스토어입니다', 'FOOD', 'OPEN', NOW(), NOW()),
    -- 오너용 추가 팝업 데이터 (더 많은 테스트 시나리오를 위해)
    ('00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000002',
     '오너 테스트 팝업', '오너가 관리하는 테스트 팝업입니다', 'FASHION', 'OPEN', NOW(), NOW());

-- 팝업 스케줄 데이터
INSERT INTO p_popup_schedules (schedule_id, popup_id, start_at, end_at, price, capacity, remaining_capacity, is_active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000101',
     '2025-01-08 10:00:00', '2025-01-08 18:00:00', 10000, 50, 50, true, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000202', '00000000-0000-0000-0000-000000000102',
     '2025-01-08 11:00:00', '2025-01-08 19:00:00', 15000, 30, 30, true, NOW(), NOW());

-- 주문 테스트 데이터 (order endpoints에서 반환할 데이터)
INSERT INTO p_orders (order_id, order_no, user_id, store_id, status, total_price, created_at, updated_at)
VALUES
    -- CUSTOMER(1001)의 주문
    ('00000000-0000-0000-0000-000000000301', 'ORD-2025-001', 1001, '00000000-0000-0000-0000-000000000001', 'PAID', 10000, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000302', 'ORD-2025-002', 1001, '00000000-0000-0000-0000-000000000002', 'PAYMENT_PENDING', 15000, NOW(), NOW()),
    -- OWNER(1002) 스토어의 주문들
    ('00000000-0000-0000-0000-000000000303', 'ORD-2025-003', 1003, '00000000-0000-0000-0000-000000000002', 'COMPLETED', 20000, NOW(), NOW());

-- 주문 상품 데이터
INSERT INTO p_order_goods (order_goods_id, order_id, schedule_id, qty, unit_price, price, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000401', '00000000-0000-0000-0000-000000000301', '00000000-0000-0000-0000-000000000201', 1, 10000, 10000, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000402', '00000000-0000-0000-0000-000000000302', '00000000-0000-0000-0000-000000000202', 1, 15000, 15000, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000403', '00000000-0000-0000-0000-000000000303', '00000000-0000-0000-0000-000000000202', 1, 20000, 20000, NOW(), NOW());