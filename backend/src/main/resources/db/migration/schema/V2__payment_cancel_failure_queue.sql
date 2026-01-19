-- payment schema cancel failure queue table
RESET ROLE;
SET ROLE payment_migrator;

DO $$ BEGIN
  CREATE TYPE payment.queue_status AS ENUM ('PENDING','RETRYING','SUCCESS','FAILED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE TABLE IF NOT EXISTS payment.payment_cancel_failure_queue (
  id            BIGSERIAL PRIMARY KEY,
  order_id      UUID NOT NULL,
  payment_id    UUID NOT NULL,
  payment_key   VARCHAR(255) NOT NULL,
  cancel_reason VARCHAR(255) NOT NULL,
  failure_reason VARCHAR(255),
  amount        INT NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  max_attempts  INT NOT NULL DEFAULT 5,
  next_retry_at TIMESTAMP,
  status        payment.queue_status NOT NULL DEFAULT 'PENDING',
  completed_at  TIMESTAMP,
  created_at    TIMESTAMP NOT NULL DEFAULT now(),
  updated_at    TIMESTAMP NOT NULL DEFAULT now()
);

RESET ROLE;
