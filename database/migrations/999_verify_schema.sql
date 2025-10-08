-- ═══════════════════════════════════════════════════════════════════
-- Verification Script: 999_verify_schema.sql
-- Description: Verify all tables, indexes, and RLS policies are created
-- Author: HazardHawk Crew Management System
-- Created: 2025-10-08
-- ═══════════════════════════════════════════════════════════════════

-- Usage:
--   psql -U postgres -d hazardhawk_db -f 999_verify_schema.sql

\echo '═══════════════════════════════════════════════════════════════════'
\echo 'HazardHawk Schema Verification'
\echo '═══════════════════════════════════════════════════════════════════'
\echo ''

-- ═══════════════════════════════════════════════════════════════════
-- 1. Verify all tables exist
-- ═══════════════════════════════════════════════════════════════════

\echo '1. Checking tables...'
\echo ''

SELECT
    table_name,
    CASE
        WHEN table_name IN (
            'companies',
            'worker_profiles',
            'company_workers',
            'certification_types',
            'worker_certifications',
            'projects',
            'crews',
            'crew_members',
            'crew_member_history',
            'magic_link_tokens',
            'onboarding_sessions',
            'worker_locations'
        ) THEN '✓'
        ELSE '✗'
    END AS status
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
  AND table_name IN (
    'companies',
    'worker_profiles',
    'company_workers',
    'certification_types',
    'worker_certifications',
    'projects',
    'crews',
    'crew_members',
    'crew_member_history',
    'magic_link_tokens',
    'onboarding_sessions',
    'worker_locations'
  )
ORDER BY table_name;

\echo ''

-- Check for missing tables
WITH expected_tables AS (
    SELECT unnest(ARRAY[
        'companies',
        'worker_profiles',
        'company_workers',
        'certification_types',
        'worker_certifications',
        'projects',
        'crews',
        'crew_members',
        'crew_member_history',
        'magic_link_tokens',
        'onboarding_sessions',
        'worker_locations'
    ]) AS table_name
),
existing_tables AS (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_type = 'BASE TABLE'
)
SELECT
    et.table_name AS missing_table
FROM expected_tables et
LEFT JOIN existing_tables ext ON et.table_name = ext.table_name
WHERE ext.table_name IS NULL;

-- ═══════════════════════════════════════════════════════════════════
-- 2. Verify Row-Level Security is enabled
-- ═══════════════════════════════════════════════════════════════════

\echo '2. Checking Row-Level Security...'
\echo ''

SELECT
    schemaname,
    tablename,
    CASE
        WHEN rowsecurity = true THEN '✓ Enabled'
        ELSE '✗ Disabled'
    END AS rls_status
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename IN (
    'companies',
    'worker_profiles',
    'company_workers',
    'worker_certifications',
    'projects',
    'crews',
    'crew_members',
    'crew_member_history',
    'magic_link_tokens',
    'onboarding_sessions',
    'worker_locations'
  )
ORDER BY tablename;

\echo ''

-- ═══════════════════════════════════════════════════════════════════
-- 3. Verify RLS policies exist
-- ═══════════════════════════════════════════════════════════════════

\echo '3. Checking RLS policies...'
\echo ''

SELECT
    schemaname,
    tablename,
    policyname,
    cmd AS command_type,
    CASE
        WHEN qual IS NOT NULL THEN '✓'
        ELSE '✗'
    END AS has_using_clause,
    CASE
        WHEN with_check IS NOT NULL THEN '✓'
        ELSE '✗'
    END AS has_check_clause
FROM pg_policies
WHERE schemaname = 'public'
ORDER BY tablename, policyname;

\echo ''

-- Count policies per table
SELECT
    tablename,
    COUNT(*) AS policy_count
FROM pg_policies
WHERE schemaname = 'public'
GROUP BY tablename
ORDER BY tablename;

\echo ''

-- ═══════════════════════════════════════════════════════════════════
-- 4. Verify indexes exist
-- ═══════════════════════════════════════════════════════════════════

\echo '4. Checking indexes...'
\echo ''

SELECT
    schemaname,
    tablename,
    COUNT(*) AS index_count
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN (
    'companies',
    'worker_profiles',
    'company_workers',
    'certification_types',
    'worker_certifications',
    'projects',
    'crews',
    'crew_members',
    'crew_member_history',
    'magic_link_tokens',
    'onboarding_sessions',
    'worker_locations'
  )
GROUP BY schemaname, tablename
ORDER BY tablename;

\echo ''

-- ═══════════════════════════════════════════════════════════════════
-- 5. Verify foreign key constraints
-- ═══════════════════════════════════════════════════════════════════

\echo '5. Checking foreign key constraints...'
\echo ''

SELECT
    tc.table_name,
    tc.constraint_name,
    tc.constraint_type,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_schema = 'public'
  AND tc.table_name IN (
    'company_workers',
    'worker_certifications',
    'projects',
    'crews',
    'crew_members',
    'crew_member_history',
    'magic_link_tokens',
    'onboarding_sessions',
    'worker_locations'
  )
ORDER BY tc.table_name, tc.constraint_name;

\echo ''

-- ═══════════════════════════════════════════════════════════════════
-- 6. Verify triggers exist
-- ═══════════════════════════════════════════════════════════════════

\echo '6. Checking triggers...'
\echo ''

SELECT
    event_object_table AS table_name,
    trigger_name,
    event_manipulation AS event,
    action_timing AS timing,
    action_orientation AS orientation
FROM information_schema.triggers
WHERE trigger_schema = 'public'
  AND event_object_table IN (
    'companies',
    'worker_profiles',
    'company_workers',
    'worker_certifications',
    'projects',
    'crews',
    'crew_members',
    'onboarding_sessions'
  )
ORDER BY table_name, trigger_name;

\echo ''

-- ═══════════════════════════════════════════════════════════════════
-- 7. Verify views exist
-- ═══════════════════════════════════════════════════════════════════

\echo '7. Checking views...'
\echo ''

SELECT
    table_name AS view_name,
    CASE
        WHEN table_name IN (
            'active_projects_view',
            'crew_rosters_view',
            'current_worker_locations_view',
            'preshift_attendance_view',
            'pending_onboarding_approvals_view',
            'abandoned_onboarding_sessions_view'
        ) THEN '✓'
        ELSE '✗'
    END AS status
FROM information_schema.views
WHERE table_schema = 'public'
  AND table_name IN (
    'active_projects_view',
    'crew_rosters_view',
    'current_worker_locations_view',
    'preshift_attendance_view',
    'pending_onboarding_approvals_view',
    'abandoned_onboarding_sessions_view'
  )
ORDER BY table_name;

\echo ''

-- ═══════════════════════════════════════════════════════════════════
-- 8. Verify functions exist
-- ═══════════════════════════════════════════════════════════════════

\echo '8. Checking functions...'
\echo ''

SELECT
    routine_name AS function_name,
    routine_type,
    data_type AS return_type
FROM information_schema.routines
WHERE routine_schema = 'public'
  AND routine_name IN (
    'update_updated_at_column',
    'auto_expire_certifications',
    'log_crew_member_change',
    'cleanup_expired_tokens',
    'clear_onboarding_sensitive_data',
    'auto_checkout_stale_locations',
    'get_workers_at_location',
    'set_current_company',
    'get_current_company'
  )
ORDER BY routine_name;

\echo ''

-- ═══════════════════════════════════════════════════════════════════
-- 9. Verify certification types seed data
-- ═══════════════════════════════════════════════════════════════════

\echo '9. Checking seed data (certification types)...'
\echo ''

SELECT
    code,
    name,
    category,
    region
FROM certification_types
ORDER BY code;

\echo ''

SELECT
    'Total certification types: ' || COUNT(*) AS summary
FROM certification_types;

\echo ''

-- ═══════════════════════════════════════════════════════════════════
-- 10. Database statistics
-- ═══════════════════════════════════════════════════════════════════

\echo '10. Database statistics...'
\echo ''

SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) AS index_size
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename IN (
    'companies',
    'worker_profiles',
    'company_workers',
    'certification_types',
    'worker_certifications',
    'projects',
    'crews',
    'crew_members',
    'crew_member_history',
    'magic_link_tokens',
    'onboarding_sessions',
    'worker_locations'
  )
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

\echo ''

-- ═══════════════════════════════════════════════════════════════════
-- Summary
-- ═══════════════════════════════════════════════════════════════════

\echo '═══════════════════════════════════════════════════════════════════'
\echo 'Verification Summary'
\echo '═══════════════════════════════════════════════════════════════════'

SELECT
    'Tables' AS component,
    COUNT(*) AS count
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
  AND table_name IN (
    'companies', 'worker_profiles', 'company_workers',
    'certification_types', 'worker_certifications',
    'projects', 'crews', 'crew_members', 'crew_member_history',
    'magic_link_tokens', 'onboarding_sessions', 'worker_locations'
  )
UNION ALL
SELECT
    'Views' AS component,
    COUNT(*) AS count
FROM information_schema.views
WHERE table_schema = 'public'
UNION ALL
SELECT
    'Indexes' AS component,
    COUNT(*) AS count
FROM pg_indexes
WHERE schemaname = 'public'
UNION ALL
SELECT
    'Foreign Keys' AS component,
    COUNT(*) AS count
FROM information_schema.table_constraints
WHERE constraint_type = 'FOREIGN KEY'
  AND table_schema = 'public'
UNION ALL
SELECT
    'RLS Policies' AS component,
    COUNT(*) AS count
FROM pg_policies
WHERE schemaname = 'public'
UNION ALL
SELECT
    'Triggers' AS component,
    COUNT(*) AS count
FROM information_schema.triggers
WHERE trigger_schema = 'public'
UNION ALL
SELECT
    'Functions' AS component,
    COUNT(*) AS count
FROM information_schema.routines
WHERE routine_schema = 'public'
  AND routine_type = 'FUNCTION';

\echo ''
\echo '═══════════════════════════════════════════════════════════════════'
\echo 'Verification complete!'
\echo '═══════════════════════════════════════════════════════════════════'
