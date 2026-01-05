-- Order status API test seed data (actual schema)
DO $$
BEGIN
	-- Use different IDs to avoid FK conflicts
	-- Clean existing data for order 1001 (proper order for FK constraints)
	DELETE FROM p_payments WHERE order_id = '00000000-0000-0000-0000-000000001001'::uuid;
	DELETE FROM p_order_goods WHERE order_id = '00000000-0000-0000-0000-000000001001'::uuid;
	DELETE FROM p_order_status_histories WHERE order_id = '00000000-0000-0000-0000-000000001001'::uuid;
	DELETE FROM p_order_items WHERE order_id = '00000000-0000-0000-0000-000000001001'::uuid;
	DELETE FROM p_orders WHERE id = '00000000-0000-0000-0000-000000001001'::uuid;

	-- Create test users if not exists
	INSERT INTO p_users (
		id, email, password, phone, name, role, is_active, created_at, updated_at
	) VALUES 
		(1001, 'customer1001@popcorn.local', 'test', '01000001001', 'Test Customer 1001', 'CUSTOMER', TRUE, NOW(), NOW())
	ON CONFLICT (id) DO NOTHING;

	INSERT INTO p_users (
		id, email, password, phone, name, role, is_active, created_at, updated_at
	) VALUES 
		(10, 'owner@popcorn.local', 'test', '01011112222', 'Test Owner', 'OWNER', TRUE, NOW(), NOW())
	ON CONFLICT (id) DO NOTHING;

	-- Create test store if not exists
	INSERT INTO p_stores (
		id, owner_id, name, publish_status, created_at, updated_at
	) VALUES (
		'00000000-0000-0000-0000-000000000001'::uuid,
		10,
		'Test Store',
		'PUBLISHED',
		NOW(),
		NOW()
	) ON CONFLICT (id) DO NOTHING;

	-- Create test product if not exists
	INSERT INTO p_products (
		id, store_id, title, description, category, status, is_hidden, created_at, updated_at
	) VALUES (
		'00000000-0000-0000-0000-000000000101'::uuid,
		'00000000-0000-0000-0000-000000000001'::uuid,
		'Test Product',
		'주문 상태 테스트 상품',
		'FOOD',
		'ACTIVE',
		FALSE,
		NOW(),
		NOW()
	) ON CONFLICT (id) DO NOTHING;

	-- Create test product session if not exists
	INSERT INTO p_product_sessions (
		id, product_id, start_at, end_at, status, created_at, updated_at
	) VALUES (
		'00000000-0000-0000-0000-000000000201'::uuid,
		'00000000-0000-0000-0000-000000000101'::uuid,
		NOW() + INTERVAL '1 day',
		NOW() + INTERVAL '2 days',
		'ACTIVE',
		NOW(),
		NOW()
	) ON CONFLICT (id) DO NOTHING;

	-- Create test order 1001
	INSERT INTO p_orders (
		id,
		order_no,
		customer_id,
		store_id,
		status,
		cancelable_until,
		total_amount,
		created_at,
		updated_at,
		product_id,
		order_type,
		version,
		user_id,
		total_price
	) VALUES (
		'00000000-0000-0000-0000-000000001001'::uuid,
		'O20250105-001001',
		1001,
		'00000000-0000-0000-0000-000000000001'::uuid,
		'PAID',
		NOW() + INTERVAL '1 day',
		30000,
		NOW() - INTERVAL '1 hour',
		NOW(),
		'00000000-0000-0000-0000-000000000101'::uuid,
		'RESERVATION',
		0,
		1001,
		30000
	);

	-- Create order items
	INSERT INTO p_order_goods (
		order_goods_id,
		order_id,
		schedule_id,
		goods_variant_id,
		qty,
		unit_price,
		price,
		created_at,
		updated_at
	) VALUES (
		'00000000-0000-0000-0000-000000002001'::uuid,
		'00000000-0000-0000-0000-000000001001'::uuid,
		'00000000-0000-0000-0000-000000000201'::uuid,
		NULL,
		2,
		15000,
		30000,
		NOW() - INTERVAL '1 hour',
		NOW()
	);

	-- Create payment record
	INSERT INTO p_payments (
		payment_id,
		order_id,
		method,
		status,
		amount,
		approved_at,
		created_at,
		updated_at
	) VALUES (
		'00000000-0000-0000-0000-000000004001'::uuid,
		'00000000-0000-0000-0000-000000001001'::uuid,
		'CARD',
		'PAID',
		30000,
		NOW() - INTERVAL '30 minutes',
		NOW() - INTERVAL '1 hour',
		NOW()
	);

	-- Create order status history
	INSERT INTO p_order_status_histories (
		order_status_id,
		order_id,
		from_status,
		to_status,
		reason,
		changed_at,
		created_at,
		updated_at
	) VALUES
		('00000000-0000-0000-0000-000000005001'::uuid,
		 '00000000-0000-0000-0000-000000001001'::uuid,
		 'REQUESTED',
		 'ACCEPTED',
		 'Order accepted by store',
		 NOW() - INTERVAL '50 minutes',
		 NOW() - INTERVAL '1 hour',
		 NOW()),
		('00000000-0000-0000-0000-000000005002'::uuid,
		 '00000000-0000-0000-0000-000000001001'::uuid,
		 'ACCEPTED',
		 'PAID',
		 'Payment completed',
		 NOW() - INTERVAL '30 minutes',
		 NOW() - INTERVAL '30 minutes',
		 NOW());

END $$;