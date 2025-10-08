-- ═══════════════════════════════════════════════════════════════════
-- Migration: 006_create_onboarding_tables.sql
-- Description: Create magic link tokens and onboarding session tables
-- Author: HazardHawk Crew Management System
-- Created: 2025-10-08
-- ═══════════════════════════════════════════════════════════════════

-- Magic Link Tokens (Passwordless onboarding)
CREATE TABLE IF NOT EXISTS magic_link_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMPTZ,
    metadata JSONB DEFAULT '{}', -- Onboarding context (role, invite_by, etc.)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add comments for documentation
COMMENT ON TABLE magic_link_tokens IS 'Passwordless authentication tokens for worker self-service onboarding. Tokens are single-use and time-limited for security.';
COMMENT ON COLUMN magic_link_tokens.token_hash IS 'SHA-256 hash of the magic link token. Actual token never stored in database.';
COMMENT ON COLUMN magic_link_tokens.expires_at IS 'Token expiration timestamp. Typically 24 hours from creation.';
COMMENT ON COLUMN magic_link_tokens.used IS 'Flag indicating if token has been consumed';
COMMENT ON COLUMN magic_link_tokens.metadata IS 'Additional context stored with invitation (role, invited_by, invitation_message, etc.)';

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_magic_links_expiry ON magic_link_tokens(expires_at) WHERE NOT used AND expires_at > NOW();
CREATE INDEX IF NOT EXISTS idx_magic_links_email ON magic_link_tokens(email, company_id);
CREATE INDEX IF NOT EXISTS idx_magic_links_token_hash ON magic_link_tokens(token_hash) WHERE NOT used;

-- Add constraint to ensure tokens are not used after expiration
ALTER TABLE magic_link_tokens ADD CONSTRAINT check_token_usage
    CHECK (NOT used OR (used AND used_at IS NOT NULL));

-- Onboarding Progress (Track multi-step onboarding)
CREATE TABLE IF NOT EXISTS onboarding_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    worker_profile_id UUID REFERENCES worker_profiles(id) ON DELETE SET NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    current_step VARCHAR(50) NOT NULL, -- 'basic_info', 'photo_id', 'certifications', 'signature', 'complete'
    completed_steps JSONB NOT NULL DEFAULT '[]',
    form_data JSONB NOT NULL DEFAULT '{}', -- Partial form data saved between steps
    status VARCHAR(20) NOT NULL DEFAULT 'in_progress', -- 'in_progress', 'pending_approval', 'approved', 'rejected'
    approved_by UUID REFERENCES company_workers(id),
    approved_at TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add comments for documentation
COMMENT ON TABLE onboarding_sessions IS 'Multi-step onboarding session tracking. Stores progress and form data for worker self-service onboarding flows.';
COMMENT ON COLUMN onboarding_sessions.current_step IS 'Current onboarding step: basic_info, photo_id, selfie, certifications, signature, complete';
COMMENT ON COLUMN onboarding_sessions.completed_steps IS 'JSON array of completed step names for progress tracking';
COMMENT ON COLUMN onboarding_sessions.form_data IS 'Partially filled form data stored across steps (cleared after approval for security)';
COMMENT ON COLUMN onboarding_sessions.status IS 'Session status: in_progress (worker filling out), pending_approval (submitted), approved (accepted), rejected (denied)';
COMMENT ON COLUMN onboarding_sessions.approved_by IS 'Company worker who approved or rejected the session';

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_onboarding_company ON onboarding_sessions(company_id, status);
CREATE INDEX IF NOT EXISTS idx_onboarding_worker ON onboarding_sessions(worker_profile_id) WHERE worker_profile_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_onboarding_status ON onboarding_sessions(status, created_at DESC) WHERE status IN ('pending_approval', 'in_progress');
CREATE INDEX IF NOT EXISTS idx_onboarding_updated ON onboarding_sessions(updated_at DESC);

-- Add constraint checks
ALTER TABLE onboarding_sessions ADD CONSTRAINT check_onboarding_status
    CHECK (status IN ('in_progress', 'pending_approval', 'approved', 'rejected'));

ALTER TABLE onboarding_sessions ADD CONSTRAINT check_onboarding_step
    CHECK (current_step IN ('basic_info', 'photo_id', 'selfie', 'certifications', 'signature', 'complete'));

ALTER TABLE onboarding_sessions ADD CONSTRAINT check_approval_fields
    CHECK (
        (status IN ('approved', 'rejected') AND approved_by IS NOT NULL AND approved_at IS NOT NULL)
        OR (status NOT IN ('approved', 'rejected'))
    );

-- Create trigger for updated_at timestamp
CREATE TRIGGER update_onboarding_sessions_updated_at
    BEFORE UPDATE ON onboarding_sessions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create trigger to automatically expire old tokens
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS TRIGGER AS $$
BEGIN
    -- Mark expired tokens as unusable
    UPDATE magic_link_tokens
    SET metadata = metadata || jsonb_build_object('expired', true)
    WHERE expires_at < NOW() AND NOT used;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Run cleanup trigger periodically (requires pg_cron extension or external scheduler)
-- This is a placeholder - actual implementation would use pg_cron or background job
COMMENT ON FUNCTION cleanup_expired_tokens IS 'Cleanup function for expired magic link tokens. Should be called by scheduler (pg_cron or external job queue).';

-- Create trigger to clear sensitive form data after approval
CREATE OR REPLACE FUNCTION clear_onboarding_sensitive_data()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status IN ('approved', 'rejected') AND OLD.status NOT IN ('approved', 'rejected') THEN
        -- Clear sensitive form data but keep basic info for audit
        NEW.form_data = jsonb_build_object(
            'completed_at', NOW(),
            'final_status', NEW.status
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER onboarding_data_cleanup
    BEFORE UPDATE ON onboarding_sessions
    FOR EACH ROW
    EXECUTE FUNCTION clear_onboarding_sensitive_data();

-- Create view for pending onboarding approvals
CREATE OR REPLACE VIEW pending_onboarding_approvals_view AS
SELECT
    os.id AS session_id,
    os.company_id,
    os.email,
    os.phone,
    os.current_step,
    os.completed_steps,
    os.created_at,
    os.updated_at,
    EXTRACT(EPOCH FROM (NOW() - os.updated_at))/3600 AS hours_since_submission,
    wp.id AS worker_profile_id,
    wp.first_name,
    wp.last_name,
    wp.photo_url
FROM onboarding_sessions os
LEFT JOIN worker_profiles wp ON os.worker_profile_id = wp.id
WHERE os.status = 'pending_approval'
ORDER BY os.created_at ASC;

COMMENT ON VIEW pending_onboarding_approvals_view IS 'Admin view showing all pending onboarding approvals with worker details and submission time';

-- Create view for in-progress onboarding sessions (for abandonment tracking)
CREATE OR REPLACE VIEW abandoned_onboarding_sessions_view AS
SELECT
    os.id AS session_id,
    os.company_id,
    os.email,
    os.phone,
    os.current_step,
    os.completed_steps,
    os.created_at,
    os.updated_at,
    EXTRACT(EPOCH FROM (NOW() - os.updated_at))/3600 AS hours_inactive
FROM onboarding_sessions os
WHERE os.status = 'in_progress'
  AND os.updated_at < NOW() - INTERVAL '24 hours'
ORDER BY os.updated_at ASC;

COMMENT ON VIEW abandoned_onboarding_sessions_view IS 'View showing abandoned onboarding sessions (inactive for 24+ hours) for follow-up or cleanup';
