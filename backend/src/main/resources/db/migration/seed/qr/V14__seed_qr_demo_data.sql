INSERT INTO p_users (user_id, password, name, email, role, created_at, updated_at)
VALUES (1, 'test', 'QR Demo User', 'qr_demo@example.com', 'CUSTOMER'::user_role, now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO p_stores (store_id, user_id, store_name, status)
VALUES ('00000000-0000-0000-0000-000000000001', 1, 'QR Demo Store', 'ACTIVE'::store_status)
ON CONFLICT DO NOTHING;

INSERT INTO p_orders (id, order_no, customer_id, store_id, product_id, order_type, status, cancelable_until, total_amount, idempotency_key, created_at, updated_at, version)
VALUES ('00000000-0000-0000-0000-000000001001', 'QR-TEST-001', 1, '00000000-0000-0000-0000-000000000001', NULL, 'RESERVATION'::order_type, 'REQUESTED'::order_status, now() + interval '1 day', 10000, NULL, now(), now(), 0)
ON CONFLICT DO NOTHING;

INSERT INTO p_order_qr_codes (id, order_id, qr_code, expires_at, created_at, created_by)
VALUES ('00000000-0000-0000-0000-000000009001', '00000000-0000-0000-0000-000000001001', 'qr-test-003', now() + interval '1 day', now(), 1)
ON CONFLICT DO NOTHING;
