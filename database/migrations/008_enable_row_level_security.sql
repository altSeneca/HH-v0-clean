-- ═══════════════════════════════════════════════════════════════════
-- Migration: 008_enable_row_level_security.sql
-- Description: Enable Row-Level Security (RLS) for multi-tenancy isolation
-- Author: HazardHawk Crew Management System
-- Created: 2025-10-08
-- ═══════════════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════════════
-- ROW-LEVEL SECURITY (Multi-Tenancy Isolation)
-- ═══════════════════════════════════════════════════════════════════

-- Enable RLS on all tenant-scoped tables
ALTER TABLE companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE worker_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE company_workers ENABLE ROW LEVEL SECURITY;
ALTER TABLE worker_certifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE crews ENABLE ROW LEVEL SECURITY;
ALTER TABLE crew_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE crew_member_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE magic_link_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE onboarding_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE worker_locations ENABLE ROW LEVEL SECURITY;

-- ═══════════════════════════════════════════════════════════════════
-- POLICIES: Companies
-- ═══════════════════════════════════════════════════════════════════

-- Policy: Users can only view their own company
CREATE POLICY companies_isolation_select ON companies
    FOR SELECT
    USING (id = current_setting('app.current_company_id', true)::UUID);

-- Policy: Users can update their own company (admin only - enforced at application level)
CREATE POLICY companies_isolation_update ON companies
    FOR UPDATE
    USING (id = current_setting('app.current_company_id', true)::UUID);

-- ═══════════════════════════════════════════════════════════════════
-- POLICIES: Worker Profiles
-- ═══════════════════════════════════════════════════════════════════

-- Policy: Users can view worker profiles associated with their company
CREATE POLICY worker_profiles_isolation_select ON worker_profiles
    FOR SELECT
    USING (
        id IN (
            SELECT worker_profile_id
            FROM company_workers
            WHERE company_id = current_setting('app.current_company_id', true)::UUID
        )
    );

-- Policy: Users can create new worker profiles (for onboarding)
CREATE POLICY worker_profiles_isolation_insert ON worker_profiles
    FOR INSERT
    WITH CHECK (true); -- Anyone can create profiles, association is controlled via company_workers

-- Policy: Users can update worker profiles associated with their company
CREATE POLICY worker_profiles_isolation_update ON worker_profiles
    FOR UPDATE
    USING (
        id IN (
            SELECT worker_profile_id
            FROM company_workers
            WHERE company_id = current_setting('app.current_company_id', true)::UUID
        )
    );

-- ═══════════════════════════════════════════════════════════════════
-- POLICIES: Company Workers
-- ═══════════════════════════════════════════════════════════════════

-- Policy: Users can only access workers in their company
CREATE POLICY company_workers_isolation_select ON company_workers
    FOR SELECT
    USING (company_id = current_setting('app.current_company_id', true)::UUID);

-- Policy: Users can create workers in their company
CREATE POLICY company_workers_isolation_insert ON company_workers
    FOR INSERT
    WITH CHECK (company_id = current_setting('app.current_company_id', true)::UUID);

-- Policy: Users can update workers in their company
CREATE POLICY company_workers_isolation_update ON company_workers
    FOR UPDATE
    USING (company_id = current_setting('app.current_company_id', true)::UUID);

-- Policy: Users can delete workers in their company
CREATE POLICY company_workers_isolation_delete ON company_workers
    FOR DELETE
    USING (company_id = current_setting('app.current_company_id', true)::UUID);

-- ═══════════════════════════════════════════════════════════════════
-- POLICIES: Worker Certifications
-- ═══════════════════════════════════════════════════════════════════

-- Policy: Users can view certifications for workers in their company
CREATE POLICY worker_certifications_isolation_select ON worker_certifications
    FOR SELECT
    USING (
        company_id = current_setting('app.current_company_id', true)::UUID
        OR (
            company_id IS NULL
            AND worker_profile_id IN (
                SELECT worker_profile_id
                FROM company_workers
                WHERE company_id = current_setting('app.current_company_id', true)::UUID
            )
        )
    );

-- Policy: Users can create certifications for workers in their company
CREATE POLICY worker_certifications_isolation_insert ON worker_certifications
    FOR INSERT
    WITH CHECK (
        company_id = current_setting('app.current_company_id', true)::UUID
        OR (
            company_id IS NULL
            AND worker_profile_id IN (
                SELECT worker_profile_id
                FROM company_workers
                WHERE company_id = current_setting('app.current_company_id', true)::UUID
            )
        )
    );

-- Policy: Users can update certifications for workers in their company
CREATE POLICY worker_certifications_isolation_update ON worker_certifications
    FOR UPDATE
    USING (
        company_id = current_setting('app.current_company_id', true)::UUID
        OR (
            company_id IS NULL
            AND worker_profile_id IN (
                SELECT worker_profile_id
                FROM company_workers
                WHERE company_id = current_setting('app.current_company_id', true)::UUID
            )
        )
    );

-- ═══════════════════════════════════════════════════════════════════
-- POLICIES: Projects
-- ═══════════════════════════════════════════════════════════════════

-- Policy: Users can only access projects in their company
CREATE POLICY projects_isolation_select ON projects
    FOR SELECT
    USING (company_id = current_setting('app.current_company_id', true)::UUID);

-- Policy: Users can create projects in their company
CREATE POLICY projects_isolation_insert ON projects
    FOR INSERT
    WITH CHECK (company_id = current_setting('app.current_company_id', true)::UUID);

-- Policy: Users can update projects in their company
CREATE POLICY projects_isolation_update ON projects
    FOR UPDATE
    USING (company_id = current_setting('app.current_company_id', true)::UUID);

-- Policy: Users can delete projects in their company
CREATE POLICY projects_isolation_delete ON projects
    FOR DELETE
    USING (company_id = current_setting('app.current_company_id', true)::UUID);

-- ═══════════════════════════════════════════════════════════════════
-- POLICIES: Crews
-- ═══════════════════════════════════════════════════════════════════

-- Policy: Users can only access crews in their company
CREATE POLICY crews_isolation_select ON crews
    FOR SELECT
    USING (company_id = current_setting('app.current_company_id', true)::UUID);

-- Policy: Users can create crews in their company
CREATE POLICY crews_isolation_insert ON crews
    FOR INSERT
    WITH CHECK (company_id = current_setting('app.current_company_id', true)::UUID);

-- Policy: Users can update crews in their company
CREATE POLICY crews_isolation_update ON crews
    FOR UPDATE
    USING (company_id = current_setting('app.current_company_id', true)::UUID);

-- Policy: Users can delete crews in their company
CREATE POLICY crews_isolation_delete ON crews
    FOR DELETE
    USING (company_id = current_setting('app.current_company_id', true)::UUID);

-- ═══════════════════════════════════════════════════════════════════
-- POLICIES: Crew Members
-- ═══════════════════════════════════════════════════════════════════

-- Policy: Users can view crew members for crews in their company
CREATE POLICY crew_members_isolation_select ON crew_members
    FOR SELECT
    USING (
        crew_id IN (
            SELECT id FROM crews WHERE company_id = current_setting('app.current_company_id', true)::UUID
        )
    );

-- Policy: Users can create crew members for crews in their company
CREATE POLICY crew_members_isolation_insert ON crew_members
    FOR INSERT
    WITH CHECK (
        crew_id IN (
            SELECT id FROM crews WHERE company_id = current_setting('app.current_company_id', true)::UUID
        )
    );

-- Policy: Users can update crew members for crews in their company
CREATE POLICY crew_members_isolation_update ON crew_members
    FOR UPDATE
    USING (
        crew_id IN (
            SELECT id FROM crews WHERE company_id = current_setting('app.current_company_id', true)::UUID
        )
    );

-- Policy: Users can delete crew members for crews in their company
CREATE POLICY crew_members_isolation_delete ON crew_members
    FOR DELETE
    USING (
        crew_id IN (
            SELECT id FROM crews WHERE company_id = current_setting('app.current_company_id', true)::UUID
        )
    );

-- ═══════════════════════════════════════════════════════════════════
-- POLICIES: Crew Member History
-- ═══════════════════════════════════════════════════════════════════

-- Policy: Users can view crew history for crews in their company
CREATE POLICY crew_member_history_isolation_select ON crew_member_history
    FOR SELECT
    USING (
        crew_id IN (
            SELECT id FROM crews WHERE company_id = current_setting('app.current_company_id', true)::UUID
        )
    );

-- Policy: Users can create crew history for crews in their company (via triggers)
CREATE POLICY crew_member_history_isolation_insert ON crew_member_history
    FOR INSERT
    WITH CHECK (
        crew_id IN (
            SELECT id FROM crews WHERE company_id = current_setting('app.current_company_id', true)::UUID
        )
    );

-- ═══════════════════════════════════════════════════════════════════
-- POLICIES: Magic Link Tokens
-- ═══════════════════════════════════════════════════════════════════

-- Policy: Users can view magic links for their company
CREATE POLICY magic_link_tokens_isolation_select ON magic_link_tokens
    FOR SELECT
    USING (company_id = current_setting('app.current_company_id', true)::UUID);

-- Policy: Users can create magic links for their company
CREATE POLICY magic_link_tokens_isolation_insert ON magic_link_tokens
    FOR INSERT
    WITH CHECK (company_id = current_setting('app.current_company_id', true)::UUID);

-- Policy: Users can update magic links for their company
CREATE POLICY magic_link_tokens_isolation_update ON magic_link_tokens
    FOR UPDATE
    USING (company_id = current_setting('app.current_company_id', true)::UUID);

-- ═══════════════════════════════════════════════════════════════════
-- POLICIES: Onboarding Sessions
-- ═══════════════════════════════════════════════════════════════════

-- Policy: Users can view onboarding sessions for their company
CREATE POLICY onboarding_sessions_isolation_select ON onboarding_sessions
    FOR SELECT
    USING (company_id = current_setting('app.current_company_id', true)::UUID);

-- Policy: Users can create onboarding sessions for their company
CREATE POLICY onboarding_sessions_isolation_insert ON onboarding_sessions
    FOR INSERT
    WITH CHECK (company_id = current_setting('app.current_company_id', true)::UUID);

-- Policy: Users can update onboarding sessions for their company
CREATE POLICY onboarding_sessions_isolation_update ON onboarding_sessions
    FOR UPDATE
    USING (company_id = current_setting('app.current_company_id', true)::UUID);

-- ═══════════════════════════════════════════════════════════════════
-- POLICIES: Worker Locations
-- ═══════════════════════════════════════════════════════════════════

-- Policy: Users can view locations for workers in their company
CREATE POLICY worker_locations_isolation_select ON worker_locations
    FOR SELECT
    USING (
        company_worker_id IN (
            SELECT id FROM company_workers WHERE company_id = current_setting('app.current_company_id', true)::UUID
        )
    );

-- Policy: Users can create locations for workers in their company
CREATE POLICY worker_locations_isolation_insert ON worker_locations
    FOR INSERT
    WITH CHECK (
        company_worker_id IN (
            SELECT id FROM company_workers WHERE company_id = current_setting('app.current_company_id', true)::UUID
        )
    );

-- Policy: Users can update locations for workers in their company
CREATE POLICY worker_locations_isolation_update ON worker_locations
    FOR UPDATE
    USING (
        company_worker_id IN (
            SELECT id FROM company_workers WHERE company_id = current_setting('app.current_company_id', true)::UUID
        )
    );

-- ═══════════════════════════════════════════════════════════════════
-- HELPER FUNCTIONS FOR RLS
-- ═══════════════════════════════════════════════════════════════════

-- Function to set the current company context
CREATE OR REPLACE FUNCTION set_current_company(company_uuid UUID)
RETURNS VOID AS $$
BEGIN
    PERFORM set_config('app.current_company_id', company_uuid::TEXT, false);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON FUNCTION set_current_company IS 'Set the current company context for row-level security. Must be called at the start of each request/transaction.';

-- Function to get the current company context
CREATE OR REPLACE FUNCTION get_current_company()
RETURNS UUID AS $$
BEGIN
    RETURN current_setting('app.current_company_id', true)::UUID;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NULL;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_current_company IS 'Get the current company context UUID. Returns NULL if not set.';

-- ═══════════════════════════════════════════════════════════════════
-- NOTES FOR IMPLEMENTATION
-- ═══════════════════════════════════════════════════════════════════

/*
RLS Implementation Guidelines:

1. APPLICATION SETUP:
   At the start of each request/transaction, set the company context:

   SELECT set_current_company('company-uuid-here');

2. USER AUTHENTICATION:
   - Extract company_id from authenticated user's JWT token or session
   - Set company context immediately after authentication
   - All subsequent queries will be automatically filtered

3. PERFORMANCE CONSIDERATIONS:
   - RLS policies use indexes on company_id columns
   - Ensure app.current_company_id is set for every query
   - Use connection pooling with proper session initialization

4. SECURITY VALIDATION:
   - Test that users cannot access data from other companies
   - Verify that company context is always set before queries
   - Audit RLS bypass attempts via logging

5. TROUBLESHOOTING:
   - If queries return no results, check: SELECT get_current_company();
   - Verify company context is set: SHOW app.current_company_id;
   - Test policies with: SET app.current_company_id = 'uuid';

6. SUPER USER ACCESS (Admin):
   - Super users can bypass RLS with: SET row_security = off;
   - Use with extreme caution and audit logging
   - Re-enable with: SET row_security = on;
*/
