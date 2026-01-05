ALTER TABLE p_products
    ADD COLUMN IF NOT EXISTS region_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_p_products_popup_filters
    ON p_products (deleted_at, region_id, category, store_id, created_at);

CREATE INDEX IF NOT EXISTS idx_p_product_sessions_popup_filters
    ON p_product_sessions (product_id, deleted_at, start_at, end_at);

CREATE INDEX IF NOT EXISTS idx_p_product_locations_latest
    ON p_product_locations (product_id, deleted_at, created_at);

CREATE INDEX IF NOT EXISTS idx_p_session_options_product_sessions
    ON p_session_options (session_id, deleted_at, created_at);
