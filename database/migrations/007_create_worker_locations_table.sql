-- ═══════════════════════════════════════════════════════════════════
-- Migration: 007_create_worker_locations_table.sql
-- Description: Create worker locations table for pre-shift meeting tracking
-- Author: HazardHawk Crew Management System
-- Created: 2025-10-08
-- ═══════════════════════════════════════════════════════════════════

-- Worker Locations (Optional - for Pre-Shift Meetings and location tracking)
CREATE TABLE IF NOT EXISTS worker_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_worker_id UUID NOT NULL REFERENCES company_workers(id) ON DELETE CASCADE,
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    location_type VARCHAR(50), -- 'floor', 'building', 'zone', 'area'
    location_identifier VARCHAR(100), -- 'Floor 3', 'Building A', 'East Wing'
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    checked_in_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    checked_out_at TIMESTAMPTZ,
    metadata JSONB DEFAULT '{}' -- Additional context (check-in method, device info, etc.)
);

-- Add comments for documentation
COMMENT ON TABLE worker_locations IS 'Worker location check-in/check-out tracking for pre-shift meetings and attendance monitoring. Optional feature for site tracking.';
COMMENT ON COLUMN worker_locations.location_type IS 'Type of location: floor (vertical level), building (structure), zone (area designation), area (general region)';
COMMENT ON COLUMN worker_locations.location_identifier IS 'Human-readable location identifier (e.g., "Floor 3", "Building A", "East Wing")';
COMMENT ON COLUMN worker_locations.latitude IS 'GPS latitude coordinate (if available)';
COMMENT ON COLUMN worker_locations.longitude IS 'GPS longitude coordinate (if available)';
COMMENT ON COLUMN worker_locations.checked_in_at IS 'Timestamp when worker checked in to location';
COMMENT ON COLUMN worker_locations.checked_out_at IS 'Timestamp when worker checked out (NULL if still at location)';
COMMENT ON COLUMN worker_locations.metadata IS 'Additional metadata (check-in method: QR code, GPS, manual; device info, etc.)';

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_worker_locations_worker ON worker_locations(company_worker_id, checked_in_at DESC);
CREATE INDEX IF NOT EXISTS idx_worker_locations_project ON worker_locations(project_id, checked_in_at DESC) WHERE project_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_worker_locations_time ON worker_locations(checked_in_at, checked_out_at) WHERE checked_out_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_worker_locations_active ON worker_locations(company_worker_id, project_id, checked_in_at DESC) WHERE checked_out_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_worker_locations_location ON worker_locations(location_identifier, checked_in_at DESC);

-- Create spatial index for GPS coordinates (requires PostGIS extension)
-- Uncomment if PostGIS is available
-- CREATE INDEX IF NOT EXISTS idx_worker_locations_gis ON worker_locations
--     USING GIST (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326))
--     WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- Add constraint checks
ALTER TABLE worker_locations ADD CONSTRAINT check_location_type
    CHECK (location_type IS NULL OR location_type IN ('floor', 'building', 'zone', 'area', 'other'));

ALTER TABLE worker_locations ADD CONSTRAINT check_location_times
    CHECK (checked_in_at <= COALESCE(checked_out_at, checked_in_at));

ALTER TABLE worker_locations ADD CONSTRAINT check_coordinates
    CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR (latitude IS NOT NULL AND longitude IS NOT NULL)
    );

ALTER TABLE worker_locations ADD CONSTRAINT check_latitude_range
    CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90));

ALTER TABLE worker_locations ADD CONSTRAINT check_longitude_range
    CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180));

-- Create view for currently checked-in workers by location
CREATE OR REPLACE VIEW current_worker_locations_view AS
SELECT
    wl.id,
    wl.company_worker_id,
    wl.project_id,
    wl.location_type,
    wl.location_identifier,
    wl.latitude,
    wl.longitude,
    wl.checked_in_at,
    wp.first_name,
    wp.last_name,
    wp.first_name || ' ' || wp.last_name AS worker_name,
    cw.employee_number,
    cw.role AS worker_role,
    p.name AS project_name,
    p.street_address AS project_address,
    EXTRACT(EPOCH FROM (NOW() - wl.checked_in_at))/3600 AS hours_at_location
FROM worker_locations wl
JOIN company_workers cw ON wl.company_worker_id = cw.id
JOIN worker_profiles wp ON cw.worker_profile_id = wp.id
LEFT JOIN projects p ON wl.project_id = p.id
WHERE wl.checked_out_at IS NULL
  AND cw.status = 'active'
ORDER BY wl.checked_in_at DESC;

COMMENT ON VIEW current_worker_locations_view IS 'View showing all currently checked-in workers with denormalized worker and project details';

-- Create view for pre-shift meeting attendance
CREATE OR REPLACE VIEW preshift_attendance_view AS
SELECT
    wl.location_identifier,
    wl.project_id,
    p.name AS project_name,
    DATE(wl.checked_in_at) AS attendance_date,
    COUNT(DISTINCT wl.company_worker_id) AS worker_count,
    jsonb_agg(
        jsonb_build_object(
            'worker_id', cw.id,
            'worker_name', wp.first_name || ' ' || wp.last_name,
            'employee_number', cw.employee_number,
            'role', cw.role,
            'checked_in_at', wl.checked_in_at
        ) ORDER BY wl.checked_in_at
    ) AS workers
FROM worker_locations wl
JOIN company_workers cw ON wl.company_worker_id = cw.id
JOIN worker_profiles wp ON cw.worker_profile_id = wp.id
LEFT JOIN projects p ON wl.project_id = p.id
WHERE wl.checked_in_at >= CURRENT_DATE
  AND cw.status = 'active'
GROUP BY wl.location_identifier, wl.project_id, p.name, DATE(wl.checked_in_at)
ORDER BY attendance_date DESC, wl.location_identifier;

COMMENT ON VIEW preshift_attendance_view IS 'Daily pre-shift meeting attendance grouped by location with aggregated worker lists';

-- Create function to auto-checkout workers after 12 hours (safety feature)
CREATE OR REPLACE FUNCTION auto_checkout_stale_locations()
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
BEGIN
    -- Auto-checkout workers who have been checked in for more than 12 hours
    UPDATE worker_locations
    SET
        checked_out_at = checked_in_at + INTERVAL '12 hours',
        metadata = metadata || jsonb_build_object(
            'auto_checkout', true,
            'reason', 'Automatic checkout after 12 hours',
            'checkout_at', NOW()
        )
    WHERE checked_out_at IS NULL
      AND checked_in_at < NOW() - INTERVAL '12 hours';

    GET DIAGNOSTICS updated_count = ROW_COUNT;

    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION auto_checkout_stale_locations IS 'Auto-checkout workers checked in for more than 12 hours. Should be called by scheduler (pg_cron or external job queue).';

-- Create function to get workers at a specific location
CREATE OR REPLACE FUNCTION get_workers_at_location(
    p_location_identifier VARCHAR,
    p_project_id UUID DEFAULT NULL,
    p_hours_window INTEGER DEFAULT 4
)
RETURNS TABLE (
    worker_id UUID,
    worker_name TEXT,
    employee_number VARCHAR,
    role VARCHAR,
    checked_in_at TIMESTAMPTZ,
    hours_at_location NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        cw.id AS worker_id,
        wp.first_name || ' ' || wp.last_name AS worker_name,
        cw.employee_number,
        cw.role,
        wl.checked_in_at,
        EXTRACT(EPOCH FROM (NOW() - wl.checked_in_at))/3600 AS hours_at_location
    FROM worker_locations wl
    JOIN company_workers cw ON wl.company_worker_id = cw.id
    JOIN worker_profiles wp ON cw.worker_profile_id = wp.id
    WHERE wl.location_identifier = p_location_identifier
      AND (p_project_id IS NULL OR wl.project_id = p_project_id)
      AND wl.checked_in_at >= NOW() - (p_hours_window || ' hours')::INTERVAL
      AND wl.checked_out_at IS NULL
      AND cw.status = 'active'
    ORDER BY wl.checked_in_at ASC;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_workers_at_location IS 'Get all active workers at a specific location within a time window (default 4 hours). Used for pre-shift meeting attendance.';
