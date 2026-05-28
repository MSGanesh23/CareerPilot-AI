package com.careerpilot.domain.interview.entity;

import com.careerpilot.common.BaseEntity;
import com.careerpilot.domain.job.entity.JobApplication;
import com.careerpilot.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mock_interview_sessions", indexes = {
        @Index(name = "idx_mock_session_user_id", columnList = "user_id"),
        @Index(name = "idx_mock_session_job_app_id", columnList = "job_application_id"),
        @Index(name = "idx_mock_session_status", columnList = "user_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockInterviewSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_application_id", nullable = false)
    private JobApplication jobApplication;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    /**
     * Computed average score (1-10) across all evaluated answers.
     * Updated each time an answer is scored. NULL until first answer is scored.
     */
    @Column(name = "overall_score", precision = 4, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "total_questions", nullable = false)
    @Builder.Default
    private int totalQuestions = 0;

    @Column(name = "answered_count", nullable = false)
    @Builder.Default
    private int answeredCount = 0;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    @Builder.Default
    private List<InterviewQuestion> questions = new ArrayList<>();
}
