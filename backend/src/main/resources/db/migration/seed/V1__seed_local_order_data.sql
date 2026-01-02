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
		INSERT INTO p_users (id, email, password, phone, name, role, is_active, created_at, updated_at)
		SELECT 1001,
			'seed-1001@popcorn.local',
			'test',
			'01000001001',
			'Seed User 1001',
			'USER',
			TRUE,
			NOW(),
			NOW()
		WHERE NOT EXISTS (
			SELECT 1 FROM p_users WHERE id = 1001
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
		SELECT v.id,
			1,
			v.name,
			'PUBLISHED',
			NOW(),
			NOW()
		FROM (
			VALUES
				('00000000-0000-0000-0000-000000000001'::uuid, 'Seed Store'),
				('00000000-0000-0000-0000-000000000010'::uuid, 'Seed Store 10')
		) v(id, name)
		WHERE NOT EXISTS (
			SELECT 1 FROM p_stores s WHERE s.id = v.id
		);
	END IF;
END $$;

-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_products'
	) AND EXISTS (
		SELECT 1 FROM p_stores
		WHERE id IN (
			'00000000-0000-0000-0000-000000000001'::uuid,
			'00000000-0000-0000-0000-000000000010'::uuid
		)
	) THEN
		INSERT INTO p_products (id, store_id, title, category, status, created_at, updated_at)
		SELECT v.id,
			v.store_id,
			v.title,
			v.category,
			v.status,
			NOW(),
			NOW()
		FROM (
			VALUES
				('00000000-0000-0000-0000-000000000101'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'Seed Popup 1', 'POPUP', 'OPEN'),
				('00000000-0000-0000-0000-000000000102'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'Seed Merch 2', 'MERCH', 'OPEN'),
				('00000000-0000-0000-0000-000000000103'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'Seed Event 3', 'EVENT', 'OPEN'),
				('00000000-0000-0000-0000-000000000155'::uuid, '00000000-0000-0000-0000-000000000010'::uuid, 'Seed Popup 55', 'POPUP', 'OPEN')
		) v(id, store_id, title, category, status)
		WHERE NOT EXISTS (
			SELECT 1 FROM p_products p WHERE p.id = v.id
		);
	END IF;
END $$;
