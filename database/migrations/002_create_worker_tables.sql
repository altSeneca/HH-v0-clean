-- ═══════════════════════════════════════════════════════════════════
-- Migration: 002_create_worker_tables.sql
-- Description: Create worker profiles and company-worker association tables
-- Author: HazardHawk Crew Management System
-- Created: 2025-10-08
-- ═══════════════════════════════════════════════════════════════════

-- Worker Profiles (Global - shared across companies for subcontractors)
CREATE TABLE IF NOT EXISTS worker_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ssn_hash VARCHAR(64) UNIQUE, -- Hashed for privacy, optional
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    email VARCHAR(255),
    phone VARCHAR(20),
    photo_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add comments for documentation
COMMENT ON TABLE worker_profiles IS 'Global worker profiles that can be associated with multiple companies. Enables subcontractor workers to work for multiple companies without duplicate profiles.';
COMMENT ON COLUMN worker_profiles.ssn_hash IS 'Hashed SSN for identity verification (SHA-256). Optional field, stored hashed for privacy compliance.';
COMMENT ON COLUMN worker_profiles.photo_url IS 'S3 URL to worker photo for identification badges and sign-in sheets';

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_worker_profiles_name ON worker_profiles(last_name, first_name);
CREATE INDEX IF NOT EXISTS idx_worker_profiles_email ON worker_profiles(email) WHERE email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_worker_profiles_phone ON worker_profiles(phone) WHERE phone IS NOT NULL;

-- Company-Worker Associations (Many-to-Many)
CREATE TABLE IF NOT EXISTS company_workers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    worker_profile_id UUID NOT NULL REFERENCES worker_profiles(id) ON DELETE CASCADE,
    employee_number VARCHAR(50) NOT NULL,
    role VARCHAR(50) NOT NULL, -- 'laborer', 'foreman', 'operator', 'superintendent', etc.
    hire_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active', -- 'active', 'inactive', 'terminated'
    hourly_rate DECIMAL(10,2),
    permissions JSONB NOT NULL DEFAULT '[]', -- ['create_ptp', 'approve_certifications', etc.]
    metadata JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(company_id, employee_number),
    UNIQUE(company_id, worker_profile_id) -- One worker per company (can be removed if multiple roles needed)
);

-- Add comments for documentation
COMMENT ON TABLE company_workers IS 'Association table linking worker profiles to companies. Workers can belong to multiple companies (subcontractors) with different roles and employee numbers.';
COMMENT ON COLUMN company_workers.employee_number IS 'Company-specific employee ID. Unique within each company.';
COMMENT ON COLUMN company_workers.role IS 'Worker role within the company: laborer, skilled_worker, operator, crew_lead, foreman, superintendent, project_manager, safety_manager';
COMMENT ON COLUMN company_workers.status IS 'Employment status: active (working), inactive (temporarily not working), terminated (no longer employed)';
COMMENT ON COLUMN company_workers.hourly_rate IS 'Worker hourly rate for payroll calculations (optional, sensitive data)';
COMMENT ON COLUMN company_workers.permissions IS 'JSON array of permission strings for role-based access control';

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_company_workers_company ON company_workers(company_id, status) WHERE status = 'active';
CREATE INDEX IF NOT EXISTS idx_company_workers_profile ON company_workers(worker_profile_id);
CREATE INDEX IF NOT EXISTS idx_company_workers_role ON company_workers(company_id, role);
CREATE INDEX IF NOT EXISTS idx_company_workers_hire_date ON company_workers(hire_date);

-- Add constraint checks
ALTER TABLE company_workers ADD CONSTRAINT check_status
    CHECK (status IN ('active', 'inactive', 'terminated'));

ALTER TABLE company_workers ADD CONSTRAINT check_role
    CHECK (role IN (
        'laborer', 'skilled_worker', 'operator', 'crew_lead',
        'foreman', 'superintendent', 'project_manager', 'safety_manager'
    ));

ALTER TABLE company_workers ADD CONSTRAINT check_hourly_rate
    CHECK (hourly_rate IS NULL OR (hourly_rate >= 0 AND hourly_rate <= 999.99));

-- Create trigger for updated_at timestamp
CREATE TRIGGER update_worker_profiles_updated_at
    BEFORE UPDATE ON worker_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_company_workers_updated_at
    BEFORE UPDATE ON company_workers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create full-text search index for worker names
CREATE INDEX IF NOT EXISTS idx_worker_profiles_search
    ON worker_profiles
    USING GIN (to_tsvector('english', first_name || ' ' || last_name));
