# HazardHawk Crew Management System - Database Schema

This directory contains PostgreSQL migration scripts for the HazardHawk crew management system. The schema implements a multi-tenant architecture with row-level security (RLS) for data isolation.

## Overview

The crew management system provides:
- Multi-tenant company management with centralized info
- Worker profiles and company associations (supporting subcontractors)
- Certification tracking with OCR and expiration alerts
- Project management with centralized client and location data
- Dynamic crew assignment and roster generation
- Magic link passwordless onboarding
- Worker location tracking for pre-shift meetings

## Schema Architecture

```
companies (tenants)
    ├── company_workers (many-to-many)
    │   └── worker_profiles (global)
    │       └── worker_certifications
    ├── projects
    │   └── crews
    │       └── crew_members
    │           └── crew_member_history (audit)
    ├── magic_link_tokens (onboarding)
    └── onboarding_sessions (multi-step)
```

## Migration Files

| File | Description | Tables Created |
|------|-------------|----------------|
| `001_create_companies_table.sql` | Company/tenant table with centralized info | `companies` |
| `002_create_worker_tables.sql` | Worker profiles and associations | `worker_profiles`, `company_workers` |
| `003_create_certification_tables.sql` | Certification types and worker certs | `certification_types`, `worker_certifications` |
| `004_create_projects_table.sql` | Projects with centralized client data | `projects` |
| `005_create_crew_tables.sql` | Crews and crew member assignments | `crews`, `crew_members`, `crew_member_history` |
| `006_create_onboarding_tables.sql` | Magic links and onboarding sessions | `magic_link_tokens`, `onboarding_sessions` |
| `007_create_worker_locations_table.sql` | Worker location tracking | `worker_locations` |
| `008_enable_row_level_security.sql` | RLS policies for multi-tenancy | N/A (policies only) |

## Running Migrations

### Option 1: Run all migrations at once

```bash
psql -U postgres -d hazardhawk_db -f migrations/000_run_all_migrations.sql
```

### Option 2: Run migrations individually

```bash
cd migrations
psql -U postgres -d hazardhawk_db -f 001_create_companies_table.sql
psql -U postgres -d hazardhawk_db -f 002_create_worker_tables.sql
# ... and so on
```

### Option 3: Run from application code

**Node.js / TypeScript:**
```typescript
import { readFileSync } from 'fs';
import { Pool } from 'pg';

const pool = new Pool({ connectionString: process.env.DATABASE_URL });

async function runMigrations() {
  const migrations = [
    '001_create_companies_table.sql',
    '002_create_worker_tables.sql',
    '003_create_certification_tables.sql',
    '004_create_projects_table.sql',
    '005_create_crew_tables.sql',
    '006_create_onboarding_tables.sql',
    '007_create_worker_locations_table.sql',
    '008_enable_row_level_security.sql',
  ];

  for (const migration of migrations) {
    console.log(`Running ${migration}...`);
    const sql = readFileSync(`./migrations/${migration}`, 'utf8');
    await pool.query(sql);
  }

  console.log('All migrations completed!');
}
```

**Go:**
```go
package main

import (
    "database/sql"
    "fmt"
    "io/ioutil"
    _ "github.com/lib/pq"
)

func runMigrations(db *sql.DB) error {
    migrations := []string{
        "001_create_companies_table.sql",
        "002_create_worker_tables.sql",
        "003_create_certification_tables.sql",
        "004_create_projects_table.sql",
        "005_create_crew_tables.sql",
        "006_create_onboarding_tables.sql",
        "007_create_worker_locations_table.sql",
        "008_enable_row_level_security.sql",
    }

    for _, migration := range migrations {
        fmt.Printf("Running %s...\n", migration)
        sql, err := ioutil.ReadFile(fmt.Sprintf("migrations/%s", migration))
        if err != nil {
            return err
        }
        _, err = db.Exec(string(sql))
        if err != nil {
            return err
        }
    }

    fmt.Println("All migrations completed!")
    return nil
}
```

## Row-Level Security (RLS)

All tables have RLS enabled for multi-tenant data isolation. **You must set the company context** before running queries:

```sql
-- Set company context (required for every request)
SELECT set_current_company('company-uuid-here');

-- Now all queries are automatically filtered by company
SELECT * FROM workers; -- Only returns workers for current company
```

### Application Integration

**PostgreSQL connection setup:**
```typescript
// After authenticating user, set company context
await pool.query('SELECT set_current_company($1)', [user.companyId]);

// All subsequent queries in this transaction are filtered
const workers = await pool.query('SELECT * FROM company_workers WHERE status = $1', ['active']);
```

**JWT-based approach:**
```typescript
// Extract company_id from JWT token
const decoded = jwt.verify(token, SECRET);
const companyId = decoded.company_id;

// Set context for this database session
await pool.query('SELECT set_current_company($1)', [companyId]);
```

### Testing RLS

```sql
-- Test as Company A
SELECT set_current_company('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');
SELECT * FROM company_workers; -- Should return only Company A workers

-- Test as Company B
SELECT set_current_company('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb');
SELECT * FROM company_workers; -- Should return only Company B workers

-- Verify isolation
SELECT get_current_company(); -- Returns current company UUID
```

## Key Features

### 1. Centralized Company Information

Company details (address, phone, logo) are stored once and reused across all documents:

```sql
SELECT * FROM companies WHERE id = 'company-uuid';
-- Returns: name, address, city, state, zip, phone, logo_url

-- This data auto-populates PTPs, reports, and safety documents
```

### 2. Centralized Project Information

Project details (client, location, GC) are stored once and reused:

```sql
SELECT * FROM projects WHERE id = 'project-uuid';
-- Returns: name, client_name, street_address, general_contractor, etc.

-- No duplicate data entry across PTPs, toolbox talks, or reports
```

### 3. Flexible Foreman Selection

Crews have a default foreman, but PTPs can override with any crew member:

```sql
-- Crew has default foreman
SELECT foreman_id FROM crews WHERE id = 'crew-uuid';

-- But PTP can select a different foreman from crew members
INSERT INTO pre_task_plans (crew_id, foreman_id, ...)
VALUES ('crew-uuid', 'different-worker-uuid', ...);
```

### 4. Certification Expiration Tracking

Automated expiration tracking with configurable alerts:

```sql
-- Get expiring certifications
SELECT * FROM worker_certifications
WHERE status = 'verified'
  AND expiration_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '30 days';

-- Background job checks daily and sends alerts at:
-- 90, 60, 30, 14, 7, 3 days before expiration
```

### 5. Worker Location Tracking

Track worker check-ins for pre-shift meetings:

```sql
-- Get workers at a location
SELECT * FROM get_workers_at_location('Floor 3', 'project-uuid', 4);

-- Auto-checkout after 12 hours (safety feature)
SELECT auto_checkout_stale_locations();
```

## Seed Data

Common certification types are automatically inserted:
- OSHA 10 & 30
- Forklift Operator
- CPR/First Aid
- Confined Space Entry
- Fall Protection
- Crane Operator
- And more...

To add more certification types:

```sql
INSERT INTO certification_types (code, name, category, region, typical_duration_months)
VALUES ('CUSTOM_CERT', 'Custom Certification Name', 'safety_training', 'US', 12);
```

## Maintenance Tasks

### Daily Background Jobs

These should be run via cron or background job queue:

```sql
-- 1. Check for expiring certifications (run daily at 2 AM)
-- Implement in application layer to send emails/SMS

-- 2. Auto-checkout stale location check-ins (run hourly)
SELECT auto_checkout_stale_locations();

-- 3. Cleanup expired magic link tokens (run daily)
DELETE FROM magic_link_tokens
WHERE expires_at < NOW() - INTERVAL '7 days' AND used = FALSE;

-- 4. Archive old crew member history (run monthly)
-- Implement based on data retention policy
```

### Performance Monitoring

```sql
-- Check table sizes
SELECT
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
    pg_total_relation_size(schemaname||'.'||tablename) AS bytes
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY bytes DESC;

-- Check slow queries (requires pg_stat_statements extension)
SELECT
    query,
    calls,
    total_time,
    mean_time,
    max_time
FROM pg_stat_statements
WHERE query LIKE '%company_workers%' OR query LIKE '%certifications%'
ORDER BY mean_time DESC
LIMIT 10;

-- Check index usage
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan AS index_scans,
    idx_tup_read AS tuples_read,
    idx_tup_fetch AS tuples_fetched
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
```

## Backup and Restore

```bash
# Full database backup
pg_dump -U postgres -d hazardhawk_db -f backup_$(date +%Y%m%d).sql

# Backup schema only (no data)
pg_dump -U postgres -d hazardhawk_db --schema-only -f schema_backup.sql

# Restore from backup
psql -U postgres -d hazardhawk_db -f backup_20251008.sql

# Backup specific tables
pg_dump -U postgres -d hazardhawk_db -t companies -t company_workers -t worker_profiles > workers_backup.sql
```

## Security Best Practices

1. **Always set company context** before queries
2. **Use parameterized queries** to prevent SQL injection
3. **Hash sensitive data** (SSN, tokens) before storage
4. **Encrypt at rest** for certification documents in S3
5. **Audit log** all RLS policy bypasses (superuser access)
6. **Rotate credentials** regularly
7. **Use connection pooling** with proper session initialization
8. **Monitor failed RLS checks** in application logs

## Troubleshooting

### Problem: Queries return no results

```sql
-- Check if company context is set
SELECT get_current_company(); -- Should return UUID, not NULL

-- Set company context
SELECT set_current_company('your-company-uuid');

-- Verify RLS is enabled
\d+ company_workers -- Look for "Row Security" section
```

### Problem: RLS policy violations

```sql
-- Check current RLS setting
SHOW row_security; -- Should be "on"

-- Temporarily disable RLS (admin only, with caution)
SET row_security = off;
-- Run queries
SET row_security = on;
```

### Problem: Slow queries

```sql
-- Analyze query plan
EXPLAIN ANALYZE
SELECT * FROM company_workers
WHERE company_id = 'uuid-here' AND status = 'active';

-- Check for missing indexes
-- If seq scan appears, consider adding index
CREATE INDEX idx_name ON table_name(column_name);
```

## Support

For questions or issues with the database schema, refer to:
- Implementation plan: `/docs/implementation/crew-management-implementation-plan.md`
- API specification: Lines 617-924 in implementation plan
- Data models: Lines 379-609 in implementation plan

## Version History

- **v1.0.0** (2025-10-08): Initial schema creation
  - Companies, workers, certifications, projects, crews
  - Onboarding with magic links
  - Row-level security for multi-tenancy
  - Centralized company/project information
  - Flexible foreman selection for PTPs
