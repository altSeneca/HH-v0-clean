-- ═══════════════════════════════════════════════════════════════════
-- Migration: 003_create_certification_tables.sql
-- Description: Create certification types and worker certifications tables
-- Author: HazardHawk Crew Management System
-- Created: 2025-10-08
-- ═══════════════════════════════════════════════════════════════════

-- Certification Types (Pre-seeded global library)
CREATE TABLE IF NOT EXISTS certification_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL, -- 'OSHA_10', 'OSHA_30', 'FORKLIFT', 'CPR', etc.
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL, -- 'safety_training', 'equipment_operation', 'emergency_response'
    region VARCHAR(10) NOT NULL DEFAULT 'US', -- 'US', 'UK', 'GLOBAL'
    typical_duration_months INTEGER, -- NULL if no expiration
    renewal_required BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    issuing_bodies JSONB DEFAULT '[]', -- ['OSHA', 'Red Cross', etc.]
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add comments for documentation
COMMENT ON TABLE certification_types IS 'Global catalog of certification types recognized by the system. Pre-seeded with common construction industry certifications.';
COMMENT ON COLUMN certification_types.code IS 'Unique certification code used for programmatic identification (e.g., OSHA_10, OSHA_30, FORKLIFT)';
COMMENT ON COLUMN certification_types.category IS 'Certification category: safety_training, equipment_operation, emergency_response, specialized_trade, etc.';
COMMENT ON COLUMN certification_types.typical_duration_months IS 'Typical validity period in months. NULL indicates no expiration.';
COMMENT ON COLUMN certification_types.issuing_bodies IS 'JSON array of organizations that issue this certification (e.g., ["OSHA", "Red Cross"])';

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_certification_types_code ON certification_types(code);
CREATE INDEX IF NOT EXISTS idx_certification_types_category ON certification_types(category);
CREATE INDEX IF NOT EXISTS idx_certification_types_region ON certification_types(region);

-- Add constraint checks
ALTER TABLE certification_types ADD CONSTRAINT check_category
    CHECK (category IN (
        'safety_training', 'equipment_operation', 'emergency_response',
        'specialized_trade', 'compliance', 'license', 'other'
    ));

ALTER TABLE certification_types ADD CONSTRAINT check_region
    CHECK (region IN ('US', 'UK', 'CA', 'AU', 'GLOBAL'));

-- Worker Certifications
CREATE TABLE IF NOT EXISTS worker_certifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    worker_profile_id UUID NOT NULL REFERENCES worker_profiles(id) ON DELETE CASCADE,
    company_id UUID REFERENCES companies(id) ON DELETE SET NULL, -- NULL if certification is global
    certification_type_id UUID NOT NULL REFERENCES certification_types(id),
    certification_number VARCHAR(100),
    issue_date DATE NOT NULL,
    expiration_date DATE,
    issuing_authority VARCHAR(255),
    document_url TEXT NOT NULL, -- S3 URL to certificate image/PDF
    thumbnail_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending_verification', -- 'pending_verification', 'verified', 'expired', 'rejected'
    verified_by UUID REFERENCES company_workers(id),
    verified_at TIMESTAMPTZ,
    rejection_reason TEXT,
    ocr_confidence DECIMAL(5,2), -- 0.00 to 100.00
    ocr_metadata JSONB DEFAULT '{}', -- Extracted fields with confidence scores
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add comments for documentation
COMMENT ON TABLE worker_certifications IS 'Worker certifications with document storage, OCR extraction, and verification workflow. Tracks expiration and sends alerts.';
COMMENT ON COLUMN worker_certifications.company_id IS 'Company-specific certification if applicable. NULL indicates certification valid across all companies.';
COMMENT ON COLUMN worker_certifications.status IS 'Verification status: pending_verification (awaiting admin review), verified (approved), expired (past expiration date), rejected (invalid)';
COMMENT ON COLUMN worker_certifications.document_url IS 'S3 URL to original certification document (image or PDF)';
COMMENT ON COLUMN worker_certifications.ocr_confidence IS 'Overall confidence score from Google Document AI OCR (0-100)';
COMMENT ON COLUMN worker_certifications.ocr_metadata IS 'JSON object containing extracted fields and individual confidence scores';

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_worker_certs_worker ON worker_certifications(worker_profile_id, status);
CREATE INDEX IF NOT EXISTS idx_worker_certs_expiration ON worker_certifications(expiration_date) WHERE status = 'verified' AND expiration_date IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_worker_certs_company ON worker_certifications(company_id, status) WHERE company_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_worker_certs_type ON worker_certifications(certification_type_id);
CREATE INDEX IF NOT EXISTS idx_worker_certs_pending ON worker_certifications(created_at DESC) WHERE status = 'pending_verification';

-- Add constraint checks
ALTER TABLE worker_certifications ADD CONSTRAINT check_cert_status
    CHECK (status IN ('pending_verification', 'verified', 'expired', 'rejected'));

ALTER TABLE worker_certifications ADD CONSTRAINT check_ocr_confidence
    CHECK (ocr_confidence IS NULL OR (ocr_confidence >= 0 AND ocr_confidence <= 100));

ALTER TABLE worker_certifications ADD CONSTRAINT check_dates
    CHECK (issue_date <= COALESCE(expiration_date, issue_date));

-- Create trigger for updated_at timestamp
CREATE TRIGGER update_worker_certifications_updated_at
    BEFORE UPDATE ON worker_certifications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create trigger to auto-update status to expired when expiration_date passes
CREATE OR REPLACE FUNCTION auto_expire_certifications()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.expiration_date IS NOT NULL AND NEW.expiration_date < CURRENT_DATE AND NEW.status = 'verified' THEN
        NEW.status = 'expired';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_certification_expiration
    BEFORE INSERT OR UPDATE ON worker_certifications
    FOR EACH ROW
    EXECUTE FUNCTION auto_expire_certifications();

-- Insert common certification types (seed data)
INSERT INTO certification_types (code, name, category, region, typical_duration_months, renewal_required, description, issuing_bodies)
VALUES
    ('OSHA_10', 'OSHA 10-Hour Construction Safety', 'safety_training', 'US', NULL, FALSE, 'Basic safety training for construction workers covering common hazards and OSHA regulations', '["OSHA"]'),
    ('OSHA_30', 'OSHA 30-Hour Construction Safety', 'safety_training', 'US', NULL, FALSE, 'Comprehensive safety training for supervisors and workers with safety responsibilities', '["OSHA"]'),
    ('FORKLIFT', 'Forklift Operator Certification', 'equipment_operation', 'US', 36, TRUE, 'Certification for operating powered industrial trucks and forklifts', '["OSHA", "Various Training Providers"]'),
    ('CPR', 'CPR/AED Certification', 'emergency_response', 'GLOBAL', 24, TRUE, 'Cardiopulmonary resuscitation and automated external defibrillator training', '["American Red Cross", "American Heart Association"]'),
    ('FIRST_AID', 'First Aid Certification', 'emergency_response', 'GLOBAL', 24, TRUE, 'Basic first aid and emergency response training', '["American Red Cross", "American Heart Association"]'),
    ('CONFINED_SPACE', 'Confined Space Entry', 'safety_training', 'US', 12, TRUE, 'Training for entering and working in confined spaces safely', '["OSHA", "Various Training Providers"]'),
    ('FALL_PROTECTION', 'Fall Protection Training', 'safety_training', 'US', 12, TRUE, 'Training on fall hazards, prevention, and proper use of fall protection equipment', '["OSHA", "Various Training Providers"]'),
    ('SCAFFOLD', 'Scaffold Competent Person', 'safety_training', 'US', 12, TRUE, 'Certification for inspecting and supervising scaffold construction and use', '["OSHA", "Various Training Providers"]'),
    ('CRANE_OPERATOR', 'Crane Operator Certification', 'equipment_operation', 'US', 60, TRUE, 'Certification for operating mobile cranes and tower cranes', '["NCCCO", "OSHA"]'),
    ('RIGGING', 'Rigging and Signal Person', 'equipment_operation', 'US', 36, TRUE, 'Training for rigging loads and directing crane operations', '["NCCCO", "Various Training Providers"]'),
    ('EXCAVATION', 'Excavation Competent Person', 'safety_training', 'US', 12, TRUE, 'Training for supervising excavation work and ensuring trench safety', '["OSHA", "Various Training Providers"]'),
    ('HAZMAT', 'Hazardous Materials Handling', 'safety_training', 'US', 12, TRUE, 'Training for handling and disposing of hazardous materials', '["OSHA", "EPA"]'),
    ('LEAD_ABATEMENT', 'Lead Abatement Certification', 'specialized_trade', 'US', 12, TRUE, 'Certification for working with lead-based paint and materials', '["EPA", "State Agencies"]'),
    ('ASBESTOS', 'Asbestos Abatement Certification', 'specialized_trade', 'US', 12, TRUE, 'Certification for asbestos inspection and removal', '["EPA", "State Agencies"]'),
    ('SILICA', 'Respirable Crystalline Silica Training', 'safety_training', 'US', 12, TRUE, 'Training on silica exposure control and respiratory protection', '["OSHA"]')
ON CONFLICT (code) DO NOTHING;
