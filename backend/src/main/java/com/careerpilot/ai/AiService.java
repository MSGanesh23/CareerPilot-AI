package com.careerpilot.ai;

import com.careerpilot.ai.dto.AiDtos;

/**
 * Core AI service abstraction.
 * <p>
 * Any LLM provider (Claude, OpenAI, Gemini, etc.) must implement this interface.
 * The implementation is selected via app.ai.provider config.
 * All methods are synchronous and return structured DTOs.
 * Raw AI responses are also stored for debugging/auditing.
 */
public interface AiService {

    /**
     * Analyze resume text against a job description.
     * Returns match score, skill gaps, strong skills, and improvement suggestions.
     */
    AiDtos.ResumeAnalysisResult analyzeResumeVsJob(String resumeText, String jobDescription);

    /**
     * Generate structured interview questions for a given job description and role.
     */
    AiDtos.InterviewQuestionsResult generateInterviewQuestions(
            String jobDescription,
            String roleTitle,
            int technicalCount,
            int behavioralCount,
            int projectCount
    );

    /**
     * Evaluate a candidate's answer to an interview question.
     * Returns score (1-10), feedback, and an ideal answer sample.
     */
    AiDtos.AnswerEvaluationResult evaluateAnswer(
            String question,
            String questionType,
            String userAnswer,
            String jobDescription
    );
}
