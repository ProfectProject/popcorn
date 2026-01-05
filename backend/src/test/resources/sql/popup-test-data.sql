-- H2 compatible popup test data
DELETE FROM p_popup_schedules;
DELETE FROM p_popups;
DELETE FROM p_stores;
DELETE FROM p_users;

INSERT INTO p_users (user_id, email, password, phone, name, role, is_active, created_at, updated_at) VALUES
(1, 'seed@popcorn.local', 'test', '01000000000', 'Seed User', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'owner@popcorn.local', 'test', '01011112222', 'Seed Owner', 'OWNER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO p_stores (store_id, user_id, store_name, status, created_at, updated_at) VALUES
('00000000-0000-0000-0000-000000000001', 10, 'Seed Store', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO p_popups (popup_id, store_id, title, description, category, status, created_at, updated_at) VALUES
('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000001', 'Seed Popup 1', '예약형 팝업', 'FOOD', 'OPEN', '2025-01-01 10:00:00', '2025-01-01 10:00:00'),
('00000000-0000-0000-0000-000000000155', '00000000-0000-0000-0000-000000000001', 'Popup Merch 55', '굿즈형 팝업', 'FOOD', 'OPEN', '2025-01-02 10:00:00', '2025-01-02 10:00:00');

INSERT INTO p_popup_schedules (schedule_id, popup_id, start_at, end_at, price, capacity, remaining_capacity, is_active, created_at, updated_at) VALUES
('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000101', '2025-01-01 10:00:00', '2025-01-05 18:00:00', 12000, 10, 0, TRUE, '2025-01-01 10:00:00', '2025-01-01 10:00:00');