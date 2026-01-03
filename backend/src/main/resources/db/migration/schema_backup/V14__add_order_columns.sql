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

ALTER TABLE p_orders
	ADD COLUMN IF NOT EXISTS product_id UUID REFERENCES p_products(id),
	ADD COLUMN IF NOT EXISTS order_type order_type,
	ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64),
	ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE p_order_items
	ADD COLUMN IF NOT EXISTS order_item_type order_item_type;
