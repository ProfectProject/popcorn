ALTER TABLE p_orders
	ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

ALTER TABLE p_order_status_histories
	ALTER COLUMN from_status TYPE VARCHAR(20) USING from_status::text,
	ALTER COLUMN to_status TYPE VARCHAR(20) USING to_status::text;
