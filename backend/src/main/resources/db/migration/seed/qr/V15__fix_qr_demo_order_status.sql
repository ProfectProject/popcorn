-- Ensure demo order is in RESERVED status for QR flows.
UPDATE p_orders
SET status = 'RESERVED'::order_status
WHERE id = '00000000-0000-0000-0000-000000001001';
