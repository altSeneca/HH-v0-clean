-- ═══════════════════════════════════════════════════════════════════
-- Master Migration Script: 000_run_all_migrations.sql
-- Description: Execute all crew management database migrations in order
-- Author: HazardHawk Crew Management System
-- Created: 2025-10-08
-- ═══════════════════════════════════════════════════════════════════

-- This script runs all migrations in the correct order.
-- It's safe to run multiple times - migrations use IF NOT EXISTS checks.

-- Usage:
--   psql -U postgres -d hazardhawk_db -f 000_run_all_migrations.sql

-- Or from application:
--   const migrations = fs.readdirSync('./migrations').filter(f => f !== '000_run_all_migrations.sql').sort();
--   for (const migration of migrations) {
--     await db.query(fs.readFileSync(`./migrations/${migration}`, 'utf8'));
--   }

\echo '═══════════════════════════════════════════════════════════════════'
\echo 'HazardHawk Crew Management System - Database Migrations'
\echo '═══════════════════════════════════════════════════════════════════'
\echo ''

-- Start transaction for atomic execution
BEGIN;

\echo 'Running migration 001: Create companies table...'
\i 001_create_companies_table.sql
\echo '✓ Migration 001 complete'
\echo ''

\echo 'Running migration 002: Create worker tables...'
\i 002_create_worker_tables.sql
\echo '✓ Migration 002 complete'
\echo ''

\echo 'Running migration 003: Create certification tables...'
\i 003_create_certification_tables.sql
\echo '✓ Migration 003 complete'
\echo ''

\echo 'Running migration 004: Create projects table...'
\i 004_create_projects_table.sql
\echo '✓ Migration 004 complete'
\echo ''

\echo 'Running migration 005: Create crew tables...'
\i 005_create_crew_tables.sql
\echo '✓ Migration 005 complete'
\echo ''

\echo 'Running migration 006: Create onboarding tables...'
\i 006_create_onboarding_tables.sql
\echo '✓ Migration 006 complete'
\echo ''

\echo 'Running migration 007: Create worker locations table...'
\i 007_create_worker_locations_table.sql
\echo '✓ Migration 007 complete'
\echo ''

\echo 'Running migration 008: Enable row-level security...'
\i 008_enable_row_level_security.sql
\echo '✓ Migration 008 complete'
\echo ''

-- Commit transaction
COMMIT;

\echo '═══════════════════════════════════════════════════════════════════'
\echo 'All migrations completed successfully!'
\echo '═══════════════════════════════════════════════════════════════════'
\echo ''
\echo 'Next steps:'
\echo '1. Verify tables: \dt'
\echo '2. Check RLS policies: \d+ companies'
\echo '3. Test company context: SELECT set_current_company(''uuid-here'');'
\echo '4. Run seed data script (if needed)'
\echo ''

-- Display migration summary
\echo 'Migration Summary:'
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename IN (
    'companies', 'worker_profiles', 'company_workers',
    'certification_types', 'worker_certifications',
    'projects', 'crews', 'crew_members', 'crew_member_history',
    'magic_link_tokens', 'onboarding_sessions', 'worker_locations'
  )
ORDER BY tablename;

\echo ''
\echo 'Indexes Created:'
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN (
    'companies', 'worker_profiles', 'company_workers',
    'certification_types', 'worker_certifications',
    'projects', 'crews', 'crew_members', 'crew_member_history',
    'magic_link_tokens', 'onboarding_sessions', 'worker_locations'
  )
ORDER BY tablename, indexname;
