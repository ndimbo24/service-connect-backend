-- Migration: Add missing columns to technician_payments table
-- Date: 2025-01-28

-- Add flw_ref column (deprecated, kept for compatibility)
ALTER TABLE technician_payments ADD COLUMN IF NOT EXISTS flw_ref VARCHAR(255);

-- Add transaction_id column if missing
ALTER TABLE technician_payments ADD COLUMN IF NOT EXISTS transaction_id VARCHAR(255);

-- Add order_tracking_id column for Pesapal
ALTER TABLE technician_payments ADD COLUMN IF NOT EXISTS order_tracking_id VARCHAR(255);

-- Add payment_method column if missing
ALTER TABLE technician_payments ADD COLUMN IF NOT EXISTS payment_method VARCHAR(100);

-- Add notes column if missing
ALTER TABLE technician_payments ADD COLUMN IF NOT EXISTS notes TEXT;

-- Add paid_at column if missing
ALTER TABLE technician_payments ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;

-- Create index for reference lookup if not exists
CREATE INDEX IF NOT EXISTS idx_technician_payments_reference ON technician_payments(reference);

-- Create index for order_tracking_id lookup
CREATE INDEX IF NOT EXISTS idx_technician_payments_order_tracking_id ON technician_payments(order_tracking_id);

-- Create index for technician_id if not exists
CREATE INDEX IF NOT EXISTS idx_technician_payments_technician_id ON technician_payments(technician_id);
