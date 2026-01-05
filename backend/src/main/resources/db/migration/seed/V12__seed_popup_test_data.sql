-- Popup integration test seed data (actual schema)
DELETE FROM p_product_sessions WHERE id IN (
	'00000000-0000-0000-0000-000000000201'::uuid
);
DELETE FROM p_products WHERE id IN (
	'00000000-0000-0000-0000-000000000101'::uuid,
	'00000000-0000-0000-0000-000000000155'::uuid
);
DELETE FROM p_stores WHERE id IN (
	'00000000-0000-0000-0000-000000000001'::uuid
);
DELETE FROM p_users WHERE id IN (1, 10);

INSERT INTO p_users (
	id, email, password, phone, name, role, is_active, created_at, updated_at
) VALUES
	(1, 'seed@popcorn.local', 'test', '01000000000', 'Seed User', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
	(10, 'owner@popcorn.local', 'test', '01011112222', 'Seed Owner', 'OWNER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO p_stores (
	id, owner_id, name, publish_status, created_at, updated_at
) VALUES (
	'00000000-0000-0000-0000-000000000001'::uuid,
	10,
	'Seed Store',
	'PUBLISHED',
	CURRENT_TIMESTAMP,
	CURRENT_TIMESTAMP
);

INSERT INTO p_products (
	id, store_id, title, description, category, status, is_hidden, created_at, updated_at
) VALUES
	('00000000-0000-0000-0000-000000000101'::uuid, '00000000-0000-0000-0000-000000000001'::uuid,
	 'Seed Product 1', '예약형 상품', 'FOOD', 'ACTIVE', FALSE, TIMESTAMP '2025-01-01 10:00:00', TIMESTAMP '2025-01-01 10:00:00'),
	('00000000-0000-0000-0000-000000000155'::uuid, '00000000-0000-0000-0000-000000000001'::uuid,
	 'Product Merch 55', '굿즈형 상품', 'FOOD', 'ACTIVE', FALSE, TIMESTAMP '2025-01-02 10:00:00', TIMESTAMP '2025-01-02 10:00:00');

INSERT INTO p_product_sessions (
	id, product_id, start_at, end_at, status, created_at, updated_at
) VALUES (
	'00000000-0000-0000-0000-000000000201'::uuid,
	'00000000-0000-0000-0000-000000000101'::uuid,
	TIMESTAMP '2025-01-01 10:00:00',
	TIMESTAMP '2025-01-05 18:00:00',
	'ACTIVE',
	TIMESTAMP '2025-01-01 10:00:00',
	TIMESTAMP '2025-01-01 10:00:00'
);
