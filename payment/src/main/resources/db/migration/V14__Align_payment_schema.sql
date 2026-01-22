-- Align payment schema with JPA mapping

CREATE SCHEMA IF NOT EXISTS payment;

DO $$ BEGIN
    CREATE TYPE payment.payment_method AS ENUM ('CARD','TRANSFER','EASY_PAY');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE payment.payment_status AS ENUM ('READY','PAID','FAILED','CANCELLED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

ALTER TABLE IF EXISTS payment.payments
    DROP COLUMN IF EXISTS id,
    DROP COLUMN IF EXISTS payment_method;

DO $$ BEGIN
    ALTER TABLE payment.payments
        ALTER COLUMN status TYPE payment.payment_status
        USING status::payment.payment_status;
EXCEPTION WHEN undefined_object THEN
    -- payment.payment_status not found; skip
END $$;

ALTER TABLE IF EXISTS payment.payments
    ADD COLUMN IF NOT EXISTS payment_key VARCHAR(200);

CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_payment_key
    ON payment.payments (payment_key);
