-- Migration: Create technician_payments table
-- Date: 2025-01-28

CREATE TABLE IF NOT EXISTS technician_payments (
    id BIGSERIAL PRIMARY KEY,
    technician_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reference VARCHAR(255) UNIQUE NOT NULL,
    transaction_id VARCHAR(255),
    flw_ref VARCHAR(255),
    payment_method VARCHAR(100),
    notes TEXT,
    due_date TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_technician_payment FOREIGN KEY (technician_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_technician_payments_technician_id
    ON technician_payments(technician_id);

CREATE INDEX IF NOT EXISTS idx_technician_payments_reference
    ON technician_payments(reference);

CREATE INDEX IF NOT EXISTS idx_technician_payments_status_due_date
    ON technician_payments(status, due_date);
