-- ============================================================
-- CareerPilot AI - V1 Initial Schema
-- Database: MySQL 8+
-- ============================================================

SET
    FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- USERS TABLE
-- ============================================================
CREATE TABLE users
(
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    email            VARCHAR(255)    NOT NULL,
    password_hash    VARCHAR(255)    NOT NULL,
    full_name        VARCHAR(255)    NOT NULL,
    years_experience INT             NULL,
    skills           TEXT            NULL COMMENT 'JSON array of skill strings',
    target_roles     TEXT            NULL COMMENT 'JSON array of role strings',
    role             VARCHAR(20)     NOT NULL DEFAULT 'USER' COMMENT 'USER | ADMIN',
    is_active        TINYINT(1)      NOT NULL DEFAULT 1,
    created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);

-- ============================================================
-- RESUMES TABLE
-- ============================================================
CREATE TABLE resumes
(
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    user_id       BIGINT          NOT NULL,
    file_name     VARCHAR(255)    NOT NULL,
    file_path     VARCHAR(500)    NOT NULL,
    file_size     BIGINT          NULL,
    content_type  VARCHAR(100)    NULL,
    parsed_text   LONGTEXT        NULL COMMENT 'Extracted plain text from resume',
    version       INT             NOT NULL DEFAULT 1,
    is_active     TINYINT(1)      NOT NULL DEFAULT 1 COMMENT 'Active/current resume version',
    label         VARCHAR(100)    NULL     COMMENT 'Optional label e.g. Backend-v3',
    created_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_resumes PRIMARY KEY (id),
    CONSTRAINT fk_resumes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_resumes_user_id ON resumes (user_id);
CREATE INDEX idx_resumes_user_active ON resumes (user_id, is_active);

-- ============================================================
-- JOB APPLICATIONS TABLE
-- ============================================================
CREATE TABLE job_applications
(
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    resume_id       BIGINT          NULL COMMENT 'Resume used for this application',
    company         VARCHAR(255)    NOT NULL,
    role_title      VARCHAR(255)    NOT NULL,
    job_description LONGTEXT        NOT NULL,
    location        VARCHAR(255)    NULL,
    job_url         VARCHAR(1000)   NULL,
    status          VARCHAR(30)     NOT NULL DEFAULT 'APPLIED' COMMENT 'APPLIED|INTERVIEWING|OFFER|REJECTED|WITHDRAWN',
    applied_date    DATE            NOT NULL,
    notes           TEXT            NULL,
    ai_match_score  INT             NULL COMMENT '0-100 score from AI analysis',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_job_applications PRIMARY KEY (id),
    CONSTRAINT fk_job_app_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_job_app_resume FOREIGN KEY (resume_id) REFERENCES resumes (id) ON DELETE SET NULL
);

CREATE INDEX idx_job_app_user_id ON job_applications (user_id);
CREATE INDEX idx_job_app_status ON job_applications (user_id, status);
CREATE INDEX idx_job_app_applied_date ON job_applications (user_id, applied_date);

-- ============================================================
-- SKILL GAP ANALYSIS TABLE
-- ============================================================
CREATE TABLE skill_gap_analyses
(
    id                       BIGINT      NOT NULL AUTO_INCREMENT,
    job_application_id       BIGINT      NOT NULL,
    match_score              INT         NOT NULL COMMENT '0-100 match percentage',
    missing_skills           TEXT        NULL COMMENT 'JSON array of missing skill strings',
    strong_skills            TEXT        NULL COMMENT 'JSON array of matched skill strings',
    improvement_suggestions  TEXT        NULL COMMENT 'AI-generated suggestions text or JSON',
    raw_ai_response          LONGTEXT    NULL COMMENT 'Full raw AI response for debugging',
    created_at               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_skill_gap PRIMARY KEY (id),
    CONSTRAINT fk_skill_gap_job_app FOREIGN KEY (job_application_id) REFERENCES job_applications (id) ON DELETE CASCADE,
    CONSTRAINT uq_skill_gap_job_app UNIQUE (job_application_id) COMMENT 'One analysis per job application'
);

CREATE INDEX idx_skill_gap_job_app_id ON skill_gap_analyses (job_application_id);

-- ============================================================
-- MOCK INTERVIEW SESSIONS TABLE
-- ============================================================
CREATE TABLE mock_interview_sessions
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    user_id            BIGINT       NOT NULL,
    job_application_id BIGINT       NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS | COMPLETED | ABANDONED',
    overall_score      DECIMAL(4,2) NULL     COMMENT 'Computed average score across all answers (1-10)',
    total_questions    INT          NOT NULL DEFAULT 0,
    answered_count     INT          NOT NULL DEFAULT 0,
    created_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_mock_sessions PRIMARY KEY (id),
    CONSTRAINT fk_mock_session_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_mock_session_job_app FOREIGN KEY (job_application_id) REFERENCES job_applications (id) ON DELETE CASCADE
);

CREATE INDEX idx_mock_session_user_id ON mock_interview_sessions (user_id);
CREATE INDEX idx_mock_session_job_app_id ON mock_interview_sessions (job_application_id);
CREATE INDEX idx_mock_session_status ON mock_interview_sessions (user_id, status);

-- ============================================================
-- INTERVIEW QUESTIONS TABLE
-- ============================================================
CREATE TABLE interview_questions
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    session_id      BIGINT       NOT NULL,
    question_type   VARCHAR(20)  NOT NULL COMMENT 'TECHNICAL | BEHAVIORAL | PROJECT',
    question_text   TEXT         NOT NULL,
    sequence_order  INT          NOT NULL DEFAULT 0 COMMENT 'Order within the session',
    user_answer     TEXT         NULL     COMMENT 'Filled in after user answers',
    ai_score        INT          NULL     COMMENT '1-10 score for the answer',
    ai_feedback     TEXT         NULL     COMMENT 'AI-generated feedback on the answer',
    ideal_answer    TEXT         NULL     COMMENT 'AI-generated sample ideal answer',
    answered_at     DATETIME(6)  NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_interview_questions PRIMARY KEY (id),
    CONSTRAINT fk_iq_session FOREIGN KEY (session_id) REFERENCES mock_interview_sessions (id) ON DELETE CASCADE
);

CREATE INDEX idx_iq_session_id ON interview_questions (session_id);
CREATE INDEX idx_iq_type ON interview_questions (session_id, question_type);

-- ============================================================
-- RE-ENABLE FK CHECKS
-- ============================================================
SET FOREIGN_KEY_CHECKS = 1;
