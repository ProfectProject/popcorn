-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_product_sessions'
	) THEN
		INSERT INTO p_product_sessions (id, product_id, start_at, end_at, status, created_at, updated_at)
		SELECT v.id,
			v.product_id,
			v.start_at,
			v.end_at,
			v.status,
			NOW(),
			NOW()
		FROM (
			VALUES
				('00000000-0000-0000-0000-000000000201'::uuid, '00000000-0000-0000-0000-000000000101'::uuid, NOW() - INTERVAL '1 day', NOW() + INTERVAL '7 days', 'OPEN'::session_status),
				('00000000-0000-0000-0000-000000000202'::uuid, '00000000-0000-0000-0000-000000000102'::uuid, NOW() - INTERVAL '2 days', NOW() + INTERVAL '5 days', 'OPEN'::session_status),
				('00000000-0000-0000-0000-000000000203'::uuid, '00000000-0000-0000-0000-000000000103'::uuid, NOW() - INTERVAL '3 days', NOW() + INTERVAL '3 days', 'OPEN'::session_status),
				('00000000-0000-0000-0000-000000000777'::uuid, '00000000-0000-0000-0000-000000000155'::uuid, NOW() - INTERVAL '1 day', NOW() + INTERVAL '7 days', 'OPEN'::session_status)
		) v(id, product_id, start_at, end_at, status)
		WHERE EXISTS (
			SELECT 1 FROM p_products p WHERE p.id = v.product_id
		) AND NOT EXISTS (
			SELECT 1 FROM p_product_sessions s WHERE s.id = v.id
		);
	END IF;
END $$;

-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_session_options'
	) THEN
		INSERT INTO p_session_options (id, session_id, name, price, capacity, remaining, is_hidden, created_at, updated_at)
		SELECT v.id,
			v.session_id,
			v.name,
			v.price,
			v.capacity,
			v.remaining,
			FALSE,
			NOW(),
			NOW()
		FROM (
			VALUES
				('00000000-0000-0000-0000-000000000301'::uuid, '00000000-0000-0000-0000-000000000201'::uuid, 'Seed Option A', 1000, 100, 100),
				('00000000-0000-0000-0000-000000000302'::uuid, '00000000-0000-0000-0000-000000000201'::uuid, 'Seed Option B', 1500, 100, 100),
				('00000000-0000-0000-0000-000000000303'::uuid, '00000000-0000-0000-0000-000000000202'::uuid, 'Seed Option C', 1200, 80, 80),
				('00000000-0000-0000-0000-000000000304'::uuid, '00000000-0000-0000-0000-000000000203'::uuid, 'Seed Option D', 1800, 60, 60),
				('00000000-0000-0000-0000-000000000388'::uuid, '00000000-0000-0000-0000-000000000777'::uuid, 'Seed Option X', 2000, 100, 100)
		) v(id, session_id, name, price, capacity, remaining)
		WHERE EXISTS (
			SELECT 1 FROM p_product_sessions s WHERE s.id = v.session_id
		) AND NOT EXISTS (
			SELECT 1 FROM p_session_options so WHERE so.id = v.id
		);
	END IF;
END $$;
