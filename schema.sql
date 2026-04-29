-- =============================================================
--  Service Connect Hub — PostgreSQL Schema
--  Run this once to initialize the database manually.
--  (Spring Boot will auto-create/update via ddl-auto=update)
-- =============================================================

-- Drop existing tables in reverse dependency order
DROP TABLE IF EXISTS job_notifications       CASCADE;
DROP TABLE IF EXISTS activity_logs           CASCADE;
DROP TABLE IF EXISTS service_requests        CASCADE;
DROP TABLE IF EXISTS technician_documents    CASCADE;
DROP TABLE IF EXISTS technician_service_types CASCADE;
DROP TABLE IF EXISTS technicians             CASCADE;
DROP TABLE IF EXISTS clients                 CASCADE;
DROP TABLE IF EXISTS users                   CASCADE;

-- ─────────────────────────────────────────────────
-- USERS (base table, uses JOINED inheritance)
-- ─────────────────────────────────────────────────
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    dtype       VARCHAR(50)  NOT NULL,         -- 'client' | 'technician' | 'admin'
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) UNIQUE,
    phone       VARCHAR(20)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL,          -- client | technician | admin
    avatar      VARCHAR(500),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────────────
-- CLIENTS
-- ─────────────────────────────────────────────────
CREATE TABLE clients (
    user_id      BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    address      VARCHAR(500),
    location_lat DOUBLE PRECISION,
    location_lng DOUBLE PRECISION
);

-- ─────────────────────────────────────────────────
-- TECHNICIANS
-- ─────────────────────────────────────────────────
CREATE TABLE technicians (
    user_id           BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    status            VARCHAR(20)  NOT NULL DEFAULT 'pending',      -- pending | approved | rejected
    availability      VARCHAR(20)  NOT NULL DEFAULT 'offline',      -- offline | available | busy
    account_status    VARCHAR(30)  NOT NULL DEFAULT 'PENDING_PAYMENT', -- PENDING_PAYMENT | ACTIVE | INACTIVE | BLOCKED
    subscription_expiry_at TIMESTAMP,
    rating            DOUBLE PRECISION DEFAULT 0.0,
    total_jobs        INTEGER DEFAULT 0,
    location_lat      DOUBLE PRECISION,
    location_lng      DOUBLE PRECISION,
    location_address  VARCHAR(500),
    rejection_reason  TEXT
);

-- Technician service types (one-to-many)
CREATE TABLE technician_service_types (
    technician_id BIGINT NOT NULL REFERENCES technicians(user_id) ON DELETE CASCADE,
    service_type  VARCHAR(100) NOT NULL
);

-- Technician document URLs
CREATE TABLE technician_documents (
    technician_id BIGINT NOT NULL REFERENCES technicians(user_id) ON DELETE CASCADE,
    document_url  VARCHAR(1000) NOT NULL
);

-- ─────────────────────────────────────────────────
-- SERVICE REQUESTS
-- ─────────────────────────────────────────────────
CREATE TABLE service_requests (
    id               BIGSERIAL PRIMARY KEY,
    client_id        BIGINT       NOT NULL,
    client_name      VARCHAR(255) NOT NULL,
    technician_id    BIGINT,
    technician_name  VARCHAR(255),
    service_type     VARCHAR(100) NOT NULL,
    description      TEXT,
    voice_text       TEXT,
    image_data       TEXT,
    ai_category      VARCHAR(100),
    ai_suggested_technician_type VARCHAR(100),
    status           VARCHAR(30)  NOT NULL DEFAULT 'pending',
    location_lat     DOUBLE PRECISION NOT NULL,
    location_lng     DOUBLE PRECISION NOT NULL,
    location_address VARCHAR(500) NOT NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at     TIMESTAMP,
    estimated_arrival TIMESTAMP
);

-- ─────────────────────────────────────────────────
-- JOB NOTIFICATIONS
-- ─────────────────────────────────────────────────
CREATE TABLE job_notifications (
    id             BIGSERIAL PRIMARY KEY,
    request_id     BIGINT       NOT NULL,
    technician_id  BIGINT       NOT NULL,
    service_type   VARCHAR(100) NOT NULL,
    client_name    VARCHAR(255) NOT NULL,
    distance       DOUBLE PRECISION,
    address        VARCHAR(500),
    description    TEXT,
    expires_at     TIMESTAMP    NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'pending'  -- pending | accepted | rejected | expired
);

-- ─────────────────────────────────────────────────
-- ACTIVITY LOGS
-- ─────────────────────────────────────────────────
CREATE TABLE activity_logs (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT,
    user_name  VARCHAR(255),
    user_role  VARCHAR(50),
    action     VARCHAR(100) NOT NULL,
    details    TEXT,
    timestamp  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────────────
-- INDEXES
-- ─────────────────────────────────────────────────
CREATE INDEX idx_users_phone            ON users(phone);
CREATE INDEX idx_users_role             ON users(role);
CREATE INDEX idx_technicians_status     ON technicians(status);
CREATE INDEX idx_technicians_account_status ON technicians(account_status);
CREATE INDEX idx_technicians_avail      ON technicians(availability);
CREATE INDEX idx_requests_client        ON service_requests(client_id);
CREATE INDEX idx_requests_technician    ON service_requests(technician_id);
CREATE INDEX idx_requests_status        ON service_requests(status);
CREATE INDEX idx_notifications_tech     ON job_notifications(technician_id, status);
CREATE INDEX idx_logs_timestamp         ON activity_logs(timestamp DESC);

-- ─────────────────────────────────────────────────
-- SEED: Admin account
-- password: admin123  (BCrypt hash)
-- ─────────────────────────────────────────────────
INSERT INTO users (dtype, name, email, phone, password, role)
VALUES (
    'admin',
    'System Admin',
    'admin@serviceconnect.com',
    '1111111111',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTpHkOoR3Wy',   -- admin123
    'admin'
) ON CONFLICT (phone) DO NOTHING;
