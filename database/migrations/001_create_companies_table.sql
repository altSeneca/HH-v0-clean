-- ═══════════════════════════════════════════════════════════════════
-- Migration: 001_create_companies_table.sql
-- Description: Create companies table with centralized company information
-- Author: HazardHawk Crew Management System
-- Created: 2025-10-08
-- ═══════════════════════════════════════════════════════════════════

-- Companies (Tenants) - Base table
CREATE TABLE IF NOT EXISTS companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(63) UNIQUE NOT NULL,
    tier VARCHAR(20) NOT NULL DEFAULT 'professional',
    max_workers INTEGER NOT NULL DEFAULT 100,
    settings JSONB NOT NULL DEFAULT '{}',

    -- Centralized company information (added for Phase 1)
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(50),
    zip VARCHAR(20),
    phone VARCHAR(20),
    logo_url TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add comments for documentation
COMMENT ON TABLE companies IS 'Central tenant table storing company information and settings. Each company represents a separate tenant with isolated data.';
COMMENT ON COLUMN companies.tier IS 'Subscription tier: starter, professional, or enterprise. Determines feature access and worker limits.';
COMMENT ON COLUMN companies.max_workers IS 'Maximum number of workers allowed for this company based on subscription tier.';
COMMENT ON COLUMN companies.settings IS 'Company-specific settings stored as JSON (preferences, feature flags, etc.)';
COMMENT ON COLUMN companies.address IS 'Company headquarters street address - used in PTPs, reports, and documents';
COMMENT ON COLUMN companies.phone IS 'Primary company contact phone - auto-populated in safety documents';
COMMENT ON COLUMN companies.logo_url IS 'Company logo S3 URL - appears on all generated PDFs and reports';

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_companies_subdomain ON companies(subdomain);
CREATE INDEX IF NOT EXISTS idx_companies_tier ON companies(tier);

-- Add constraint checks
ALTER TABLE companies ADD CONSTRAINT check_tier
    CHECK (tier IN ('starter', 'professional', 'enterprise'));
ALTER TABLE companies ADD CONSTRAINT check_max_workers
    CHECK (max_workers > 0 AND max_workers <= 10000);

-- Create trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_companies_updated_at
    BEFORE UPDATE ON companies
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
