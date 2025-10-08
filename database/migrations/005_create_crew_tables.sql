-- ═══════════════════════════════════════════════════════════════════
-- Migration: 005_create_crew_tables.sql
-- Description: Create crews, crew members, and crew history tables
-- Author: HazardHawk Crew Management System
-- Created: 2025-10-08
-- ═══════════════════════════════════════════════════════════════════

-- Crews table
CREATE TABLE IF NOT EXISTS crews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL, -- NULL if company-wide crew
    name VARCHAR(255) NOT NULL,
    crew_type VARCHAR(50) NOT NULL DEFAULT 'project_based', -- 'permanent', 'project_based', 'trade_specific'
    trade VARCHAR(50), -- 'framing', 'electrical', 'plumbing', 'concrete', etc.
    foreman_id UUID REFERENCES company_workers(id), -- Default crew foreman (can be overridden per PTP)
    location VARCHAR(255), -- 'Floor 3', 'Building A', etc.
    status VARCHAR(20) NOT NULL DEFAULT 'active', -- 'active', 'inactive', 'disbanded'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add comments for documentation
COMMENT ON TABLE crews IS 'Crew definitions for organizing workers by project, trade, or permanent assignments. Crews can be project-specific or company-wide.';
COMMENT ON COLUMN crews.crew_type IS 'Crew type: permanent (long-term company crew), project_based (temporary for specific project), trade_specific (specialized trade crew)';
COMMENT ON COLUMN crews.trade IS 'Trade specialization if applicable (framing, electrical, plumbing, concrete, etc.)';
COMMENT ON COLUMN crews.foreman_id IS 'Default foreman for this crew. Can be overridden when creating PTPs to allow flexible foreman selection.';
COMMENT ON COLUMN crews.location IS 'Current work location or zone within project site';
COMMENT ON COLUMN crews.status IS 'Crew status: active (working), inactive (temporarily not working), disbanded (permanently dissolved)';

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_crews_company ON crews(company_id, status);
CREATE INDEX IF NOT EXISTS idx_crews_project ON crews(project_id) WHERE project_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_crews_foreman ON crews(foreman_id) WHERE foreman_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_crews_trade ON crews(company_id, trade) WHERE trade IS NOT NULL;

-- Add constraint checks
ALTER TABLE crews ADD CONSTRAINT check_crew_type
    CHECK (crew_type IN ('permanent', 'project_based', 'trade_specific'));

ALTER TABLE crews ADD CONSTRAINT check_crew_status
    CHECK (status IN ('active', 'inactive', 'disbanded'));

-- Crew Members (Worker-Crew assignments)
CREATE TABLE IF NOT EXISTS crew_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    crew_id UUID NOT NULL REFERENCES crews(id) ON DELETE CASCADE,
    company_worker_id UUID NOT NULL REFERENCES company_workers(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'member', -- 'crew_lead', 'foreman', 'member'
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'active', -- 'active', 'inactive', 'transferred'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(crew_id, company_worker_id, start_date) -- Allow re-assignments
);

-- Add comments for documentation
COMMENT ON TABLE crew_members IS 'Association table linking workers to crews. Workers can be assigned to multiple crews with different roles and dates.';
COMMENT ON COLUMN crew_members.role IS 'Worker role within crew: crew_lead (assistant foreman), foreman (supervisor), member (regular worker)';
COMMENT ON COLUMN crew_members.start_date IS 'Date worker was assigned to this crew';
COMMENT ON COLUMN crew_members.end_date IS 'Date worker left this crew (NULL if currently assigned)';
COMMENT ON COLUMN crew_members.status IS 'Assignment status: active (currently assigned), inactive (temporarily removed), transferred (moved to another crew)';

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_crew_members_crew ON crew_members(crew_id, status) WHERE status = 'active';
CREATE INDEX IF NOT EXISTS idx_crew_members_worker ON crew_members(company_worker_id, status) WHERE status = 'active';
CREATE INDEX IF NOT EXISTS idx_crew_members_dates ON crew_members(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_crew_members_role ON crew_members(crew_id, role);

-- Add constraint checks
ALTER TABLE crew_members ADD CONSTRAINT check_crew_member_role
    CHECK (role IN ('crew_lead', 'foreman', 'member'));

ALTER TABLE crew_members ADD CONSTRAINT check_crew_member_status
    CHECK (status IN ('active', 'inactive', 'transferred'));

ALTER TABLE crew_members ADD CONSTRAINT check_crew_member_dates
    CHECK (start_date <= COALESCE(end_date, start_date));

-- Crew Member History (Audit trail)
CREATE TABLE IF NOT EXISTS crew_member_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    crew_id UUID NOT NULL REFERENCES crews(id),
    company_worker_id UUID NOT NULL REFERENCES company_workers(id),
    action VARCHAR(50) NOT NULL, -- 'added', 'removed', 'role_changed', 'transferred'
    previous_crew_id UUID REFERENCES crews(id),
    previous_role VARCHAR(50),
    new_role VARCHAR(50),
    changed_by UUID REFERENCES company_workers(id),
    reason TEXT,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add comments for documentation
COMMENT ON TABLE crew_member_history IS 'Audit trail for all crew membership changes. Tracks additions, removals, role changes, and transfers.';
COMMENT ON COLUMN crew_member_history.action IS 'Type of change: added (new member), removed (left crew), role_changed (promotion/demotion), transferred (moved to another crew)';
COMMENT ON COLUMN crew_member_history.changed_by IS 'Company worker who made the change (supervisor/admin)';

-- Create indexes for history queries
CREATE INDEX IF NOT EXISTS idx_crew_history_worker ON crew_member_history(company_worker_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_crew_history_crew ON crew_member_history(crew_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_crew_history_action ON crew_member_history(action, created_at DESC);

-- Add constraint checks
ALTER TABLE crew_member_history ADD CONSTRAINT check_history_action
    CHECK (action IN ('added', 'removed', 'role_changed', 'transferred'));

-- Create triggers for updated_at timestamps
CREATE TRIGGER update_crews_updated_at
    BEFORE UPDATE ON crews
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_crew_members_updated_at
    BEFORE UPDATE ON crew_members
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create trigger to automatically log crew member changes to history
CREATE OR REPLACE FUNCTION log_crew_member_change()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO crew_member_history (crew_id, company_worker_id, action, new_role, metadata)
        VALUES (NEW.crew_id, NEW.company_worker_id, 'added', NEW.role, jsonb_build_object('start_date', NEW.start_date));
    ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.role != NEW.role THEN
            INSERT INTO crew_member_history (crew_id, company_worker_id, action, previous_role, new_role)
            VALUES (NEW.crew_id, NEW.company_worker_id, 'role_changed', OLD.role, NEW.role);
        END IF;
        IF OLD.status = 'active' AND NEW.status = 'transferred' THEN
            INSERT INTO crew_member_history (crew_id, company_worker_id, action, previous_role)
            VALUES (OLD.crew_id, OLD.company_worker_id, 'transferred', OLD.role);
        END IF;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO crew_member_history (crew_id, company_worker_id, action, previous_role, metadata)
        VALUES (OLD.crew_id, OLD.company_worker_id, 'removed', OLD.role, jsonb_build_object('end_date', OLD.end_date));
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER crew_member_change_log
    AFTER INSERT OR UPDATE OR DELETE ON crew_members
    FOR EACH ROW
    EXECUTE FUNCTION log_crew_member_change();

-- Create view for crew rosters with member details
CREATE OR REPLACE VIEW crew_rosters_view AS
SELECT
    c.id AS crew_id,
    c.company_id,
    c.project_id,
    c.name AS crew_name,
    c.crew_type,
    c.trade,
    c.location,
    c.status AS crew_status,
    foreman.id AS foreman_id,
    foreman_profile.first_name || ' ' || foreman_profile.last_name AS foreman_name,
    cm.id AS crew_member_id,
    cm.company_worker_id,
    cm.role AS member_role,
    cm.status AS member_status,
    wp.first_name,
    wp.last_name,
    wp.first_name || ' ' || wp.last_name AS member_name,
    cw.employee_number,
    cw.role AS worker_role,
    cm.start_date,
    cm.end_date
FROM crews c
LEFT JOIN company_workers foreman ON c.foreman_id = foreman.id
LEFT JOIN worker_profiles foreman_profile ON foreman.worker_profile_id = foreman_profile.id
LEFT JOIN crew_members cm ON c.id = cm.crew_id AND cm.status = 'active'
LEFT JOIN company_workers cw ON cm.company_worker_id = cw.id
LEFT JOIN worker_profiles wp ON cw.worker_profile_id = wp.id
WHERE c.status = 'active';

COMMENT ON VIEW crew_rosters_view IS 'Convenience view for active crews with denormalized member details for roster generation';
