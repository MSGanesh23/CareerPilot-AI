package com.careerpilot.domain.job.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "skill_gap_analyses", indexes = {
        @Index(name = "idx_skill_gap_job_app_id", columnList = "job_application_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillGapAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_application_id", nullable = false, unique = true)
    private JobApplication jobApplication;

    @Column(name = "match_score", nullable = false)
    private int matchScore;

    @Column(name = "missing_skills", columnDefinition = "TEXT")
    private String missingSkills;   // JSON array

    @Column(name = "strong_skills", columnDefinition = "TEXT")
    private String strongSkills;    // JSON array

    @Column(name = "improvement_suggestions", columnDefinition = "TEXT")
    private String improvementSuggestions; // JSON array

    @Column(name = "raw_ai_response", columnDefinition = "LONGTEXT")
    private String rawAiResponse;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
