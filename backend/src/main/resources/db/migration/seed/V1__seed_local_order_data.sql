-- noinspection SqlResolve
DO $$
DECLARE
	required_cols integer;
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

	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_stores'
	) AND EXISTS (
		SELECT 1 FROM information_schema.columns
		WHERE table_name = 'p_stores'
			AND column_name = 'id'
			AND data_type IN ('bigint', 'integer')
	) THEN
		INSERT INTO p_stores (id, owner_id, name, publish_status, created_at, updated_at)
		SELECT 1,
			1,
			'Seed Store',
			'PUBLISHED',
			NOW(),
			NOW()
		WHERE NOT EXISTS (
			SELECT 1 FROM p_stores WHERE id = 1
		);
	END IF;

	IF EXISTS (
		SELECT 1 FROM information_schema.tables WHERE table_name = 'p_products'
	) THEN
		SELECT COUNT(*) INTO required_cols
		FROM information_schema.columns
		WHERE table_name = 'p_products'
			AND is_nullable = 'NO'
			AND column_default IS NULL
			AND column_name NOT IN ('id');

		IF required_cols = 0 THEN
			INSERT INTO p_products (id)
			SELECT 1
			WHERE NOT EXISTS (
				SELECT 1 FROM p_products WHERE id = 1
			);
		END IF;
	END IF;
END $$;
