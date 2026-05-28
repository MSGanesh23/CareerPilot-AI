package com.careerpilot.domain.job.entity;

import com.careerpilot.common.BaseEntity;
import com.careerpilot.domain.resume.entity.Resume;
import com.careerpilot.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "job_applications", indexes = {
        @Index(name = "idx_job_app_user_id", columnList = "user_id"),
        @Index(name = "idx_job_app_status", columnList = "user_id, status"),
        @Index(name = "idx_job_app_applied_date", columnList = "user_id, applied_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @Column(name = "company", nullable = false, length = 255)
    private String company;

    @Column(name = "role_title", nullable = false, length = 255)
    private String roleTitle;

    @Column(name = "job_description", columnDefinition = "LONGTEXT", nullable = false)
    private String jobDescription;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "job_url", length = 1000)
    private String jobUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(30)")
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "applied_date", nullable = false)
    private LocalDate appliedDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * AI-computed match score (0-100) from SkillGapAnalysis.
     * Denormalized here for fast list queries.
     */
    @Column(name = "ai_match_score")
    private Integer aiMatchScore;
}
