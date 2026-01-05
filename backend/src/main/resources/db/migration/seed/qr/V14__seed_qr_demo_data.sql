INSERT INTO p_users (id, email, password, phone, name, role, is_active, created_at, updated_at)
VALUES (1, 'qr_demo@example.com', 'test', '01000000000', 'QR Demo User', 'USER', TRUE, now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO p_stores (id, owner_id, name, publish_status, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001'::uuid, 1, 'QR Demo Store', 'PUBLISHED', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO p_orders (id, order_no, customer_id, store_id, product_id, order_type, status, cancelable_until, total_amount, idempotency_key, created_at, updated_at, version)
VALUES ('00000000-0000-0000-0000-000000001001', 'QR-TEST-001', 1, '00000000-0000-0000-0000-000000000001', NULL, 'RESERVATION'::order_type, 'REQUESTED'::order_status, now() + interval '1 day', 10000, NULL, now(), now(), 0)
ON CONFLICT DO NOTHING;

INSERT INTO p_order_qr_codes (id, order_id, qr_code, expires_at, created_at, created_by)
VALUES ('00000000-0000-0000-0000-000000009001', '00000000-0000-0000-0000-000000001001', 'qr-test-003', now() + interval '1 day', now(), 1)
ON CONFLICT DO NOTHING;
