package com.careerpilot.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public final class AiDtos {

    private AiDtos() {}

    // ----------------------------------------------------------------
    // Resume vs Job Analysis
    // ----------------------------------------------------------------
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeAnalysisResult {
        private int matchScore;                      // 0-100
        private List<String> missingSkills;
        private List<String> strongSkills;
        private List<String> improvementSuggestions;
        private String rawAiResponse;               // Full raw response for audit
    }

    // ----------------------------------------------------------------
    // Interview Question Generation
    // ----------------------------------------------------------------
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewQuestionsResult {
        private List<GeneratedQuestion> questions;
        private String rawAiResponse;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedQuestion {
        private String questionType;  // TECHNICAL | BEHAVIORAL | PROJECT
        private String questionText;
        private int sequenceOrder;
    }

    // ----------------------------------------------------------------
    // Answer Evaluation
    // ----------------------------------------------------------------
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerEvaluationResult {
        private int score;           // 1-10
        private String feedback;
        private String idealAnswer;
        private String rawAiResponse;
    }
}
