ALTER TABLE technicians
ADD COLUMN IF NOT EXISTS account_status VARCHAR(30);

UPDATE technicians
SET account_status = 'PENDING_PAYMENT'
WHERE account_status IS NULL;

ALTER TABLE technicians
ALTER COLUMN account_status SET DEFAULT 'PENDING_PAYMENT';

ALTER TABLE technicians
ALTER COLUMN account_status SET NOT NULL;
