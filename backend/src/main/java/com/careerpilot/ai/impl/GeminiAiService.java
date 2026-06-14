package com.careerpilot.ai.impl;

import com.careerpilot.ai.AiService;
import com.careerpilot.ai.dto.AiDtos;
import com.careerpilot.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini AI service implementation.
 *
 * Uses the Google AI Studio REST API (free tier):
 *   https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}
 *
 * Key advantages over Claude:
 *  - Free tier: 1,500 requests/day, 15 RPM (Gemini 2.0 Flash)
 *  - Native JSON response schema enforcement (no regex parsing)
 *  - 1M token context window — handles long resumes + JDs easily
 *
 * Activated via: app.ai.provider=gemini
 * Required env var: GEMINI_API_KEY (get from https://aistudio.google.com)
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini")
public class GeminiAiService implements AiService {

    private final AppProperties.Ai aiConfig;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiAiService(AppProperties appProperties, ObjectMapper objectMapper) {
        this.aiConfig = appProperties.getAi();
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(aiConfig.getBaseUrl())
                .build();
        log.info("GeminiAiService initialized — model={}, baseUrl={}",
                aiConfig.getModel(), aiConfig.getBaseUrl());
    }

    // ================================================================
    // 1. Resume vs Job Description Analysis
    // ================================================================
    @Override
    public AiDtos.ResumeAnalysisResult analyzeResumeVsJob(String resumeText, String jobDescription) {
        log.info("Gemini: analyzing resume vs job description");

        String prompt = """
                You are an expert technical recruiter and career coach.
                Analyze the candidate's resume against the job description provided.
                
                RESUME:
                %s
                
                JOB DESCRIPTION:
                %s
                
                Provide a detailed analysis with:
                - matchScore: integer 0-100 reflecting how well the resume matches the JD
                - missingSkills: skills/technologies in the JD that are absent from the resume
                - strongSkills: skills from the resume that are explicitly required or preferred in the JD
                - improvementSuggestions: specific, actionable suggestions to improve the resume for this role
                """.formatted(resumeText, jobDescription);

        // Response schema enforces structured JSON — no parsing needed
        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "matchScore",              Map.of("type", "INTEGER"),
                        "missingSkills",           Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                        "strongSkills",            Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                        "improvementSuggestions",  Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))
                ),
                "required", List.of("matchScore", "missingSkills", "strongSkills", "improvementSuggestions")
        );

        String rawResponse = callGemini(prompt, responseSchema);

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            return AiDtos.ResumeAnalysisResult.builder()
                    .matchScore(root.path("matchScore").asInt(0))
                    .missingSkills(toStringList(root.path("missingSkills")))
                    .strongSkills(toStringList(root.path("strongSkills")))
                    .improvementSuggestions(toStringList(root.path("improvementSuggestions")))
                    .rawAiResponse(rawResponse)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Gemini resume analysis response: {}", e.getMessage());
            throw new RuntimeException("AI analysis failed: " + e.getMessage(), e);
        }
    }

    // ================================================================
    // 2. Interview Question Generation
    // ================================================================
    @Override
    public AiDtos.InterviewQuestionsResult generateInterviewQuestions(
            String jobDescription, String roleTitle,
            int technicalCount, int behavioralCount, int projectCount
    ) {
        log.info("Gemini: generating interview questions for role={}", roleTitle);

        String prompt = """
                You are a senior technical interviewer with expertise in hiring for software engineering roles.
                Generate interview questions for a candidate applying for: %s
                
                JOB DESCRIPTION:
                %s
                
                Generate exactly:
                - %d TECHNICAL questions (algorithms, system design, or technology-specific questions relevant to the JD)
                - %d BEHAVIORAL questions (leadership, teamwork, conflict resolution, STAR-format questions)
                - %d PROJECT questions (about past work, architecture decisions, impact)
                
                Each question must have:
                - questionType: exactly "TECHNICAL", "BEHAVIORAL", or "PROJECT"
                - questionText: the full question text (specific and relevant to the role)
                - sequenceOrder: sequential integer starting from 1
                """.formatted(roleTitle, jobDescription, technicalCount, behavioralCount, projectCount);

        Map<String, Object> questionSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "questionType",  Map.of("type", "STRING", "enum", List.of("TECHNICAL", "BEHAVIORAL", "PROJECT")),
                        "questionText",  Map.of("type", "STRING"),
                        "sequenceOrder", Map.of("type", "INTEGER")
                ),
                "required", List.of("questionType", "questionText", "sequenceOrder")
        );

        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "questions", Map.of(
                                "type", "ARRAY",
                                "items", questionSchema
                        )
                ),
                "required", List.of("questions")
        );

        String rawResponse = callGemini(prompt, responseSchema);

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            List<AiDtos.GeneratedQuestion> questions = new ArrayList<>();
            for (JsonNode q : root.path("questions")) {
                questions.add(AiDtos.GeneratedQuestion.builder()
                        .questionType(q.path("questionType").asText("TECHNICAL"))
                        .questionText(q.path("questionText").asText())
                        .sequenceOrder(q.path("sequenceOrder").asInt(questions.size() + 1))
                        .build());
            }
            return AiDtos.InterviewQuestionsResult.builder()
                    .questions(questions)
                    .rawAiResponse(rawResponse)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Gemini interview questions response: {}", e.getMessage());
            throw new RuntimeException("AI question generation failed: " + e.getMessage(), e);
        }
    }

    // ================================================================
    // 3. Answer Evaluation
    // ================================================================
    @Override
    public AiDtos.AnswerEvaluationResult evaluateAnswer(
            String question, String questionType,
            String userAnswer, String jobDescription
    ) {
        log.info("Gemini: evaluating answer for questionType={}", questionType);

        String prompt = """
                You are an expert technical interviewer evaluating a candidate's answer.
                
                ROLE CONTEXT (from job description):
                %s
                
                QUESTION TYPE: %s
                QUESTION: %s
                
                CANDIDATE'S ANSWER:
                %s
                
                Evaluate the answer and provide:
                - score: integer 1-10 (1=very poor, 10=exceptional)
                - feedback: specific, constructive feedback on the answer's strengths and weaknesses
                - idealAnswer: a concise example of an excellent answer to this question
                """.formatted(jobDescription, questionType, question, userAnswer);

        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "score",       Map.of("type", "INTEGER"),
                        "feedback",    Map.of("type", "STRING"),
                        "idealAnswer", Map.of("type", "STRING")
                ),
                "required", List.of("score", "feedback", "idealAnswer")
        );

        String rawResponse = callGemini(prompt, responseSchema);

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            return AiDtos.AnswerEvaluationResult.builder()
                    .score(root.path("score").asInt(5))
                    .feedback(root.path("feedback").asText())
                    .idealAnswer(root.path("idealAnswer").asText())
                    .rawAiResponse(rawResponse)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Gemini answer evaluation response: {}", e.getMessage());
            throw new RuntimeException("AI evaluation failed: " + e.getMessage(), e);
        }
    }

    // ================================================================
    // Core Gemini API call — uses response schema for guaranteed JSON
    // ================================================================
    private String callGemini(String prompt, Map<String, Object> responseSchema) {
        String endpoint = String.format(
                "/v1beta/models/%s:generateContent?key=%s",
                aiConfig.getModel(), aiConfig.getApiKey()
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema",   responseSchema,
                        "maxOutputTokens",  aiConfig.getMaxTokens(),
                        "temperature",      0.4   // balanced: creative but consistent
                )
        );

        JsonNode responseNode = webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class).flatMap(body -> {
                            log.error("Gemini 4xx error: {}", body);
                            return Mono.error(new RuntimeException("Gemini API error (4xx): " + body));
                        })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class).flatMap(body -> {
                            log.error("Gemini 5xx error: {}", body);
                            return Mono.error(new RuntimeException("Gemini API error (5xx): " + body));
                        })
                )
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds()))
                .block();

        if (responseNode == null) {
            throw new RuntimeException("Gemini returned null response");
        }

        // Extract text from: candidates[0].content.parts[0].text
        JsonNode textNode = responseNode
                .path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text");

        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            log.error("Unexpected Gemini response structure: {}", responseNode);
            throw new RuntimeException("Gemini response missing text content");
        }

        return textNode.asText();
    }

    // ================================================================
    // Helper — converts a JSON array node to List<String>
    // ================================================================
    private List<String> toStringList(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode.isArray()) {
            arrayNode.forEach(node -> result.add(node.asText()));
        }
        return result;
    }
}
