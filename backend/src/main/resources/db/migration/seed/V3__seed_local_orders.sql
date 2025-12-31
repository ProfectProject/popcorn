-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_orders'
	) THEN
		INSERT INTO p_orders (
			id,
			order_no,
			customer_id,
			store_id,
			product_id,
			order_type,
			status,
			cancelable_until,
			total_amount,
			idempotency_key,
			created_at,
			updated_at,
			version
		)
		SELECT
			'00000000-0000-0000-0000-000000001001'::uuid,
			'O20251231-001001',
			1,
			'00000000-0000-0000-0000-000000000010'::uuid,
			'00000000-0000-0000-0000-000000000155'::uuid,
			'RESERVATION',
			'REQUESTED',
			NOW() + INTERVAL '1 day',
			2000,
			NULL,
			NOW(),
			NOW(),
			0
		WHERE EXISTS (
			SELECT 1 FROM p_users u WHERE u.id = 1
		) AND EXISTS (
			SELECT 1 FROM p_stores s WHERE s.id = '00000000-0000-0000-0000-000000000010'::uuid
		) AND EXISTS (
			SELECT 1 FROM p_products p WHERE p.id = '00000000-0000-0000-0000-000000000155'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_orders o WHERE o.id = '00000000-0000-0000-0000-000000001001'::uuid
		);
	END IF;
END $$;

-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_order_items'
	) THEN
		INSERT INTO p_order_items (
			id,
			order_id,
			order_item_type,
			session_option_id,
			qty,
			unit_price,
			line_amount,
			created_at,
			updated_at
		)
		SELECT
			'00000000-0000-0000-0000-000000002001'::uuid,
			'00000000-0000-0000-0000-000000001001'::uuid,
			'RESERVATION',
			'00000000-0000-0000-0000-000000000388'::uuid,
			2,
			2000,
			4000,
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_orders o WHERE o.id = '00000000-0000-0000-0000-000000001001'::uuid
		) AND EXISTS (
			SELECT 1 FROM p_session_options so WHERE so.id = '00000000-0000-0000-0000-000000000388'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_order_items oi WHERE oi.id = '00000000-0000-0000-0000-000000002001'::uuid
		);
	END IF;
END $$;

-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_order_status_histories'
	) THEN
		INSERT INTO p_order_status_histories (
			id,
			order_id,
			from_status,
			to_status,
			reason,
			changed_at,
			created_at
		)
		SELECT
			'00000000-0000-0000-0000-000000003001'::uuid,
			'00000000-0000-0000-0000-000000001001'::uuid,
			NULL,
			'PENDING',
			'Seed order created',
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_orders o WHERE o.id = '00000000-0000-0000-0000-000000001001'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_order_status_histories h WHERE h.id = '00000000-0000-0000-0000-000000003001'::uuid
		);
	END IF;
END $$;
