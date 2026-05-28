package com.careerpilot.ai.impl;

import com.careerpilot.ai.AiService;
import com.careerpilot.ai.dto.AiDtos;
import com.careerpilot.ai.prompt.PromptBuilder;
import com.careerpilot.common.exception.AiProcessingException;
import com.careerpilot.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "claude")
@RequiredArgsConstructor
public class ClaudeAiService implements AiService {

    private final AppProperties appProperties;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    private static final String ANTHROPIC_VERSION_HEADER = "anthropic-version";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    // ----------------------------------------------------------------
    // Resume Analysis
    // ----------------------------------------------------------------
    @Override
    public AiDtos.ResumeAnalysisResult analyzeResumeVsJob(String resumeText, String jobDescription) {
        String prompt = promptBuilder.buildResumeAnalysisPrompt(resumeText, jobDescription);
        String rawResponse = callClaudeApi(prompt);

        try {
            JsonNode json = objectMapper.readTree(rawResponse);
            return AiDtos.ResumeAnalysisResult.builder()
                    .matchScore(json.get("matchScore").asInt())
                    .missingSkills(parseStringList(json.get("missingSkills")))
                    .strongSkills(parseStringList(json.get("strongSkills")))
                    .improvementSuggestions(parseStringList(json.get("improvementSuggestions")))
                    .rawAiResponse(rawResponse)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse resume analysis response: {}", rawResponse, e);
            throw new AiProcessingException("Failed to parse resume analysis from AI", e);
        }
    }

    // ----------------------------------------------------------------
    // Interview Question Generation
    // ----------------------------------------------------------------
    @Override
    public AiDtos.InterviewQuestionsResult generateInterviewQuestions(
            String jobDescription,
            String roleTitle,
            int technicalCount,
            int behavioralCount,
            int projectCount
    ) {
        String prompt = promptBuilder.buildInterviewQuestionsPrompt(
                jobDescription, roleTitle, technicalCount, behavioralCount, projectCount
        );
        String rawResponse = callClaudeApi(prompt);

        try {
            JsonNode json = objectMapper.readTree(rawResponse);
            JsonNode questionsNode = json.get("questions");
            List<AiDtos.GeneratedQuestion> questions = new ArrayList<>();

            for (JsonNode q : questionsNode) {
                questions.add(AiDtos.GeneratedQuestion.builder()
                        .questionType(q.get("questionType").asText())
                        .questionText(q.get("questionText").asText())
                        .sequenceOrder(q.get("sequenceOrder").asInt())
                        .build());
            }

            return AiDtos.InterviewQuestionsResult.builder()
                    .questions(questions)
                    .rawAiResponse(rawResponse)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse interview questions response: {}", rawResponse, e);
            throw new AiProcessingException("Failed to parse interview questions from AI", e);
        }
    }

    // ----------------------------------------------------------------
    // Answer Evaluation
    // ----------------------------------------------------------------
    @Override
    public AiDtos.AnswerEvaluationResult evaluateAnswer(
            String question,
            String questionType,
            String userAnswer,
            String jobDescription
    ) {
        String prompt = promptBuilder.buildAnswerEvaluationPrompt(
                question, questionType, userAnswer, jobDescription
        );
        String rawResponse = callClaudeApi(prompt);

        try {
            JsonNode json = objectMapper.readTree(rawResponse);
            return AiDtos.AnswerEvaluationResult.builder()
                    .score(json.get("score").asInt())
                    .feedback(json.get("feedback").asText())
                    .idealAnswer(json.get("idealAnswer").asText())
                    .rawAiResponse(rawResponse)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse answer evaluation response: {}", rawResponse, e);
            throw new AiProcessingException("Failed to parse answer evaluation from AI", e);
        }
    }

    // ----------------------------------------------------------------
    // Internal: Call Claude Anthropic API
    // ----------------------------------------------------------------
    private String callClaudeApi(String prompt) {
        AppProperties.Ai aiConfig = appProperties.getAi();

        Map<String, Object> requestBody = Map.of(
                "model", aiConfig.getModel(),
                "max_tokens", aiConfig.getMaxTokens(),
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        try {
            log.debug("Calling Claude API with model={}", aiConfig.getModel());

            WebClient client = WebClient.builder()
                    .baseUrl(aiConfig.getBaseUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiConfig.getApiKey())
                    .defaultHeader(ANTHROPIC_VERSION_HEADER, ANTHROPIC_VERSION)
                    .build();

            Map<?, ?> response = client.post()
                    .uri("/v1/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds()))
                    .block();

            // Extract text from Claude's response structure
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            String rawText = (String) content.get(0).get("text");

            // Strip any markdown code fences if present
            return cleanJsonResponse(rawText);

        } catch (WebClientResponseException e) {
            log.error("Claude API error: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiProcessingException("Claude API returned error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling Claude API", e);
            throw new AiProcessingException("Failed to communicate with AI service", e);
        }
    }

    private String cleanJsonResponse(String rawText) {
        if (rawText == null) return "{}";
        String cleaned = rawText.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private List<String> parseStringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> result.add(item.asText()));
        }
        return result;
    }
}
