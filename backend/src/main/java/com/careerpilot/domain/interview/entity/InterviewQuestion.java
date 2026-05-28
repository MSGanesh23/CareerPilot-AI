package com.careerpilot.domain.interview.entity;

import com.careerpilot.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "interview_questions", indexes = {
        @Index(name = "idx_iq_session_id", columnList = "session_id"),
        @Index(name = "idx_iq_type", columnList = "session_id, question_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private MockInterviewSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, columnDefinition = "VARCHAR(20)")
    private QuestionType questionType;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "sequence_order", nullable = false)
    @Builder.Default
    private int sequenceOrder = 0;

    // ---- Filled in after the user submits their answer ----

    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    /** AI-assigned score: 1–10. NULL until answer is evaluated. */
    @Column(name = "ai_score")
    private Integer aiScore;

    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    @Column(name = "ideal_answer", columnDefinition = "TEXT")
    private String idealAnswer;

    @Column(name = "answered_at")
    private Instant answeredAt;
}
