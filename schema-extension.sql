-- Incremental extension SQL for production-safe rollout
-- PostgreSQL

-- 1) Technician payment/subscription status
ALTER TABLE technicians
    ADD COLUMN IF NOT EXISTS account_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    ADD COLUMN IF NOT EXISTS subscription_expiry_at TIMESTAMP;

-- 2) Multimodal request payload + AI suggestion fields
ALTER TABLE service_requests
    ADD COLUMN IF NOT EXISTS voice_text TEXT,
    ADD COLUMN IF NOT EXISTS image_data TEXT,
    ADD COLUMN IF NOT EXISTS ai_category VARCHAR(100),
    ADD COLUMN IF NOT EXISTS ai_suggested_technician_type VARCHAR(100);

-- 3) Useful indexes for filtering
CREATE INDEX IF NOT EXISTS idx_technicians_account_status ON technicians(account_status);
CREATE INDEX IF NOT EXISTS idx_technicians_subscription_expiry ON technicians(subscription_expiry_at);
