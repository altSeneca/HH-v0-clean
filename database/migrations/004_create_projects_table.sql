-- ═══════════════════════════════════════════════════════════════════
-- Migration: 004_create_projects_table.sql
-- Description: Create projects table with centralized project information
-- Author: HazardHawk Crew Management System
-- Created: 2025-10-08
-- ═══════════════════════════════════════════════════════════════════

-- Projects table with centralized project info
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    project_number VARCHAR(50),
    location TEXT, -- General description, deprecated in favor of structured address
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'active', -- 'active', 'completed', 'on_hold'
    project_manager_id UUID REFERENCES company_workers(id),
    superintendent_id UUID REFERENCES company_workers(id),

    -- Centralized client information (eliminates duplicate entry in PTPs/reports)
    client_name VARCHAR(255),
    client_contact VARCHAR(255),
    client_phone VARCHAR(20),
    client_email VARCHAR(255),

    -- Centralized project address (structured for consistent formatting)
    street_address TEXT,
    city VARCHAR(100),
    state VARCHAR(50),
    zip VARCHAR(20),

    -- Additional project info
    general_contractor VARCHAR(255),

    metadata JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add comments for documentation
COMMENT ON TABLE projects IS 'Project master table with centralized client and location information. Single source of truth for project details used in PTPs, reports, and documents.';
COMMENT ON COLUMN projects.project_number IS 'Company-specific project identifier/code';
COMMENT ON COLUMN projects.status IS 'Project status: active (ongoing), completed (finished), on_hold (temporarily paused)';
COMMENT ON COLUMN projects.project_manager_id IS 'Company worker responsible for overall project management';
COMMENT ON COLUMN projects.superintendent_id IS 'Company worker supervising field operations';
COMMENT ON COLUMN projects.client_name IS 'Client/customer name - auto-populated in PTPs and reports';
COMMENT ON COLUMN projects.client_contact IS 'Primary client contact person';
COMMENT ON COLUMN projects.street_address IS 'Project site street address';
COMMENT ON COLUMN projects.general_contractor IS 'General contractor company name (if different from company)';
COMMENT ON COLUMN projects.metadata IS 'Additional project metadata (custom fields, budget info, etc.)';

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_projects_company ON projects(company_id, status);
CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status);
CREATE INDEX IF NOT EXISTS idx_projects_dates ON projects(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_projects_manager ON projects(project_manager_id) WHERE project_manager_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_projects_superintendent ON projects(superintendent_id) WHERE superintendent_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_projects_number ON projects(company_id, project_number) WHERE project_number IS NOT NULL;

-- Add constraint checks
ALTER TABLE projects ADD CONSTRAINT check_project_status
    CHECK (status IN ('active', 'completed', 'on_hold'));

ALTER TABLE projects ADD CONSTRAINT check_project_dates
    CHECK (start_date <= COALESCE(end_date, start_date));

-- Create trigger for updated_at timestamp
CREATE TRIGGER update_projects_updated_at
    BEFORE UPDATE ON projects
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create full-text search index for project names
CREATE INDEX IF NOT EXISTS idx_projects_search
    ON projects
    USING GIN (to_tsvector('english', name || ' ' || COALESCE(project_number, '')));

-- Create view for active projects with manager details
CREATE OR REPLACE VIEW active_projects_view AS
SELECT
    p.id,
    p.company_id,
    p.name,
    p.project_number,
    p.start_date,
    p.end_date,
    p.client_name,
    p.street_address,
    p.city,
    p.state,
    p.zip,
    p.general_contractor,
    pm.id AS project_manager_id,
    pm_profile.first_name || ' ' || pm_profile.last_name AS project_manager_name,
    super.id AS superintendent_id,
    super_profile.first_name || ' ' || super_profile.last_name AS superintendent_name,
    p.created_at,
    p.updated_at
FROM projects p
LEFT JOIN company_workers pm ON p.project_manager_id = pm.id
LEFT JOIN worker_profiles pm_profile ON pm.worker_profile_id = pm_profile.id
LEFT JOIN company_workers super ON p.superintendent_id = super.id
LEFT JOIN worker_profiles super_profile ON super.worker_profile_id = super_profile.id
WHERE p.status = 'active';

COMMENT ON VIEW active_projects_view IS 'Convenience view for active projects with denormalized manager/superintendent names';
