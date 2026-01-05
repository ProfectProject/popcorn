-- Payment API test seed data (actual schema)
DO $$
BEGIN
	-- Clean existing payment/order data
	DELETE FROM p_payments WHERE order_id IN (
		'00000000-0000-0000-0000-000000001003'::uuid,
		'00000000-0000-0000-0000-000000001004'::uuid
	);
	DELETE FROM p_order_goods WHERE order_id IN (
		'00000000-0000-0000-0000-000000001003'::uuid,
		'00000000-0000-0000-0000-000000001004'::uuid
	);
	DELETE FROM p_order_status_histories WHERE order_id IN (
		'00000000-0000-0000-0000-000000001003'::uuid,
		'00000000-0000-0000-0000-000000001004'::uuid
	);
	DELETE FROM p_orders WHERE id IN (
		'00000000-0000-0000-0000-000000001003'::uuid,
		'00000000-0000-0000-0000-000000001004'::uuid
	);

	-- Clean related data
	DELETE FROM p_product_sessions WHERE id = '00000000-0000-0000-0000-000000000388'::uuid;
	DELETE FROM p_products WHERE id = '00000000-0000-0000-0000-000000000155'::uuid;
	DELETE FROM p_stores WHERE id = '00000000-0000-0000-0000-000000000010'::uuid;
	DELETE FROM p_users WHERE id IN (1, 10);

	INSERT INTO p_users (
		id, email, password, phone, name, role, is_active, created_at, updated_at
	) VALUES
		(1, 'seed@popcorn.local', 'test', '01000000000', 'Seed Customer', 'CUSTOMER', TRUE, NOW(), NOW()),
		(10, 'owner@popcorn.local', 'test', '01011112222', 'Seed Owner', 'OWNER', TRUE, NOW(), NOW());

	INSERT INTO p_stores (
		id, owner_id, name, publish_status, created_at, updated_at
	) VALUES (
		'00000000-0000-0000-0000-000000000010'::uuid,
		10,
		'Seed Store 10',
		'PUBLISHED',
		NOW(),
		NOW()
	);

	INSERT INTO p_products (
		id, store_id, title, description, category, status, is_hidden, created_at, updated_at
	) VALUES (
		'00000000-0000-0000-0000-000000000155'::uuid,
		'00000000-0000-0000-0000-000000000010'::uuid,
		'Payment Test Product',
		'결제 테스트 상품',
		'FOOD',
		'ACTIVE',
		FALSE,
		NOW(),
		NOW()
	);

	INSERT INTO p_product_sessions (
		id, product_id, start_at, end_at, status, created_at, updated_at
	) VALUES (
		'00000000-0000-0000-0000-000000000388'::uuid,
		'00000000-0000-0000-0000-000000000155'::uuid,
		NOW() + INTERVAL '1 day',
		NOW() + INTERVAL '2 days',
		'ACTIVE',
		NOW(),
		NOW()
	);

	INSERT INTO p_orders (
		id, order_no, customer_id, store_id, status, cancelable_until, total_amount, 
		created_at, updated_at, product_id, order_type, version, user_id, total_price
	) VALUES
		('00000000-0000-0000-0000-000000001003'::uuid, 'O20251231-001003', 1,
		 '00000000-0000-0000-0000-000000000010'::uuid, 'REQUESTED',
		 NOW() + INTERVAL '1 day', 4000, NOW(), NOW(),
		 '00000000-0000-0000-0000-000000000155'::uuid, 'RESERVATION', 0, 1, 4000),
		('00000000-0000-0000-0000-000000001004'::uuid, 'O20251231-001004', 1,
		 '00000000-0000-0000-0000-000000000010'::uuid, 'REQUESTED',
		 NOW() + INTERVAL '1 hour', 3000, NOW(), NOW(),
		 '00000000-0000-0000-0000-000000000155'::uuid, 'PURCHASE', 0, 1, 3000);

	INSERT INTO p_order_goods (
		order_goods_id, order_id, schedule_id, goods_variant_id, qty, unit_price, price, created_at, updated_at
	) VALUES
		('00000000-0000-0000-0000-000000002003'::uuid,
		 '00000000-0000-0000-0000-000000001003'::uuid,
		 '00000000-0000-0000-0000-000000000388'::uuid, NULL, 2, 2000, 4000, NOW(), NOW()),
		('00000000-0000-0000-0000-000000002004'::uuid,
		 '00000000-0000-0000-0000-000000001004'::uuid,
		 NULL, '00000000-0000-0000-0000-000000000401'::uuid, 2, 1500, 3000, NOW(), NOW());

	INSERT INTO p_order_status_histories (
		order_status_id, order_id, from_status, to_status, reason, changed_at, created_at, updated_at
	) VALUES
		('00000000-0000-0000-0000-000000003003'::uuid,
		 '00000000-0000-0000-0000-000000001003'::uuid, 'REQUESTED', 'REQUESTED',
		 'Seed order created for reservation payment API', NOW(), NOW(), NOW()),
		('00000000-0000-0000-0000-000000003004'::uuid,
		 '00000000-0000-0000-0000-000000001004'::uuid, 'REQUESTED', 'REQUESTED',
		 'Seed order created for order payment API', NOW(), NOW(), NOW());
END $$;
