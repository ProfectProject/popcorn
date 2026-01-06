-- 기본 사용자 데이터 생성

-- 1. 테스트 사용자 생성
INSERT INTO p_users (
    user_id, password, name, phone, email, role, is_active, 
    created_at, updated_at, created_by, updated_by
) VALUES
    (1001, '$2a$10$example.hash.for.password123', '김고객', '01012345678', 'customer@test.com', 'CUSTOMER', true, NOW(), NOW(), 1001, 1001),
    (2001, '$2a$10$example.hash.for.password123', '박사장', '01087654321', 'owner@test.com', 'OWNER', true, NOW(), NOW(), 2001, 2001),
    (3001, '$2a$10$example.hash.for.password123', '이매니저', '01055555555', 'manager@test.com', 'MANAGER', true, NOW(), NOW(), 3001, 3001)
ON CONFLICT (user_id) DO NOTHING;

-- 2. 고객 주소 정보
INSERT INTO p_customer_addresses (
    addr_id, user_id, addr_name, address1, address2, postal_code, is_default,
    created_at, updated_at, created_by, updated_by
) VALUES
    ('10000000-0000-0000-0000-000000000001', 1001, '집', '서울특별시 강남구 테헤란로 123', '456호', '06142', true, NOW(), NOW(), 1001, 1001),
    ('10000000-0000-0000-0000-000000000002', 1001, '회사', '서울특별시 서초구 서초대로 456', '7층', '06543', false, NOW(), NOW(), 1001, 1001)
ON CONFLICT (addr_id) DO NOTHING;