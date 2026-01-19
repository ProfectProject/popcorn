-- flyway:transactional=false
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_stores_store_user_active
ON p_stores (store_id, user_id)
WHERE deleted_at IS NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_popups_store_created_at_desc
ON p_popups (store_id, created_at DESC, popup_id)
WHERE deleted_at IS NULL;
