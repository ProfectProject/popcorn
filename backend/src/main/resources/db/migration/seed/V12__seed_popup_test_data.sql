-- Popup integration test seed data (V0 schema)
DELETE FROM p_goods_variants WHERE goods_id IN (
	'00000000-0000-0000-0000-000000000451'::uuid
);
DELETE FROM p_popup_schedules WHERE schedule_id IN (
	'00000000-0000-0000-0000-000000000201'::uuid
);
DELETE FROM p_popups WHERE popup_id IN (
	'00000000-0000-0000-0000-000000000101'::uuid,
	'00000000-0000-0000-0000-000000000155'::uuid
);
DELETE FROM p_stores WHERE store_id IN (
	'00000000-0000-0000-0000-000000000001'::uuid
);
DELETE FROM p_users WHERE user_id IN (1, 10);

INSERT INTO p_users (
	user_id, password, name, phone, email, role, is_active, created_at, updated_at
) VALUES
	(1, 'test', 'Seed User', '01000000000', 'seed@popcorn.local', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
	(10, 'test', 'Seed Owner', '01011112222', 'owner@popcorn.local', 'OWNER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO p_stores (
	store_id, user_id, store_name, status, created_at, updated_at
) VALUES (
	'00000000-0000-0000-0000-000000000001'::uuid,
	10,
	'Seed Store',
	'ACTIVE',
	CURRENT_TIMESTAMP,
	CURRENT_TIMESTAMP
);

INSERT INTO p_popups (
	popup_id, store_id, title, description, category, status, created_at, updated_at
) VALUES
	('00000000-0000-0000-0000-000000000101'::uuid, '00000000-0000-0000-0000-000000000001'::uuid,
	 'Seed Popup 1', '예약형 팝업', 'FOOD', 'OPEN', TIMESTAMP '2025-01-01 10:00:00', TIMESTAMP '2025-01-01 10:00:00'),
	('00000000-0000-0000-0000-000000000155'::uuid, '00000000-0000-0000-0000-000000000001'::uuid,
	 'Popup Merch 55', '굿즈형 팝업', 'FOOD', 'OPEN', TIMESTAMP '2025-01-02 10:00:00', TIMESTAMP '2025-01-02 10:00:00');

INSERT INTO p_popup_schedules (
	schedule_id, popup_id, start_at, end_at, price, capacity, remaining_capacity, is_active, created_at, updated_at
) VALUES (
	'00000000-0000-0000-0000-000000000201'::uuid,
	'00000000-0000-0000-0000-000000000101'::uuid,
	TIMESTAMP '2025-01-01 10:00:00',
	TIMESTAMP '2025-01-05 18:00:00',
	12000,
	50,
	50,
	TRUE,
	TIMESTAMP '2025-01-01 10:00:00',
	TIMESTAMP '2025-01-01 10:00:00'
);

INSERT INTO p_goods_variants (
	goods_id, popup_id, stock_unit, goods_name, goods_price, stock, is_active, created_at, updated_at
) VALUES (
	'00000000-0000-0000-0000-000000000451'::uuid,
	'00000000-0000-0000-0000-000000000155'::uuid,
	'SKU-055',
	'Popup Merch 55',
	15000,
	30,
	TRUE,
	TIMESTAMP '2025-01-02 10:00:00',
	TIMESTAMP '2025-01-02 10:00:00'
);
