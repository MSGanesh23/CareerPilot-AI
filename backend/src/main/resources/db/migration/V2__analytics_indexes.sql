-- ============================================================
-- CareerPilot AI - V2 Analytics Performance Indexes
-- ============================================================

-- Speed up analytics GROUP BY month queries on job applications
CREATE INDEX idx_job_app_applied_date_user
    ON job_applications (user_id, applied_date);

-- Speed up session trend queries
CREATE INDEX idx_mock_session_created_status
    ON mock_interview_sessions (user_id, status, created_at);

-- Speed up answered question lookups for score recalculation
CREATE INDEX idx_iq_session_score
    ON interview_questions (session_id, ai_score);
