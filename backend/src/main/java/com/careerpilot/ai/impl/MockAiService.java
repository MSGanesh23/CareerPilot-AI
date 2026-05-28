package com.careerpilot.ai.impl;

import com.careerpilot.ai.AiService;
import com.careerpilot.ai.dto.AiDtos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Mock AI service for local development and testing.
 * Returns deterministic responses without calling any external API.
 * Activated via: app.ai.provider=mock
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock")
public class MockAiService implements AiService {

    @Override
    public AiDtos.ResumeAnalysisResult analyzeResumeVsJob(String resumeText, String jobDescription) {
        log.info("[MOCK AI] Analyzing resume vs job description");
        return AiDtos.ResumeAnalysisResult.builder()
                .matchScore(72)
                .missingSkills(List.of("Kubernetes", "Apache Kafka", "gRPC"))
                .strongSkills(List.of("Java", "Spring Boot", "REST APIs", "PostgreSQL", "Docker"))
                .improvementSuggestions(List.of(
                        "Add experience with container orchestration (Kubernetes) to match the JD requirements",
                        "Highlight any event-driven architecture experience to align with Kafka usage",
                        "Quantify your impact — add metrics to project descriptions (e.g. reduced latency by 40%)",
                        "Include system design examples to demonstrate scalability thinking"
                ))
                .rawAiResponse("{\"mock\": true}")
                .build();
    }

    @Override
    public AiDtos.InterviewQuestionsResult generateInterviewQuestions(
            String jobDescription, String roleTitle,
            int technicalCount, int behavioralCount, int projectCount
    ) {
        log.info("[MOCK AI] Generating interview questions for role={}", roleTitle);
        return AiDtos.InterviewQuestionsResult.builder()
                .questions(List.of(
                        AiDtos.GeneratedQuestion.builder()
                                .questionType("TECHNICAL")
                                .questionText("Explain the difference between optimistic and pessimistic locking in a relational database. When would you use each?")
                                .sequenceOrder(1).build(),
                        AiDtos.GeneratedQuestion.builder()
                                .questionType("TECHNICAL")
                                .questionText("How would you design a rate limiter for a high-traffic REST API?")
                                .sequenceOrder(2).build(),
                        AiDtos.GeneratedQuestion.builder()
                                .questionType("BEHAVIORAL")
                                .questionText("Tell me about a time you disagreed with a technical decision made by your team. How did you handle it?")
                                .sequenceOrder(3).build(),
                        AiDtos.GeneratedQuestion.builder()
                                .questionType("BEHAVIORAL")
                                .questionText("Describe a situation where you had to deliver under a tight deadline. What trade-offs did you make?")
                                .sequenceOrder(4).build(),
                        AiDtos.GeneratedQuestion.builder()
                                .questionType("PROJECT")
                                .questionText("Walk me through the most complex system you have built. What were the key architectural decisions and what would you do differently?")
                                .sequenceOrder(5).build()
                ))
                .rawAiResponse("{\"mock\": true}")
                .build();
    }

    @Override
    public AiDtos.AnswerEvaluationResult evaluateAnswer(
            String question, String questionType,
            String userAnswer, String jobDescription
    ) {
        log.info("[MOCK AI] Evaluating answer for question type={}", questionType);
        return AiDtos.AnswerEvaluationResult.builder()
                .score(7)
                .feedback("Good answer that demonstrates solid foundational knowledge. You correctly identified the core concepts. To score higher, add a concrete real-world example from your experience and discuss edge cases or trade-offs.")
                .idealAnswer("An ideal answer would begin by defining the concept clearly, then provide a specific use case from production experience, discuss trade-offs, mention any pitfalls or limitations, and conclude with how you made the decision in context.")
                .rawAiResponse("{\"mock\": true}")
                .build();
    }
}
