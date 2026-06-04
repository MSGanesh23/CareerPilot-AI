-- ============================================================
-- CareerPilot AI - V3: Add updated_at to skill_gap_analyses
--
-- Reason: SkillGapAnalysis entity now extends BaseEntity which
-- manages both created_at and updated_at via Spring Data Auditing.
-- The original V1 schema only had created_at on this table.
-- ============================================================

ALTER TABLE skill_gap_analyses
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
        AFTER created_at;
