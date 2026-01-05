-- Payment API test seed data (V0 schema)
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_orders'
	) THEN
		-- Clean existing payment/order data
		DELETE FROM p_payments
		WHERE order_id IN (
			'00000000-0000-0000-0000-000000001003'::uuid,
			'00000000-0000-0000-0000-000000001004'::uuid
		);

		DELETE FROM p_order_goods
		WHERE order_id IN (
			'00000000-0000-0000-0000-000000001003'::uuid,
			'00000000-0000-0000-0000-000000001004'::uuid
		);

		DELETE FROM p_order_status_histories
		WHERE order_id IN (
			'00000000-0000-0000-0000-000000001003'::uuid,
			'00000000-0000-0000-0000-000000001004'::uuid
		);

		DELETE FROM p_orders
		WHERE order_id IN (
			'00000000-0000-0000-0000-000000001003'::uuid,
			'00000000-0000-0000-0000-000000001004'::uuid
		);

		-- Ensure user/store/popup data exists
		DELETE FROM p_goods_variants WHERE goods_id = '00000000-0000-0000-0000-000000000401'::uuid;
		DELETE FROM p_popup_schedules WHERE schedule_id = '00000000-0000-0000-0000-000000000388'::uuid;
		DELETE FROM p_popups WHERE popup_id = '00000000-0000-0000-0000-000000000155'::uuid;
		DELETE FROM p_stores WHERE store_id = '00000000-0000-0000-0000-000000000010'::uuid;
		DELETE FROM p_users WHERE user_id IN (1, 10);

		INSERT INTO p_users (
			user_id, password, name, phone, email, role, is_active, created_at, updated_at
		) VALUES
			(1, 'test', 'Seed Customer', '01000000000', 'seed@popcorn.local', 'CUSTOMER', TRUE, NOW(), NOW()),
			(10, 'test', 'Seed Owner', '01011112222', 'owner@popcorn.local', 'OWNER', TRUE, NOW(), NOW());

		INSERT INTO p_stores (
			store_id, user_id, store_name, status, created_at, updated_at
		) VALUES (
			'00000000-0000-0000-0000-000000000010'::uuid,
			10,
			'Seed Store 10',
			'ACTIVE',
			NOW(),
			NOW()
		);

		INSERT INTO p_popups (
			popup_id, store_id, title, description, category, status, created_at, updated_at
		) VALUES (
			'00000000-0000-0000-0000-000000000155'::uuid,
			'00000000-0000-0000-0000-000000000010'::uuid,
			'Popup Merch 55',
			'결제 테스트 팝업',
			'FOOD',
			'OPEN',
			NOW(),
			NOW()
		);

		INSERT INTO p_popup_schedules (
			schedule_id, popup_id, start_at, end_at, price, capacity, remaining_capacity, is_active, created_at, updated_at
		) VALUES (
			'00000000-0000-0000-0000-000000000388'::uuid,
			'00000000-0000-0000-0000-000000000155'::uuid,
			NOW() + INTERVAL '1 day',
			NOW() + INTERVAL '2 days',
			2000,
			20,
			20,
			TRUE,
			NOW(),
			NOW()
		);

		INSERT INTO p_goods_variants (
			goods_id, popup_id, stock_unit, goods_name, goods_price, stock, is_active, created_at, updated_at
		) VALUES (
			'00000000-0000-0000-0000-000000000401'::uuid,
			'00000000-0000-0000-0000-000000000155'::uuid,
			'SKU-401',
			'Payment Test Goods',
			1500,
			20,
			TRUE,
			NOW(),
			NOW()
		);

		INSERT INTO p_orders (
			order_id,
			order_no,
			user_id,
			store_id,
			status,
			cancelable_until,
			total_price,
			created_at,
			updated_at
		) VALUES
			('00000000-0000-0000-0000-000000001003'::uuid, 'O20251231-001003', 1,
			 '00000000-0000-0000-0000-000000000010'::uuid, 'REQUESTED',
			 NOW() + INTERVAL '1 day', 4000, NOW(), NOW()),
			('00000000-0000-0000-0000-000000001004'::uuid, 'O20251231-001004', 1,
			 '00000000-0000-0000-0000-000000000010'::uuid, 'REQUESTED',
			 NOW() + INTERVAL '1 hour', 3000, NOW(), NOW());

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
		) VALUES
			('00000000-0000-0000-0000-000000002003'::uuid,
			 '00000000-0000-0000-0000-000000001003'::uuid,
			 '00000000-0000-0000-0000-000000000388'::uuid,
			 NULL,
			 2,
			 2000,
			 4000,
			 NOW(),
			 NOW()),
			('00000000-0000-0000-0000-000000002004'::uuid,
			 '00000000-0000-0000-0000-000000001004'::uuid,
			 NULL,
			 '00000000-0000-0000-0000-000000000401'::uuid,
			 2,
			 1500,
			 3000,
			 NOW(),
			 NOW());

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
			('00000000-0000-0000-0000-000000003003'::uuid,
			 '00000000-0000-0000-0000-000000001003'::uuid,
			 NULL,
			 'REQUESTED',
			 'Seed order created for reservation payment API',
			 NOW(),
			 NOW(),
			 NOW()),
			('00000000-0000-0000-0000-000000003004'::uuid,
			 '00000000-0000-0000-0000-000000001004'::uuid,
			 NULL,
			 'REQUESTED',
			 'Seed order created for order payment API',
			 NOW(),
			 NOW(),
			 NOW());
	END IF;
END $$;
