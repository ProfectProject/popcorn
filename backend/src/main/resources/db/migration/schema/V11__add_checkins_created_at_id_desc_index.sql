-- flyway:transactional=false
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_checkins_created_at_id_desc
ON p_checkins (created_at DESC, checkin_id DESC);
