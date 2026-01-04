-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_user_addresses'
	) THEN
		INSERT INTO p_user_addresses (
			id,
			user_id,
			name,
			address1,
			address2,
			postal_code,
			is_default,
			created_at,
			updated_at
		)
		SELECT
			'00000000-0000-0000-0000-000000009001'::uuid,
			1,
			'Seed User',
			'서울특별시 강남구 테헤란로 123',
			'ABC빌딩 12층 1201호',
			'06000',
			TRUE,
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_users u WHERE u.id = 1
		) AND NOT EXISTS (
			SELECT 1 FROM p_user_addresses ua WHERE ua.user_id = 1 AND ua.is_default = TRUE
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
			id,
			product_id,
			sku,
			name,
			price,
			stock,
			is_hidden,
			created_at,
			updated_at
		)
		SELECT
			'00000000-0000-0000-0000-000000000401'::uuid,
			'00000000-0000-0000-0000-000000000102'::uuid,
			'SEED-SKU-401',
			'Seed Merch Variant',
			1500,
			100,
			FALSE,
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_products p WHERE p.id = '00000000-0000-0000-0000-000000000102'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_merch_variants mv WHERE mv.id = '00000000-0000-0000-0000-000000000401'::uuid
		);
	END IF;
END $$;

-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_orders'
	) THEN
		UPDATE p_orders
		SET total_amount = 4000
		WHERE id = '00000000-0000-0000-0000-000000001001'::uuid
		  AND total_amount <> 4000;

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
			'00000000-0000-0000-0000-000000001002'::uuid,
			'O20251231-001002',
			1,
			'00000000-0000-0000-0000-000000000001'::uuid,
			'00000000-0000-0000-0000-000000000102'::uuid,
			'PURCHASE',
			'REQUESTED',
			NOW() + INTERVAL '1 hour',
			3000,
			NULL,
			NOW(),
			NOW(),
			0
		WHERE EXISTS (
			SELECT 1 FROM p_users u WHERE u.id = 1
		) AND EXISTS (
			SELECT 1 FROM p_stores s WHERE s.id = '00000000-0000-0000-0000-000000000001'::uuid
		) AND EXISTS (
			SELECT 1 FROM p_products p WHERE p.id = '00000000-0000-0000-0000-000000000102'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_orders o WHERE o.id = '00000000-0000-0000-0000-000000001002'::uuid
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
			merch_variant_id,
			qty,
			unit_price,
			line_amount,
			created_at,
			updated_at
		)
		SELECT
			'00000000-0000-0000-0000-000000002002'::uuid,
			'00000000-0000-0000-0000-000000001002'::uuid,
			'MERCH',
			'00000000-0000-0000-0000-000000000401'::uuid,
			2,
			1500,
			3000,
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_orders o WHERE o.id = '00000000-0000-0000-0000-000000001002'::uuid
		) AND EXISTS (
			SELECT 1 FROM p_merch_variants mv WHERE mv.id = '00000000-0000-0000-0000-000000000401'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_order_items oi WHERE oi.id = '00000000-0000-0000-0000-000000002002'::uuid
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
			'00000000-0000-0000-0000-000000003002'::uuid,
			'00000000-0000-0000-0000-000000001002'::uuid,
			NULL,
			'REQUESTED',
			'Seed order created',
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_orders o WHERE o.id = '00000000-0000-0000-0000-000000001002'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_order_status_histories h WHERE h.id = '00000000-0000-0000-0000-000000003002'::uuid
		);
	END IF;
END $$;

-- noinspection SqlResolve
DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_payments'
	) THEN
		INSERT INTO p_payments (
			id,
			order_id,
			method,
			status,
			amount,
			approved_at,
			created_at,
			updated_at
		)
		SELECT
			'00000000-0000-0000-0000-000000004001'::uuid,
			'00000000-0000-0000-0000-000000001001'::uuid,
			'CARD',
			'APPROVED',
			4000,
			NOW(),
			NOW(),
			NOW()
		WHERE EXISTS (
			SELECT 1 FROM p_orders o WHERE o.id = '00000000-0000-0000-0000-000000001001'::uuid
		) AND NOT EXISTS (
			SELECT 1 FROM p_payments p WHERE p.id = '00000000-0000-0000-0000-000000004001'::uuid
		);
	END IF;
END $$;
