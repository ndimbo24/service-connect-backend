-- Migration: Add structured location fields for technicians
-- Date: 2025-01-28

-- Add new location columns to technicians table
ALTER TABLE technicians
    ADD COLUMN IF NOT EXISTS region VARCHAR(100) NOT NULL DEFAULT 'Unknown',
    ADD COLUMN IF NOT EXISTS district VARCHAR(100) NOT NULL DEFAULT 'Unknown',
    ADD COLUMN IF NOT EXISTS street VARCHAR(200) NOT NULL DEFAULT 'Unknown';

-- Add index for fast region+district lookups
CREATE INDEX IF NOT EXISTS idx_technicians_region_district
    ON technicians(region, district);

