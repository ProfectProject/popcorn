-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_product_sessions'
	) THEN
		INSERT INTO p_product_sessions (id, product_id, start_at, end_at, status, created_at, updated_at)
		SELECT
			'00000000-0000-0000-0000-000000000201'::uuid,
			'00000000-0000-0000-0000-000000000101'::uuid,
			NOW() - INTERVAL '1 day',
			NOW() + INTERVAL '7 days',
			'OPEN',
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_products p WHERE p.id = '00000000-0000-0000-0000-000000000101'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_product_sessions s WHERE s.id = '00000000-0000-0000-0000-000000000201'::uuid
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
		SELECT
			'00000000-0000-0000-0000-000000000301'::uuid,
			'00000000-0000-0000-0000-000000000201'::uuid,
			'Seed Option A',
			1000,
			100,
			100,
			FALSE,
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_product_sessions s WHERE s.id = '00000000-0000-0000-0000-000000000201'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_session_options so WHERE so.id = '00000000-0000-0000-0000-000000000301'::uuid
		);
	END IF;
END $$;
