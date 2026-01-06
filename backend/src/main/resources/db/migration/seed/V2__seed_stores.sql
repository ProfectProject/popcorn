-- 스토어 데이터 생성

INSERT INTO p_stores (
    store_id, user_id, store_name, status, reason,
    created_at, updated_at, created_by, updated_by
) VALUES
    ('20000000-0000-0000-0000-000000000001', 2001, '팝콘 카페', 'ACTIVE', '승인 완료', NOW(), NOW(), 2001, 2001),
    ('20000000-0000-0000-0000-000000000002', 2001, '아트 갤러리', 'ACTIVE', '승인 완료', NOW(), NOW(), 2001, 2001)
ON CONFLICT (store_id) DO NOTHING;