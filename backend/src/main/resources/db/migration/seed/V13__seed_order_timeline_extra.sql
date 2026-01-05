-- Extra timeline seed data for /orders/me
INSERT INTO p_orders (
    id, order_no, customer_id, store_id, status, cancelable_until, total_amount, created_at, updated_at,
    product_id, order_type, idempotency_key, version
) VALUES
(
    '00000000-0000-0000-0000-000000001304',
    'O20260102-130104',
    1001,
    '00000000-0000-0000-0000-000000000001',
    'REQUESTED',
    NOW() + INTERVAL '20 minutes',
    12000,
    NOW() - INTERVAL '10 minutes',
    NOW() - INTERVAL '10 minutes',
    '00000000-0000-0000-0000-000000000101',
    'RESERVATION',
    NULL,
    0
) ON CONFLICT (id) DO NOTHING;

INSERT INTO p_order_items (
    id, order_id, session_option_id, merch_variant_id, qty, unit_price, line_amount, created_at, updated_at,
    order_item_type
) VALUES
(
    '00000000-0000-0000-0000-000000002304',
    '00000000-0000-0000-0000-000000001304',
    '00000000-0000-0000-0000-000000000301',
    NULL,
    1,
    12000,
    12000,
    NOW() - INTERVAL '10 minutes',
    NOW() - INTERVAL '10 minutes',
    'RESERVATION'
) ON CONFLICT (id) DO NOTHING;
