package com.careerpilot.domain.interview.dto;

import com.careerpilot.domain.interview.entity.QuestionType;
import com.careerpilot.domain.interview.entity.SessionStatus;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class InterviewDtos {

    private InterviewDtos() {
    }

    // ----------------------------------------------------------------
    // Start new session request
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class StartSessionRequest {

        @NotNull(message = "Job application ID is required")
        private Long jobApplicationId;

        @Min(value = 1, message = "Must generate at least 1 technical question")
        @Max(value = 10, message = "Maximum 10 technical questions")
        @Builder.Default
        private int technicalCount = 3;

        @Min(value = 1)
        @Max(value = 10)
        @Builder.Default
        private int behavioralCount = 2;

        @Min(value = 0)
        @Max(value = 5)
        @Builder.Default
        private int projectCount = 1;
    }

    // ----------------------------------------------------------------
    // Submit an answer
    // ----------------------------------------------------------------
    @Data
    public static class SubmitAnswerRequest {

        @NotBlank(message = "Answer cannot be empty")
        @Size(min = 10, message = "Answer must be at least 10 characters")
        @Size(max = 10000, message = "Answer cannot exceed 10,000 characters")
        private String answer;
    }

    // ----------------------------------------------------------------
    // Session summary (list view)
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class SessionSummaryDto {
        private Long id;
        private Long jobApplicationId;
        private String company;
        private String roleTitle;
        private SessionStatus status;
        private BigDecimal overallScore;
        private int totalQuestions;
        private int answeredCount;
        private Instant createdAt;
        private Instant updatedAt;
    }

    // ----------------------------------------------------------------
    // Session detail (includes all questions)
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class SessionDetailDto {
        private Long id;
        private Long jobApplicationId;
        private String company;
        private String roleTitle;
        private SessionStatus status;
        private BigDecimal overallScore;
        private int totalQuestions;
        private int answeredCount;
        private List<QuestionDto> questions;
        private Instant createdAt;
        private Instant updatedAt;
    }

    // ----------------------------------------------------------------
    // Single question DTO
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class QuestionDto {
        private Long id;
        private QuestionType questionType;
        private String questionText;
        private int sequenceOrder;
        private boolean answered;

        // Only populated after answer is submitted
        private String userAnswer;
        private Integer aiScore;
        private String aiFeedback;
        private String idealAnswer;
        private Instant answeredAt;
    }

    // ----------------------------------------------------------------
    // Answer evaluation result (returned after submitting answer)
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class AnswerResultDto {
        private Long questionId;
        private int aiScore;
        private String aiFeedback;
        private String idealAnswer;
        private BigDecimal sessionOverallScore;
        private int answeredCount;
        private int totalQuestions;
        private boolean sessionCompleted;
    }
}
