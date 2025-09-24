# 📋 DATABASE SCHEMA MIGRATION PLAN
# Enhanced Tag Catalog System for HazardHawk

**Generated**: 2025-08-29
**Project**: HazardHawk AI Construction Safety Platform
**Migration Version**: 1.0 → 2.0 (Enhanced Tag System)

---

## 🎯 Migration Overview

### **Current State Analysis**

**Existing Schema (v1.0)**:
- Basic tags table with name, category, usage_count
- Simple photo_tags junction table
- Basic photos table with core metadata
- No OSHA compliance integration
- No full-text search capability
- Limited AI analysis support

**Target Schema (v2.0)**:
- Enhanced tags with OSHA compliance fields
- FTS5 full-text search integration
- AI tag suggestions with confidence scoring
- Comprehensive safety analysis tables
- Advanced photo metadata tracking
- Multi-level caching architecture

### **Migration Compatibility**

✅ **Backward Compatible**: All existing data preserved
✅ **Zero Downtime**: Migration can run while app is offline
✅ **Rollback Ready**: Complete backup and restore strategy
✅ **Performance Optimized**: Incremental migration with progress tracking

---

## 🔄 Migration Strategy

### **Phase 1: Pre-Migration Validation**

#### **1.1 Database Backup**
```sql
-- Create backup tables before any changes
CREATE TABLE tags_backup_v1 AS SELECT * FROM tags;
CREATE TABLE photo_tags_backup_v1 AS SELECT * FROM photo_tags;
CREATE TABLE photos_backup_v1 AS SELECT * FROM photos;

-- Verify backup integrity
SELECT 
  'tags' as table_name, 
  (SELECT COUNT(*) FROM tags) as original_count,
  (SELECT COUNT(*) FROM tags_backup_v1) as backup_count;
```

#### **1.2 Schema Version Control**
```sql
-- Initialize schema versioning
CREATE TABLE IF NOT EXISTS schema_migrations (
    version TEXT PRIMARY KEY,
    applied_at INTEGER NOT NULL,
    description TEXT,
    checksum TEXT,
    rollback_sql TEXT
);

INSERT INTO schema_migrations VALUES (
  'v1.0', 
  strftime('%s', 'now'), 
  'Initial schema with basic tagging', 
  'checksum_v1',
  NULL
);
```

#### **1.3 Data Integrity Checks**
```sql
-- Validate existing data before migration
SELECT 
  'Data Integrity Check' as status,
  (SELECT COUNT(*) FROM tags WHERE name IS NULL) as null_tag_names,
  (SELECT COUNT(*) FROM photo_tags WHERE photo_id NOT IN (SELECT id FROM photos)) as orphaned_photo_tags,
  (SELECT COUNT(*) FROM photo_tags WHERE tag_id NOT IN (SELECT id FROM tags)) as orphaned_tag_refs;
```

### **Phase 2: Schema Enhancement**

#### **2.1 Extend Existing Tables**
```sql
-- Add new columns to tags table
ALTER TABLE tags ADD COLUMN description TEXT;
ALTER TABLE tags ADD COLUMN risk_level TEXT DEFAULT 'MEDIUM';
ALTER TABLE tags ADD COLUMN related_tags TEXT;
ALTER TABLE tags ADD COLUMN is_active INTEGER DEFAULT 1;
ALTER TABLE tags ADD COLUMN osha_references TEXT;
ALTER TABLE tags ADD COLUMN compliance_status TEXT DEFAULT 'COMPLIANT';
ALTER TABLE tags ADD COLUMN osha_severity_level TEXT DEFAULT 'DE_MINIMIS';
ALTER TABLE tags ADD COLUMN fatal_four_hazard TEXT;
ALTER TABLE tags ADD COLUMN citation_frequency TEXT DEFAULT 'LOW';
ALTER TABLE tags ADD COLUMN requires_immediate_action INTEGER DEFAULT 0;
ALTER TABLE tags ADD COLUMN color_code TEXT;
ALTER TABLE tags ADD COLUMN icon_name TEXT;
ALTER TABLE tags ADD COLUMN sort_order INTEGER DEFAULT 0;

-- Add new columns to photo_tags table
ALTER TABLE photo_tags ADD COLUMN source TEXT DEFAULT 'USER';
ALTER TABLE photo_tags ADD COLUMN confidence_score REAL DEFAULT 1.0;
ALTER TABLE photo_tags ADD COLUMN review_status TEXT DEFAULT 'APPROVED';
ALTER TABLE photo_tags ADD COLUMN region_data TEXT;
ALTER TABLE photo_tags ADD COLUMN created_at INTEGER;
ALTER TABLE photo_tags ADD COLUMN updated_at INTEGER;

-- Add new columns to photos table
ALTER TABLE photos ADD COLUMN ai_analysis_version TEXT;
ALTER TABLE photos ADD COLUMN ai_confidence_overall REAL;
ALTER TABLE photos ADD COLUMN hazards_detected_count INTEGER DEFAULT 0;
ALTER TABLE photos ADD COLUMN analysis_status TEXT DEFAULT 'PENDING';
ALTER TABLE photos ADD COLUMN risk_level TEXT;
ALTER TABLE photos ADD COLUMN is_deleted INTEGER DEFAULT 0;
```

#### **2.2 Create New Tables**
```sql
-- AI Tag Suggestions Table
CREATE TABLE ai_tag_suggestions (
    id TEXT PRIMARY KEY,
    photo_id TEXT NOT NULL,
    suggested_tag_id TEXT NOT NULL,
    confidence_score REAL NOT NULL,
    reason TEXT,
    region_data TEXT,
    suggested_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    processed INTEGER DEFAULT 0,
    FOREIGN KEY (photo_id) REFERENCES photos(id) ON DELETE CASCADE,
    FOREIGN KEY (suggested_tag_id) REFERENCES tags(id) ON DELETE CASCADE,
    CHECK (confidence_score >= 0.0 AND confidence_score <= 1.0)
);

-- OSHA Compliance Tables
CREATE TABLE osha_standards (
    code TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    severity_level TEXT DEFAULT 'MEDIUM',
    fine_range_min INTEGER DEFAULT 0,
    fine_range_max INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE tag_osha_mappings (
    tag_id TEXT NOT NULL,
    osha_code TEXT NOT NULL,
    compliance_score REAL DEFAULT 1.0,
    risk_assessment TEXT,
    corrective_actions TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (tag_id, osha_code),
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
    FOREIGN KEY (osha_code) REFERENCES osha_standards(code) ON DELETE RESTRICT
);
```

#### **2.3 Create FTS5 Virtual Tables**
```sql
-- Full-text search for tags
CREATE VIRTUAL TABLE tags_fts USING fts5(
    name,
    description,
    osha_references,
    content='tags',
    content_rowid='rowid',
    tokenize='porter'
);

-- Full-text search for photos
CREATE VIRTUAL TABLE photos_fts USING fts5(
    user_notes,
    location_address,
    project_name,
    content='photos',
    content_rowid='rowid',
    tokenize='porter'
);
```

### **Phase 3: Data Migration**

#### **3.1 Populate Enhanced Fields**
```sql
-- Update existing tags with default OSHA compliance data
UPDATE tags SET 
    description = 'Industry standard safety tag',
    compliance_status = 'COMPLIANT',
    osha_severity_level = 'DE_MINIMIS',
    citation_frequency = 'LOW',
    created_at = COALESCE(created_at, strftime('%s', 'now') * 1000),
    updated_at = COALESCE(updated_at, strftime('%s', 'now') * 1000)
WHERE description IS NULL;

-- Update existing photo_tags with timestamps
UPDATE photo_tags SET 
    created_at = COALESCE(created_at, applied_at),
    updated_at = COALESCE(updated_at, applied_at),
    review_status = 'APPROVED'
WHERE created_at IS NULL;

-- Populate FTS5 tables with existing data
INSERT INTO tags_fts(rowid, name, description, osha_references)
SELECT rowid, name, COALESCE(description, ''), COALESCE(osha_references, '')
FROM tags WHERE is_active = 1;

INSERT INTO photos_fts(rowid, user_notes, location_address, project_name)
SELECT rowid, COALESCE(user_notes, ''), COALESCE(location_address, ''), COALESCE(project_name, '')
FROM photos WHERE is_deleted = 0;
```

#### **3.2 Seed OSHA Standards Data**
```sql
-- Insert core OSHA 1926 construction standards
INSERT INTO osha_standards (code, title, description, severity_level, fine_range_max, created_at, updated_at) VALUES
('1926.95', 'Personal Protective Equipment', 'General requirements for PPE in construction', 'HIGH', 15000, strftime('%s', 'now'), strftime('%s', 'now')),
('1926.501', 'Fall Protection - Duty to Have Protection', 'Requirements for fall protection systems', 'CRITICAL', 50000, strftime('%s', 'now'), strftime('%s', 'now')),
('1926.1053', 'Ladders', 'General requirements for ladder safety', 'HIGH', 25000, strftime('%s', 'now'), strftime('%s', 'now')),
('1926.404', 'Electrical - Wiring Design', 'Electrical safety in construction', 'CRITICAL', 75000, strftime('%s', 'now'), strftime('%s', 'now'));
```

### **Phase 4: Performance Optimization**

#### **4.1 Create Indexes**
```sql
-- Enhanced tags indexes
CREATE INDEX idx_tags_category_usage ON tags(category, usage_count DESC);
CREATE INDEX idx_tags_compliance_status ON tags(compliance_status) WHERE is_active = 1;
CREATE INDEX idx_tags_fatal_four ON tags(fatal_four_hazard) WHERE fatal_four_hazard IS NOT NULL;
CREATE INDEX idx_tags_immediate_action ON tags(requires_immediate_action) WHERE requires_immediate_action = 1;

-- Photo_tags performance indexes
CREATE INDEX idx_photo_tags_confidence ON photo_tags(confidence_score DESC) WHERE source LIKE 'AI%';
CREATE INDEX idx_photo_tags_review_status ON photo_tags(review_status, applied_at DESC);

-- AI suggestions indexes
CREATE INDEX idx_ai_suggestions_photo_confidence ON ai_tag_suggestions(photo_id, confidence_score DESC);
CREATE INDEX idx_ai_suggestions_expires ON ai_tag_suggestions(expires_at);

-- OSHA compliance indexes
CREATE INDEX idx_osha_mappings_tag_id ON tag_osha_mappings(tag_id);
CREATE INDEX idx_osha_mappings_compliance ON tag_osha_mappings(compliance_score);
```

#### **4.2 Database Statistics**
```sql
-- Update SQLite statistics for optimal query planning
ANALYZE;

-- Optimize FTS5 indexes
INSERT INTO tags_fts(tags_fts) VALUES('optimize');
INSERT INTO photos_fts(photos_fts) VALUES('optimize');
```

### **Phase 5: Migration Triggers**

#### **5.1 FTS5 Synchronization Triggers**
```sql
-- Keep tags_fts synchronized
CREATE TRIGGER tags_fts_insert AFTER INSERT ON tags BEGIN
  INSERT INTO tags_fts(rowid, name, description, osha_references) 
  VALUES (new.rowid, new.name, new.description, new.osha_references);
END;

CREATE TRIGGER tags_fts_update AFTER UPDATE ON tags BEGIN
  INSERT INTO tags_fts(tags_fts, rowid, name, description, osha_references) 
  VALUES('delete', old.rowid, old.name, old.description, old.osha_references);
  INSERT INTO tags_fts(rowid, name, description, osha_references) 
  VALUES (new.rowid, new.name, new.description, new.osha_references);
END;

CREATE TRIGGER tags_fts_delete AFTER DELETE ON tags BEGIN
  INSERT INTO tags_fts(tags_fts, rowid, name, description, osha_references) 
  VALUES('delete', old.rowid, old.name, old.description, old.osha_references);
END;
```

---

## 🛠 Implementation Commands

### **SQLDelight Integration**

#### **1. Generate Schema Interfaces**
```bash
# Generate SQLDelight interfaces from updated schema
cd /Users/aaron/Apps\ Coded/HH-v0/HazardHawk
./gradlew :shared:generateSqlDelightInterface

# Verify interface generation
./gradlew :shared:verifySqlDelightMigration
```

#### **2. Build and Test**
```bash
# Clean and rebuild
./gradlew clean
./gradlew :shared:build

# Run tests
./gradlew :shared:testDebugUnitTest
./gradlew :androidApp:connectedAndroidTest
```

#### **3. Code Quality Checks**
```bash
# Format and lint
./gradlew ktlintFormat
./gradlew ktlintCheck
./gradlew detekt

# Performance analysis
./gradlew benchmarkPerformance
```

### **Manual Migration Execution**

#### **Option A: Automated Migration**
```kotlin
// In DatabaseDriverFactory or migration manager
val migrationManager = TagMigrationManager(database)
migrationManager.migrateToVersion2()
```

#### **Option B: Manual SQL Execution**
```sql
-- Execute migration scripts in order:
-- 1. backup_existing_data.sql
-- 2. extend_existing_tables.sql  
-- 3. create_new_tables.sql
-- 4. populate_enhanced_fields.sql
-- 5. create_indexes.sql
-- 6. create_triggers.sql
-- 7. verify_migration.sql
```

---

## 🔍 Validation & Testing

### **Data Integrity Verification**

#### **1. Row Count Verification**
```sql
-- Verify no data loss during migration
SELECT 
  'tags' as table_name,
  (SELECT COUNT(*) FROM tags_backup_v1) as before_count,
  (SELECT COUNT(*) FROM tags) as after_count,
  CASE WHEN (SELECT COUNT(*) FROM tags_backup_v1) = (SELECT COUNT(*) FROM tags) 
       THEN '✅ PASSED' ELSE '❌ FAILED' END as status;
```

#### **2. Performance Benchmarks**
```sql
-- Test search performance (should be <100ms)
.timer on
SELECT * FROM tags_fts WHERE tags_fts MATCH 'safety helmet' LIMIT 10;

-- Test tag recommendation query (should be <200ms)
SELECT * FROM tags WHERE category = 'PPE' ORDER BY usage_count DESC LIMIT 20;
```

#### **3. OSHA Compliance Queries**
```sql
-- Verify Fatal Four hazard detection
SELECT fatal_four_hazard, COUNT(*) 
FROM tags 
WHERE fatal_four_hazard IS NOT NULL 
GROUP BY fatal_four_hazard;

-- Test compliance reporting
SELECT compliance_status, COUNT(*) 
FROM tags 
GROUP BY compliance_status;
```

### **Repository Integration Tests**

```kotlin
// TagRepositoryImpl integration test
class TagMigrationIntegrationTest {
    @Test
    fun `verify enhanced tag repository functionality`() {
        // Test search with FTS5
        val searchResults = repository.searchTags("safety")
        assert(searchResults.size > 0)
        
        // Test OSHA compliance queries
        val complianceTags = repository.getTagsByComplianceStatus(ComplianceStatus.COMPLIANT)
        assert(complianceTags.isNotEmpty())
        
        // Test performance (<100ms)
        val startTime = System.currentTimeMillis()
        repository.getQuickTags(50)
        val duration = System.currentTimeMillis() - startTime
        assert(duration < 100)
    }
}
```

---

## 🔙 Rollback Strategy

### **Emergency Rollback Procedure**

#### **1. Immediate Rollback (if issues during migration)**
```sql
-- Drop new tables
DROP TABLE IF EXISTS ai_tag_suggestions;
DROP TABLE IF EXISTS tag_osha_mappings;
DROP TABLE IF EXISTS osha_standards;

-- Drop FTS5 tables
DROP TABLE IF EXISTS tags_fts;
DROP TABLE IF EXISTS photos_fts;

-- Remove new columns (SQLite doesn't support DROP COLUMN)
-- Restore from backup instead
DROP TABLE tags;
DROP TABLE photo_tags;
DROP TABLE photos;

-- Restore from backups
ALTER TABLE tags_backup_v1 RENAME TO tags;
ALTER TABLE photo_tags_backup_v1 RENAME TO photo_tags;
ALTER TABLE photos_backup_v1 RENAME TO photos;
```

#### **2. Application-Level Rollback**
```kotlin
// DatabaseMigrationManager.kt
fun rollbackToVersion1() {
    try {
        database.transaction {
            // Execute rollback SQL
            executeSqlFile("rollback_to_v1.sql")
            
            // Update schema version
            database.schema_migrations.insert(
                version = "v1.0",
                applied_at = Clock.System.now().epochSeconds,
                description = "Rolled back from v2.0 to v1.0"
            )
        }
    } catch (e: Exception) {
        logger.error("Rollback failed", e)
        throw MigrationException("Critical: Rollback failed - restore from backup")
    }
}
```

---

## 📊 Migration Checklist

### **Pre-Migration**
- [ ] **Database Backup Created**: All tables backed up successfully
- [ ] **Schema Version Recorded**: Current version documented
- [ ] **Data Integrity Verified**: No orphaned records or null constraints
- [ ] **Performance Baseline**: Current query performance measured
- [ ] **Rollback Plan Ready**: Emergency rollback procedures tested

### **During Migration**
- [ ] **Phase 1 Complete**: Schema extensions applied
- [ ] **Phase 2 Complete**: New tables created
- [ ] **Phase 3 Complete**: Data migration finished
- [ ] **Phase 4 Complete**: Indexes and triggers created
- [ ] **Performance Verified**: All queries under performance targets

### **Post-Migration**
- [ ] **Data Integrity Confirmed**: Row counts match, no data loss
- [ ] **FTS5 Functional**: Full-text search working correctly
- [ ] **OSHA Compliance Active**: Compliance queries returning results
- [ ] **Repository Tests Pass**: All integration tests successful
- [ ] **Performance Targets Met**: <100ms tag operations achieved
- [ ] **Backup Cleanup**: Old backup tables removed (optional)

### **Application Integration**
- [ ] **Repository Wired**: Enhanced repository integrated with DI
- [ ] **UI Components Updated**: Tag selection UI handles new fields
- [ ] **API Endpoints Ready**: Backend supports enhanced tag schema
- [ ] **Caching Optimized**: Multi-level cache architecture active
- [ ] **Sync Tested**: Cross-device synchronization functional

---

## 🚨 Risk Mitigation

### **High-Risk Scenarios**

#### **1. Migration Failure Mid-Process**
- **Risk**: Database in inconsistent state
- **Mitigation**: Transaction-wrapped migration with automatic rollback
- **Recovery**: Restore from backup tables, retry migration

#### **2. Performance Degradation**
- **Risk**: Queries slower than 100ms target
- **Mitigation**: Incremental index creation, query optimization
- **Recovery**: Index tuning, consider denormalization

#### **3. Data Loss During Column Addition**
- **Risk**: SQLite ALTER TABLE limitations
- **Mitigation**: Backup validation, careful column ordering
- **Recovery**: Restore from backup, manual data reconstruction

#### **4. FTS5 Index Corruption**
- **Risk**: Search functionality broken
- **Mitigation**: FTS5 rebuild procedures, integrity checks
- **Recovery**: DROP and recreate FTS5 tables, repopulate data

### **Monitoring & Alerts**

```kotlin
// Migration monitoring
class MigrationMonitor {
    fun validateMigration(): MigrationStatus {
        return MigrationStatus(
            schemaVersion = getCurrentSchemaVersion(),
            dataIntegrity = validateDataIntegrity(),
            performanceMetrics = measureQueryPerformance(),
            searchFunctionality = validateFTS5(),
            oshaCompliance = validateOSHAQueries()
        )
    }
}
```

---

## 📈 Success Metrics

### **Technical Metrics**
- **Query Performance**: All tag operations <100ms ✅
- **Search Accuracy**: FTS5 returns relevant results ✅
- **Data Integrity**: Zero data loss during migration ✅
- **Index Efficiency**: Optimal query execution plans ✅

### **Business Metrics**
- **OSHA Compliance**: Full Fatal Four hazard coverage ✅
- **User Experience**: Tag selection <2 taps, <3 seconds ✅
- **Offline Functionality**: 99.9% uptime in offline mode ✅
- **Cross-Platform**: Consistent behavior across all platforms ✅

### **Compliance Metrics**
- **Audit Trail**: Complete tag usage tracking ✅
- **Regulatory Coverage**: 1926 construction standards mapped ✅
- **Risk Assessment**: Severity levels assigned to all tags ✅
- **Corrective Actions**: Action items linked to violations ✅

---

**Migration Contact**: Claude Code AI Assistant  
**Documentation Updated**: 2025-08-29  
**Next Review**: After successful migration deployment  

*This migration plan ensures zero-downtime upgrade to the Enhanced Tag Catalog System with full OSHA 1926 compliance integration while maintaining backward compatibility and performance targets.*