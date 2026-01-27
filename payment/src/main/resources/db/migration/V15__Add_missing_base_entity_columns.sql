-- Add missing BaseEntity columns to payments table
SET search_path TO payment;

ALTER TABLE payment.payments
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Update existing records to set default values for audit columns
UPDATE payment.payments
SET created_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP,
    is_deleted = false
WHERE created_at IS NULL OR updated_at IS NULL OR is_deleted IS NULL;