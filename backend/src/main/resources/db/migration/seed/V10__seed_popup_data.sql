-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_users'
	) THEN
		INSERT INTO p_users (id, email, password, phone, name, role, is_active, created_at, updated_at)
		SELECT 1,
			'seed@popcorn.local',
			'test',
			'01000000000',
			'Seed User',
			'USER',
			TRUE,
			NOW(),
			NOW()
		WHERE NOT EXISTS (
			SELECT 1 FROM p_users WHERE id = 1
		);
	END IF;
END $$;

-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_stores'
	) THEN
		INSERT INTO p_stores (id, owner_id, name, publish_status, created_at, updated_at)
		SELECT
			'00000000-0000-0000-0000-000000000001'::uuid,
			1,
			'Seed Store',
			'PUBLISHED',
			NOW(),
			NOW()
		WHERE NOT EXISTS (
			SELECT 1 FROM p_stores WHERE id = '00000000-0000-0000-0000-000000000001'::uuid
		);
	END IF;
END $$;

-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_products'
	) AND EXISTS (
		SELECT 1 FROM information_schema.columns
		WHERE table_name = 'p_products' AND column_name = 'region_id'
	) THEN
		INSERT INTO p_products (
			id, store_id, title, description, category, status, is_hidden, region_id, created_at, updated_at
		)
		SELECT v.id,
			v.store_id,
			v.title,
			v.description,
			v.category,
			v.status,
			v.is_hidden,
			v.region_id,
			NOW(),
			NOW()
		FROM (
			VALUES
				('00000000-0000-0000-0000-000000000180'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'Seed Popup Reservation', '예약형 팝업', 'POPUP', 'OPEN', FALSE, 101),
				('00000000-0000-0000-0000-000000000181'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'Seed Popup Merch', '머치형 팝업', 'POPUP', 'OPEN', FALSE, 101)
		) v(id, store_id, title, description, category, status, is_hidden, region_id)
		WHERE EXISTS (
			SELECT 1 FROM p_stores s WHERE s.id = v.store_id
		) AND NOT EXISTS (
			SELECT 1 FROM p_products p WHERE p.id = v.id
		);

		UPDATE p_products
		SET region_id = 101
		WHERE id IN (
			'00000000-0000-0000-0000-000000000101'::uuid,
			'00000000-0000-0000-0000-000000000155'::uuid
		) AND region_id IS NULL;
	END IF;
END $$;

-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_products'
	) AND NOT EXISTS (
		SELECT 1 FROM information_schema.columns
		WHERE table_name = 'p_products' AND column_name = 'region_id'
	) THEN
		INSERT INTO p_products (
			id, store_id, title, description, category, status, is_hidden, created_at, updated_at
		)
		SELECT v.id,
			v.store_id,
			v.title,
			v.description,
			v.category,
			v.status,
			v.is_hidden,
			NOW(),
			NOW()
		FROM (
			VALUES
				('00000000-0000-0000-0000-000000000180'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'Seed Popup Reservation', '예약형 팝업', 'POPUP', 'OPEN', FALSE),
				('00000000-0000-0000-0000-000000000181'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'Seed Popup Merch', '머치형 팝업', 'POPUP', 'OPEN', FALSE)
		) v(id, store_id, title, description, category, status, is_hidden)
		WHERE EXISTS (
			SELECT 1 FROM p_stores s WHERE s.id = v.store_id
		) AND NOT EXISTS (
			SELECT 1 FROM p_products p WHERE p.id = v.id
		);
	END IF;
END $$;

-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_product_locations'
	) THEN
		INSERT INTO p_product_locations (
			id, product_id, name, address1, address2, latitude, longitude, created_at, updated_at
		)
		SELECT
			'00000000-0000-0000-0000-000000009155'::uuid,
			'00000000-0000-0000-0000-000000000155'::uuid,
			'Seed Popup 55 장소',
			'서울특별시 종로구 세종대로 1',
			'세종빌딩 3층',
			37.5729000,
			126.9768000,
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_products p WHERE p.id = '00000000-0000-0000-0000-000000000155'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_product_locations pl WHERE pl.id = '00000000-0000-0000-0000-000000009155'::uuid
		);

		INSERT INTO p_product_locations (
			id, product_id, name, address1, address2, latitude, longitude, created_at, updated_at
		)
		SELECT
			'00000000-0000-0000-0000-000000009180'::uuid,
			'00000000-0000-0000-0000-000000000180'::uuid,
			'Seed Popup Reservation 장소',
			'서울특별시 성동구 왕십리로 1',
			'팝업홀 2층',
			37.5612000,
			127.0377000,
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_products p WHERE p.id = '00000000-0000-0000-0000-000000000180'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_product_locations pl WHERE pl.id = '00000000-0000-0000-0000-000000009180'::uuid
		);
	END IF;
END $$;

-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_product_sessions'
	) THEN
		INSERT INTO p_product_sessions (id, product_id, start_at, end_at, status, created_at, updated_at)
		SELECT
			'00000000-0000-0000-0000-000000000880'::uuid,
			'00000000-0000-0000-0000-000000000180'::uuid,
			NOW() - INTERVAL '1 day',
			NOW() + INTERVAL '3 days',
			'OPEN'::session_status,
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_products p WHERE p.id = '00000000-0000-0000-0000-000000000180'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_product_sessions s WHERE s.id = '00000000-0000-0000-0000-000000000880'::uuid
		);

		INSERT INTO p_product_sessions (id, product_id, start_at, end_at, status, created_at, updated_at)
		SELECT
			'00000000-0000-0000-0000-000000000201'::uuid,
			'00000000-0000-0000-0000-000000000101'::uuid,
			NOW() + INTERVAL '2 days',
			NOW() + INTERVAL '2 days 8 hours',
			'OPEN'::session_status,
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_products p WHERE p.id = '00000000-0000-0000-0000-000000000101'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_product_sessions s WHERE s.id = '00000000-0000-0000-0000-000000000201'::uuid
		);

		INSERT INTO p_product_sessions (id, product_id, start_at, end_at, status, created_at, updated_at)
		SELECT
			'00000000-0000-0000-0000-000000000202'::uuid,
			'00000000-0000-0000-0000-000000000101'::uuid,
			NOW() + INTERVAL '5 days',
			NOW() + INTERVAL '5 days 6 hours',
			'OPEN'::session_status,
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_products p WHERE p.id = '00000000-0000-0000-0000-000000000101'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_product_sessions s WHERE s.id = '00000000-0000-0000-0000-000000000202'::uuid
		);

		INSERT INTO p_product_sessions (id, product_id, start_at, end_at, status, created_at, updated_at)
		SELECT
			'00000000-0000-0000-0000-000000000203'::uuid,
			'00000000-0000-0000-0000-000000000101'::uuid,
			NOW() + INTERVAL '8 days',
			NOW() + INTERVAL '8 days 4 hours',
			'OPEN'::session_status,
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_products p WHERE p.id = '00000000-0000-0000-0000-000000000101'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_product_sessions s WHERE s.id = '00000000-0000-0000-0000-000000000203'::uuid
		);
	END IF;
END $$;

-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_merch_variants'
	) THEN
		INSERT INTO p_merch_variants (
			id, product_id, sku, name, price, stock, is_hidden, created_at, updated_at
		)
		SELECT
			'00000000-0000-0000-0000-000000000881'::uuid,
			'00000000-0000-0000-0000-000000000181'::uuid,
			'SEED-POPUP-MERCH-1',
			'Seed Popup Merch Variant',
			5000,
			100,
			FALSE,
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_products p WHERE p.id = '00000000-0000-0000-0000-000000000181'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_merch_variants mv WHERE mv.id = '00000000-0000-0000-0000-000000000881'::uuid
		);
	END IF;
END $$;

-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_session_options'
	) THEN
		INSERT INTO p_session_options (
			id, session_id, name, price, capacity, is_hidden, created_at, updated_at
		)
		SELECT
			'00000000-0000-0000-0000-000000000301'::uuid,
			'00000000-0000-0000-0000-000000000201'::uuid,
			'일반 좌석',
			10000,
			20,
			FALSE,
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_product_sessions s WHERE s.id = '00000000-0000-0000-0000-000000000201'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_session_options o WHERE o.id = '00000000-0000-0000-0000-000000000301'::uuid
		);

		INSERT INTO p_session_options (
			id, session_id, name, price, capacity, is_hidden, created_at, updated_at
		)
		SELECT
			'00000000-0000-0000-0000-000000000302'::uuid,
			'00000000-0000-0000-0000-000000000201'::uuid,
			'프리미엄 좌석',
			15000,
			10,
			FALSE,
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_product_sessions s WHERE s.id = '00000000-0000-0000-0000-000000000201'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_session_options o WHERE o.id = '00000000-0000-0000-0000-000000000302'::uuid
		);
	END IF;
END $$;
