package com.careerpilot.ai.prompt;

import org.springframework.stereotype.Component;

/**
 * Builds structured, deterministic prompts for each AI task.
 * Prompts demand JSON output to enable reliable parsing.
 * Each method is self-contained and independently testable.
 */
@Component
public class PromptBuilder {

    // ----------------------------------------------------------------
    // 1) Resume vs Job Analysis
    // ----------------------------------------------------------------
    public String buildResumeAnalysisPrompt(String resumeText, String jobDescription) {
        return """
                You are an expert technical recruiter and career coach with 15+ years of experience.
                
                Analyze the candidate's resume against the provided job description.
                Be objective, precise, and constructive.
                
                RESUME TEXT:
                ---
                %s
                ---
                
                JOB DESCRIPTION:
                ---
                %s
                ---
                
                Respond ONLY with a valid JSON object. No markdown, no explanation, no preamble.
                The JSON must match this exact schema:
                
                {
                  "matchScore": <integer 0-100>,
                  "missingSkills": ["skill1", "skill2"],
                  "strongSkills": ["skill1", "skill2"],
                  "improvementSuggestions": [
                    "Specific actionable suggestion 1",
                    "Specific actionable suggestion 2"
                  ]
                }
                
                Rules:
                - matchScore: percentage of job requirements the resume satisfies
                - missingSkills: technical skills and tools mentioned in JD but absent from resume
                - strongSkills: skills present in both resume and JD
                - improvementSuggestions: maximum 5 specific, actionable items
                - Do not invent skills not mentioned in either document
                """.formatted(resumeText, jobDescription);
    }

    // ----------------------------------------------------------------
    // 2) Interview Question Generation
    // ----------------------------------------------------------------
    public String buildInterviewQuestionsPrompt(
            String jobDescription,
            String roleTitle,
            int technicalCount,
            int behavioralCount,
            int projectCount
    ) {
        return """
                You are a senior engineering hiring manager conducting a technical interview.
                
                Generate interview questions for the following role and job description.
                
                ROLE TITLE: %s
                
                JOB DESCRIPTION:
                ---
                %s
                ---
                
                Generate exactly:
                - %d TECHNICAL questions (specific to skills and technologies in the JD)
                - %d BEHAVIORAL questions (using STAR method context, soft skills, teamwork)
                - %d PROJECT questions (deep-dive into past work, architecture decisions)
                
                Respond ONLY with a valid JSON object. No markdown, no explanation, no preamble.
                The JSON must match this exact schema:
                
                {
                  "questions": [
                    {
                      "questionType": "TECHNICAL",
                      "questionText": "...",
                      "sequenceOrder": 1
                    },
                    {
                      "questionType": "BEHAVIORAL",
                      "questionText": "...",
                      "sequenceOrder": 2
                    }
                  ]
                }
                
                Rules:
                - Questions must be specific to the role, not generic
                - TECHNICAL questions must reference actual technologies from the JD
                - BEHAVIORAL questions must be open-ended and situational
                - PROJECT questions must probe depth of experience and decision-making
                - sequenceOrder must be sequential starting from 1
                - Total questions = %d
                """.formatted(
                        roleTitle, jobDescription,
                        technicalCount, behavioralCount, projectCount,
                        technicalCount + behavioralCount + projectCount
                );
    }

    // ----------------------------------------------------------------
    // 3) Answer Evaluation & Scoring
    // ----------------------------------------------------------------
    public String buildAnswerEvaluationPrompt(
            String question,
            String questionType,
            String userAnswer,
            String jobDescription
    ) {
        return """
                You are an expert technical interviewer evaluating a candidate's response.
                
                QUESTION TYPE: %s
                
                QUESTION: %s
                
                CANDIDATE'S ANSWER:
                ---
                %s
                ---
                
                CONTEXT (Job Description excerpt):
                ---
                %s
                ---
                
                Evaluate the answer strictly and fairly. Consider:
                - Technical accuracy and depth
                - Clarity and communication
                - Completeness
                - Relevance to the question
                
                Respond ONLY with a valid JSON object. No markdown, no explanation, no preamble.
                The JSON must match this exact schema:
                
                {
                  "score": <integer 1-10>,
                  "feedback": "Detailed constructive feedback explaining the score. Highlight strengths and gaps.",
                  "idealAnswer": "A comprehensive model answer demonstrating the ideal response to this question."
                }
                
                Rules:
                - score 1-3: poor (missing key points, incorrect)
                - score 4-6: adequate (correct but shallow or incomplete)
                - score 7-9: good (accurate, clear, demonstrates depth)
                - score 10: exceptional (complete, insightful, production-ready thinking)
                - feedback must be specific, reference the candidate's actual answer
                - idealAnswer must be substantive and demonstrate expert-level knowledge
                """.formatted(questionType, question, userAnswer, jobDescription);
    }
}
