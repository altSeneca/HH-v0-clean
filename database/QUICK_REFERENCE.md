# HazardHawk Crew Management - Quick Reference

## Essential SQL Commands

### Setting Company Context (Required for RLS)

```sql
-- Set company context at start of each request/transaction
SELECT set_current_company('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');

-- Verify company context
SELECT get_current_company();

-- Check current setting
SHOW app.current_company_id;
```

### Common Queries

#### Workers

```sql
-- List all active workers for current company
SELECT
    cw.id,
    cw.employee_number,
    wp.first_name,
    wp.last_name,
    cw.role,
    cw.hire_date
FROM company_workers cw
JOIN worker_profiles wp ON cw.worker_profile_id = wp.id
WHERE cw.status = 'active'
ORDER BY wp.last_name, wp.first_name;

-- Get worker with certifications
SELECT
    cw.id,
    wp.first_name || ' ' || wp.last_name AS name,
    cw.employee_number,
    cw.role,
    jsonb_agg(
        jsonb_build_object(
            'cert_type', ct.name,
            'status', wc.status,
            'expires', wc.expiration_date
        )
    ) FILTER (WHERE wc.id IS NOT NULL) AS certifications
FROM company_workers cw
JOIN worker_profiles wp ON cw.worker_profile_id = wp.id
LEFT JOIN worker_certifications wc ON wp.id = wc.worker_profile_id AND wc.status = 'verified'
LEFT JOIN certification_types ct ON wc.certification_type_id = ct.id
WHERE cw.id = 'worker-uuid'
GROUP BY cw.id, wp.first_name, wp.last_name, cw.employee_number, cw.role;

-- Search workers by name
SELECT
    cw.id,
    wp.first_name,
    wp.last_name,
    cw.employee_number,
    cw.role
FROM company_workers cw
JOIN worker_profiles wp ON cw.worker_profile_id = wp.id
WHERE to_tsvector('english', wp.first_name || ' ' || wp.last_name) @@ plainto_tsquery('english', 'john doe')
  AND cw.status = 'active';
```

#### Projects

```sql
-- List active projects with manager
SELECT * FROM active_projects_view
ORDER BY start_date DESC;

-- Get project with centralized info
SELECT
    p.name,
    p.project_number,
    p.client_name,
    p.street_address || ', ' || p.city || ', ' || p.state || ' ' || p.zip AS full_address,
    p.general_contractor,
    pm.first_name || ' ' || pm.last_name AS project_manager,
    super.first_name || ' ' || super.last_name AS superintendent
FROM projects p
LEFT JOIN company_workers pm_cw ON p.project_manager_id = pm_cw.id
LEFT JOIN worker_profiles pm ON pm_cw.worker_profile_id = pm.id
LEFT JOIN company_workers super_cw ON p.superintendent_id = super_cw.id
LEFT JOIN worker_profiles super ON super_cw.worker_profile_id = super.id
WHERE p.id = 'project-uuid';
```

#### Crews

```sql
-- Get crew roster with all member details
SELECT * FROM crew_rosters_view
WHERE crew_id = 'crew-uuid'
ORDER BY member_role, member_name;

-- List crews by project
SELECT
    c.id,
    c.name,
    c.crew_type,
    c.trade,
    c.location,
    COUNT(cm.id) AS member_count,
    foreman.first_name || ' ' || foreman.last_name AS foreman_name
FROM crews c
LEFT JOIN crew_members cm ON c.id = cm.crew_id AND cm.status = 'active'
LEFT JOIN company_workers foreman_cw ON c.foreman_id = foreman_cw.id
LEFT JOIN worker_profiles foreman ON foreman_cw.worker_profile_id = foreman.id
WHERE c.project_id = 'project-uuid'
  AND c.status = 'active'
GROUP BY c.id, c.name, c.crew_type, c.trade, c.location, foreman.first_name, foreman.last_name
ORDER BY c.name;

-- Get crew members eligible to be foreman
SELECT
    cm.id,
    cw.id AS worker_id,
    wp.first_name || ' ' || wp.last_name AS name,
    cw.role,
    CASE
        WHEN cw.role IN ('foreman', 'superintendent', 'project_manager') THEN true
        ELSE false
    END AS can_be_foreman
FROM crew_members cm
JOIN company_workers cw ON cm.company_worker_id = cw.id
JOIN worker_profiles wp ON cw.worker_profile_id = wp.id
WHERE cm.crew_id = 'crew-uuid'
  AND cm.status = 'active'
ORDER BY can_be_foreman DESC, wp.last_name;
```

#### Certifications

```sql
-- Get expiring certifications (next 30 days)
SELECT
    wp.first_name,
    wp.last_name,
    cw.employee_number,
    ct.name AS certification,
    wc.expiration_date,
    wc.expiration_date - CURRENT_DATE AS days_until_expiration
FROM worker_certifications wc
JOIN worker_profiles wp ON wc.worker_profile_id = wp.id
JOIN company_workers cw ON wp.id = cw.worker_profile_id
JOIN certification_types ct ON wc.certification_type_id = ct.id
WHERE wc.status = 'verified'
  AND wc.expiration_date IS NOT NULL
  AND wc.expiration_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '30 days'
ORDER BY wc.expiration_date;

-- Get pending certification approvals
SELECT
    wp.first_name,
    wp.last_name,
    ct.name AS certification,
    wc.issue_date,
    wc.created_at,
    EXTRACT(EPOCH FROM (NOW() - wc.created_at))/3600 AS hours_pending
FROM worker_certifications wc
JOIN worker_profiles wp ON wc.worker_profile_id = wp.id
JOIN certification_types ct ON wc.certification_type_id = ct.id
WHERE wc.status = 'pending_verification'
ORDER BY wc.created_at;
```

#### Location Tracking

```sql
-- Get workers currently at location
SELECT * FROM get_workers_at_location('Floor 3', 'project-uuid', 4);

-- Today's pre-shift attendance by location
SELECT * FROM preshift_attendance_view
WHERE attendance_date = CURRENT_DATE
ORDER BY location_identifier;

-- Currently checked-in workers
SELECT * FROM current_worker_locations_view
ORDER BY checked_in_at;
```

#### Onboarding

```sql
-- Pending onboarding approvals
SELECT * FROM pending_onboarding_approvals_view
ORDER BY created_at;

-- Abandoned onboarding sessions (inactive 24+ hours)
SELECT * FROM abandoned_onboarding_sessions_view
ORDER BY hours_inactive DESC;

-- Active magic links
SELECT
    email,
    phone,
    expires_at,
    EXTRACT(EPOCH FROM (expires_at - NOW()))/3600 AS hours_until_expiration
FROM magic_link_tokens
WHERE NOT used
  AND expires_at > NOW()
ORDER BY expires_at;
```

### Data Modification

#### Create Worker

```sql
-- Step 1: Create worker profile
INSERT INTO worker_profiles (first_name, last_name, email, phone, date_of_birth)
VALUES ('John', 'Doe', 'john.doe@example.com', '+1-555-0100', '1990-01-15')
RETURNING id;

-- Step 2: Associate with company
INSERT INTO company_workers (
    company_id,
    worker_profile_id,
    employee_number,
    role,
    hire_date,
    status
)
VALUES (
    current_setting('app.current_company_id')::UUID,
    'worker-profile-uuid-from-step-1',
    'E-2024-001',
    'laborer',
    '2025-10-08',
    'active'
)
RETURNING id;
```

#### Create Crew

```sql
INSERT INTO crews (
    company_id,
    project_id,
    name,
    crew_type,
    trade,
    foreman_id,
    location,
    status
)
VALUES (
    current_setting('app.current_company_id')::UUID,
    'project-uuid',
    'Concrete Crew #1',
    'project_based',
    'concrete',
    'foreman-worker-uuid',
    'Floor 3',
    'active'
)
RETURNING id;
```

#### Assign Worker to Crew

```sql
INSERT INTO crew_members (
    crew_id,
    company_worker_id,
    role,
    start_date,
    status
)
VALUES (
    'crew-uuid',
    'worker-uuid',
    'member',
    CURRENT_DATE,
    'active'
);
```

#### Add Certification

```sql
INSERT INTO worker_certifications (
    worker_profile_id,
    company_id,
    certification_type_id,
    certification_number,
    issue_date,
    expiration_date,
    issuing_authority,
    document_url,
    status
)
VALUES (
    'worker-profile-uuid',
    current_setting('app.current_company_id')::UUID,
    (SELECT id FROM certification_types WHERE code = 'OSHA_10'),
    'OSHA10-123456',
    '2024-06-15',
    NULL, -- OSHA 10 doesn't expire
    'OSHA Training Institute',
    's3://bucket/certifications/osha10-123456.pdf',
    'pending_verification'
);
```

### Maintenance

#### Auto-checkout Stale Locations

```sql
SELECT auto_checkout_stale_locations();
```

#### Cleanup Expired Tokens

```sql
DELETE FROM magic_link_tokens
WHERE expires_at < NOW() - INTERVAL '7 days'
  AND used = FALSE;
```

#### Archive Old History

```sql
-- Archive crew member history older than 1 year
CREATE TABLE crew_member_history_archive AS
SELECT * FROM crew_member_history
WHERE created_at < NOW() - INTERVAL '1 year';

DELETE FROM crew_member_history
WHERE created_at < NOW() - INTERVAL '1 year';
```

## Useful Queries for Development

### Test RLS Isolation

```sql
-- Create test companies
INSERT INTO companies (id, name, subdomain, tier)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Company A', 'company-a', 'professional'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Company B', 'company-b', 'professional');

-- Set context to Company A
SELECT set_current_company('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');
SELECT * FROM company_workers; -- Should only see Company A workers

-- Set context to Company B
SELECT set_current_company('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb');
SELECT * FROM company_workers; -- Should only see Company B workers
```

### Performance Testing

```sql
-- Check query execution time
EXPLAIN ANALYZE
SELECT * FROM company_workers
WHERE status = 'active';

-- Check index usage
SELECT
    relname AS table_name,
    indexrelname AS index_name,
    idx_scan AS times_used,
    idx_tup_read AS tuples_read,
    idx_tup_fetch AS tuples_fetched
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
```

## Environment Variables

```bash
# PostgreSQL connection
DATABASE_URL=postgresql://user:password@localhost:5432/hazardhawk_db

# For connection pooling
DB_POOL_SIZE=20
DB_IDLE_TIMEOUT=30000
DB_CONNECTION_TIMEOUT=5000
```

## Common Issues

### Issue: No results from queries

**Solution:** Ensure company context is set:
```sql
SELECT set_current_company('your-company-uuid');
```

### Issue: Foreign key constraint violation

**Solution:** Ensure referenced records exist and belong to current company:
```sql
-- Check if worker exists in current company
SELECT id FROM company_workers WHERE id = 'worker-uuid';
```

### Issue: RLS policy violation

**Solution:** Verify user has permission and company context is correct:
```sql
SELECT get_current_company();
SHOW row_security;
```

## Additional Resources

- Full schema documentation: `/database/README.md`
- Implementation plan: `/docs/implementation/crew-management-implementation-plan.md`
- Verification script: `/database/migrations/999_verify_schema.sql`
