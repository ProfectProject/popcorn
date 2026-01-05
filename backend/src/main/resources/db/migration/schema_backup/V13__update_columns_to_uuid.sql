ALTER TABLE p_product_sessions DROP CONSTRAINT IF EXISTS p_sessions_created_by_fkey;
ALTER TABLE p_product_sessions DROP CONSTRAINT IF EXISTS p_sessions_updated_by_fkey;
ALTER TABLE p_product_sessions DROP CONSTRAINT IF EXISTS p_sessions_deleted_by_fkey;

ALTER TABLE p_session_options DROP CONSTRAINT IF EXISTS p_session_options_created_by_fkey;
ALTER TABLE p_session_options DROP CONSTRAINT IF EXISTS p_session_options_updated_by_fkey;
ALTER TABLE p_session_options DROP CONSTRAINT IF EXISTS p_session_options_deleted_by_fkey;

ALTER TABLE p_merch_variants DROP CONSTRAINT IF EXISTS p_merch_created_by_fkey;
ALTER TABLE p_merch_variants DROP CONSTRAINT IF EXISTS p_merch_updated_by_fkey;
ALTER TABLE p_merch_variants DROP CONSTRAINT IF EXISTS p_merch_deleted_by_fkey;

ALTER TABLE p_merch_variants DROP CONSTRAINT IF EXISTS p_merch_variants_created_by_fkey;
ALTER TABLE p_merch_variants DROP CONSTRAINT IF EXISTS p_merch_variants_updated_by_fkey;
ALTER TABLE p_merch_variants DROP CONSTRAINT IF EXISTS p_merch_variants_deleted_by_fkey;

ALTER TABLE p_orders DROP CONSTRAINT IF EXISTS p_orders_created_by_fkey;
ALTER TABLE p_orders DROP CONSTRAINT IF EXISTS p_orders_updated_by_fkey;
ALTER TABLE p_orders DROP CONSTRAINT IF EXISTS p_orders_deleted_by_fkey;
ALTER TABLE p_orders DROP CONSTRAINT IF EXISTS p_orders_customer_id_fkey;

ALTER TABLE p_order_items DROP CONSTRAINT IF EXISTS p_order_items_created_by_fkey;
ALTER TABLE p_order_items DROP CONSTRAINT IF EXISTS p_order_items_updated_by_fkey;
ALTER TABLE p_order_items DROP CONSTRAINT IF EXISTS p_order_items_deleted_by_fkey;

ALTER TABLE p_order_status_histories DROP CONSTRAINT IF EXISTS p_order_status_histories_created_by_fkey;
ALTER TABLE p_order_status_histories DROP CONSTRAINT IF EXISTS p_order_status_histories_updated_by_fkey;
ALTER TABLE p_order_status_histories DROP CONSTRAINT IF EXISTS p_order_status_histories_deleted_by_fkey;
ALTER TABLE p_order_status_histories DROP CONSTRAINT IF EXISTS p_order_status_histories_changed_by_fkey;

ALTER TABLE p_payments DROP CONSTRAINT IF EXISTS p_payments_created_by_fkey;
ALTER TABLE p_payments DROP CONSTRAINT IF EXISTS p_payments_updated_by_fkey;
ALTER TABLE p_payments DROP CONSTRAINT IF EXISTS p_payments_deleted_by_fkey;

ALTER TABLE p_order_qr_codes DROP CONSTRAINT IF EXISTS p_order_qr_codes_created_by_fkey;
ALTER TABLE p_order_qr_codes DROP CONSTRAINT IF EXISTS p_order_qr_codes_updated_by_fkey;
ALTER TABLE p_order_qr_codes DROP CONSTRAINT IF EXISTS p_order_qr_codes_deleted_by_fkey;

ALTER TABLE p_managers_store DROP CONSTRAINT IF EXISTS p_managers_created_by_fkey;
ALTER TABLE p_managers_store DROP CONSTRAINT IF EXISTS p_managers_updated_by_fkey;
ALTER TABLE p_managers_store DROP CONSTRAINT IF EXISTS p_managers_deleted_by_fkey;
