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
