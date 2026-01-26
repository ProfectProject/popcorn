SET search_path TO store;

CREATE TABLE IF NOT EXISTS store.goods_order_reservations (
    reservation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    order_no VARCHAR(64),
    popup_id UUID,
    goods_variant_id UUID,
    schedule_id UUID,
    quantity INT NOT NULL,
    reservation_type VARCHAR(32) NOT NULL DEFAULT 'GOODS',
    status VARCHAR(32) NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_goods_order_reservations_order_id
    ON store.goods_order_reservations(order_id);

CREATE INDEX IF NOT EXISTS idx_goods_order_reservations_schedule_id
    ON store.goods_order_reservations(schedule_id);
