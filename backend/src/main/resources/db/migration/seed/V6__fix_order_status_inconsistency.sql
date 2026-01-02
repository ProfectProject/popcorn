-- 주문 상태 불일치 문제 수정
DO $$
BEGIN
    -- 주문 상태 히스토리의 잘못된 'PENDING' 상태를 'REQUESTED'로 수정
    IF EXISTS (
        SELECT 1 FROM information_schema.tables WHERE table_name = 'p_order_status_histories'
    ) THEN
        UPDATE p_order_status_histories
        SET to_status = 'REQUESTED'
        WHERE to_status = 'PENDING'
          AND from_status IS NULL;

        -- 로그를 위한 확인
        RAISE NOTICE 'Fixed order status history from PENDING to REQUESTED';
    END IF;

    -- 주문 테이블의 상태도 확실히 REQUESTED로 설정
    IF EXISTS (
        SELECT 1 FROM information_schema.tables WHERE table_name = 'p_orders'
    ) THEN
        UPDATE p_orders
        SET status = 'REQUESTED'
        WHERE id = '00000000-0000-0000-0000-000000001001'::uuid
          AND status != 'REQUESTED';

        -- 로그를 위한 확인
        RAISE NOTICE 'Ensured order status is REQUESTED for test order';
    END IF;
END $$;