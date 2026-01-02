DO $$
BEGIN
	IF NOT EXISTS (
		SELECT 1 FROM pg_type WHERE typname = 'order_type'
	) THEN
		CREATE TYPE order_type AS ENUM ('RESERVATION', 'PURCHASE');
	END IF;

	IF NOT EXISTS (
		SELECT 1 FROM pg_type WHERE typname = 'order_item_type'
	) THEN
		CREATE TYPE order_item_type AS ENUM ('RESERVATION', 'MERCH');
	END IF;
END $$;

DO $$
BEGIN
	IF EXISTS (
		SELECT 1 FROM pg_type WHERE typname = 'order_status'
	) THEN
		IF NOT EXISTS (
			SELECT 1
			FROM pg_enum e
			JOIN pg_type t ON t.oid = e.enumtypid
			WHERE t.typname = 'order_status' AND e.enumlabel = 'REQUESTED'
		) THEN
			ALTER TYPE order_status ADD VALUE 'REQUESTED';
		END IF;
		IF NOT EXISTS (
			SELECT 1
			FROM pg_enum e
			JOIN pg_type t ON t.oid = e.enumtypid
			WHERE t.typname = 'order_status' AND e.enumlabel = 'OWNER_ACCEPTED'
		) THEN
			ALTER TYPE order_status ADD VALUE 'OWNER_ACCEPTED';
		END IF;
		IF NOT EXISTS (
			SELECT 1
			FROM pg_enum e
			JOIN pg_type t ON t.oid = e.enumtypid
			WHERE t.typname = 'order_status' AND e.enumlabel = 'OWNER_REJECTED'
		) THEN
			ALTER TYPE order_status ADD VALUE 'OWNER_REJECTED';
		END IF;
		IF NOT EXISTS (
			SELECT 1
			FROM pg_enum e
			JOIN pg_type t ON t.oid = e.enumtypid
			WHERE t.typname = 'order_status' AND e.enumlabel = 'CONFIRMED'
		) THEN
			ALTER TYPE order_status ADD VALUE 'CONFIRMED';
		END IF;
		IF NOT EXISTS (
			SELECT 1
			FROM pg_enum e
			JOIN pg_type t ON t.oid = e.enumtypid
			WHERE t.typname = 'order_status' AND e.enumlabel = 'PREPARING'
		) THEN
			ALTER TYPE order_status ADD VALUE 'PREPARING';
		END IF;
		IF NOT EXISTS (
			SELECT 1
			FROM pg_enum e
			JOIN pg_type t ON t.oid = e.enumtypid
			WHERE t.typname = 'order_status' AND e.enumlabel = 'READY'
		) THEN
			ALTER TYPE order_status ADD VALUE 'READY';
		END IF;
		IF NOT EXISTS (
			SELECT 1
			FROM pg_enum e
			JOIN pg_type t ON t.oid = e.enumtypid
			WHERE t.typname = 'order_status' AND e.enumlabel = 'COMPLETED'
		) THEN
			ALTER TYPE order_status ADD VALUE 'COMPLETED';
		END IF;
		IF NOT EXISTS (
			SELECT 1
			FROM pg_enum e
			JOIN pg_type t ON t.oid = e.enumtypid
			WHERE t.typname = 'order_status' AND e.enumlabel = 'CANCELLED'
		) THEN
			ALTER TYPE order_status ADD VALUE 'CANCELLED';
		END IF;
	END IF;
END $$;

DO $$
BEGIN
	IF NOT EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_name = 'p_orders' AND column_name = 'product_id'
	) THEN
		ALTER TABLE p_orders
			ADD COLUMN product_id UUID REFERENCES p_products(id);
	END IF;

	IF NOT EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_name = 'p_orders' AND column_name = 'order_type'
	) THEN
		ALTER TABLE p_orders
			ADD COLUMN order_type order_type;
	END IF;

	IF NOT EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_name = 'p_orders' AND column_name = 'idempotency_key'
	) THEN
		ALTER TABLE p_orders
			ADD COLUMN idempotency_key VARCHAR(64);
	END IF;

	IF NOT EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_name = 'p_orders' AND column_name = 'version'
	) THEN
		ALTER TABLE p_orders
			ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
	END IF;
END $$;

DO $$
BEGIN
	IF NOT EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_name = 'p_order_items' AND column_name = 'order_item_type'
	) THEN
		ALTER TABLE p_order_items
			ADD COLUMN order_item_type order_item_type;
	END IF;
END $$;
