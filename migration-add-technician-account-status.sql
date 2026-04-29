ALTER TABLE technicians
    ADD COLUMN IF NOT EXISTS account_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT';

UPDATE technicians
SET account_status = 'PENDING_PAYMENT'
WHERE account_status IS NULL;
