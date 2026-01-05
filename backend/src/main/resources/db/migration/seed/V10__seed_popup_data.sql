-- V0 popup seed data
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_users'
	) THEN
		-- Clean existing seed data
		DELETE FROM p_goods_variants WHERE goods_id IN (
			'00000000-0000-0000-0000-000000000481'::uuid
		);
		DELETE FROM p_popup_schedules WHERE schedule_id IN (
			'00000000-0000-0000-0000-000000000880'::uuid
		);
		DELETE FROM p_popups WHERE popup_id IN (
			'00000000-0000-0000-0000-000000000180'::uuid,
			'00000000-0000-0000-0000-000000000181'::uuid
		);
		DELETE FROM p_stores WHERE store_id IN (
			'00000000-0000-0000-0000-000000000001'::uuid
		);
		DELETE FROM p_users WHERE user_id IN (1, 10);

		INSERT INTO p_users (
			user_id, password, name, phone, email, role, is_active, created_at, updated_at
		) VALUES
			(1, 'test', 'Seed Customer', '01000000000', 'seed@popcorn.local', 'CUSTOMER', TRUE, NOW(), NOW()),
			(10, 'test', 'Seed Owner', '01011112222', 'owner@popcorn.local', 'OWNER', TRUE, NOW(), NOW());

		INSERT INTO p_stores (
			store_id, user_id, store_name, status, created_at, updated_at
		) VALUES (
			'00000000-0000-0000-0000-000000000001'::uuid,
			10,
			'Seed Store',
			'ACTIVE',
			NOW(),
			NOW()
		);

		INSERT INTO p_popups (
			popup_id, store_id, title, description, category, status, created_at, updated_at
		) VALUES
			('00000000-0000-0000-0000-000000000180'::uuid, '00000000-0000-0000-0000-000000000001'::uuid,
			 'Seed Popup Reservation', '예약형 팝업', 'FOOD', 'OPEN', NOW(), NOW()),
			('00000000-0000-0000-0000-000000000181'::uuid, '00000000-0000-0000-0000-000000000001'::uuid,
			 'Seed Popup Merch', '굿즈형 팝업', 'FOOD', 'OPEN', NOW(), NOW());

		INSERT INTO p_popup_schedules (
			schedule_id, popup_id, start_at, end_at, price, capacity, remaining_capacity, is_active, created_at, updated_at
		) VALUES (
			'00000000-0000-0000-0000-000000000880'::uuid,
			'00000000-0000-0000-0000-000000000180'::uuid,
			NOW() - INTERVAL '1 day',
			NOW() + INTERVAL '3 days',
			12000,
			50,
			50,
			TRUE,
			NOW(),
			NOW()
		);

		INSERT INTO p_goods_variants (
			goods_id, popup_id, stock_unit, goods_name, goods_price, stock, is_active, created_at, updated_at
		) VALUES (
			'00000000-0000-0000-0000-000000000481'::uuid,
			'00000000-0000-0000-0000-000000000181'::uuid,
			'SKU-001',
			'Seed Goods',
			15000,
			30,
			TRUE,
			NOW(),
			NOW()
		);
	END IF;
END $$;
