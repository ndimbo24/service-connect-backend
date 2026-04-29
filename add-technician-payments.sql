-- Add technician payment tracking table
CREATE TABLE IF NOT EXISTS technician_payments (
    id                  BIGSERIAL PRIMARY KEY,
    technician_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount              DOUBLE PRECISION NOT NULL,
    payment_type        VARCHAR(50)  NOT NULL,  -- REGISTRATION, MONTHLY_SUBSCRIPTION, RENEWAL
    status              VARCHAR(50)  NOT NULL,  -- PENDING, COMPLETED, FAILED, OVERDUE
    payment_method      VARCHAR(50),             -- IPA, CARD, CASH, BANK_TRANSFER, etc.
    transaction_id      VARCHAR(255),            -- Reference ID from payment gateway
    failure_reason      TEXT,                    -- Reason for payment failure
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    due_date            TIMESTAMP NOT NULL,
    paid_at             TIMESTAMP
);

-- Create indexes for efficient querying
CREATE INDEX idx_payments_technician_status ON technician_payments(technician_id, status);
CREATE INDEX idx_payments_due_date ON technician_payments(due_date);
CREATE INDEX idx_payments_payment_type ON technician_payments(payment_type, status);
