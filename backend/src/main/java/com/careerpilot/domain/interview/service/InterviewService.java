package com.careerpilot.domain.interview.service;

import com.careerpilot.ai.AiService;
import com.careerpilot.ai.dto.AiDtos;
import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.common.exception.ResourceNotFoundException;
import com.careerpilot.common.response.PagedResponse;
import com.careerpilot.domain.interview.dto.InterviewDtos;
import com.careerpilot.domain.interview.entity.*;
import com.careerpilot.domain.interview.mapper.InterviewMapper;
import com.careerpilot.domain.interview.repository.InterviewQuestionRepository;
import com.careerpilot.domain.interview.repository.MockInterviewSessionRepository;
import com.careerpilot.domain.job.entity.JobApplication;
import com.careerpilot.domain.job.repository.JobApplicationRepository;
import com.careerpilot.domain.user.entity.User;
import com.careerpilot.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final MockInterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final JobApplicationRepository jobRepository;
    private final UserService userService;
    private final AiService aiService;
    private final InterviewMapper interviewMapper;

    // ----------------------------------------------------------------
    // Start a new mock interview session
    // ----------------------------------------------------------------
    @Transactional
    public InterviewDtos.SessionDetailDto startSession(
            Long userId,
            InterviewDtos.StartSessionRequest request
    ) {
        User user = userService.findUserById(userId);
        JobApplication job = findJobForUser(userId, request.getJobApplicationId());

        MockInterviewSession session = MockInterviewSession.builder()
                .user(user)
                .jobApplication(job)
                .status(SessionStatus.IN_PROGRESS)
                .build();

        sessionRepository.save(session);
        log.info("Interview session started: sessionId={}, userId={}, jobId={}",
                session.getId(), userId, job.getId());

        // Generate questions via AI
        AiDtos.InterviewQuestionsResult aiResult = aiService.generateInterviewQuestions(
                job.getJobDescription(),
                job.getRoleTitle(),
                request.getTechnicalCount(),
                request.getBehavioralCount(),
                request.getProjectCount()
        );

        // Persist all generated questions
        List<InterviewQuestion> questions = new ArrayList<>();
        for (AiDtos.GeneratedQuestion gq : aiResult.getQuestions()) {
            InterviewQuestion question = InterviewQuestion.builder()
                    .session(session)
                    .questionType(QuestionType.valueOf(gq.getQuestionType()))
                    .questionText(gq.getQuestionText())
                    .sequenceOrder(gq.getSequenceOrder())
                    .build();
            questions.add(question);
        }

        questionRepository.saveAll(questions);

        session.setTotalQuestions(questions.size());
        sessionRepository.save(session);

        log.info("Generated {} questions for sessionId={}", questions.size(), session.getId());
        return interviewMapper.toDetailDto(session, questions);
    }

    // ----------------------------------------------------------------
    // List sessions for a user
    // ----------------------------------------------------------------
    @Transactional(readOnly = true)
    public PagedResponse<InterviewDtos.SessionSummaryDto> listSessions(
            Long userId, SessionStatus statusFilter, Pageable pageable
    ) {
        Page<InterviewDtos.SessionSummaryDto> page;
        if (statusFilter != null) {
            page = sessionRepository
                    .findByUserIdAndStatusOrderByCreatedAtDesc(userId, statusFilter, pageable)
                    .map(interviewMapper::toSummaryDto);
        } else {
            page = sessionRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                    .map(interviewMapper::toSummaryDto);
        }
        return PagedResponse.from(page);
    }

    // ----------------------------------------------------------------
    // Get session detail with all questions
    // ----------------------------------------------------------------
    @Transactional(readOnly = true)
    public InterviewDtos.SessionDetailDto getSession(Long userId, Long sessionId) {
        MockInterviewSession session = findSessionForUser(userId, sessionId);
        List<InterviewQuestion> questions =
                questionRepository.findBySessionIdOrderBySequenceOrderAsc(sessionId);
        return interviewMapper.toDetailDto(session, questions);
    }

    // ----------------------------------------------------------------
    // Submit answer to a question and get AI evaluation
    // ----------------------------------------------------------------
    @Transactional
    public InterviewDtos.AnswerResultDto submitAnswer(
            Long userId,
            Long sessionId,
            Long questionId,
            InterviewDtos.SubmitAnswerRequest request
    ) {
        MockInterviewSession session = findSessionForUser(userId, sessionId);

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new BadRequestException("This interview session is already completed.");
        }
        if (session.getStatus() == SessionStatus.ABANDONED) {
            throw new BadRequestException("This interview session has been abandoned.");
        }

        InterviewQuestion question = questionRepository
                .findByIdAndSessionIdAndUserId(questionId, sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        if (question.getUserAnswer() != null) {
            throw new BadRequestException("This question has already been answered.");
        }

        // Evaluate answer with AI
        AiDtos.AnswerEvaluationResult evaluation = aiService.evaluateAnswer(
                question.getQuestionText(),
                question.getQuestionType().name(),
                request.getAnswer(),
                session.getJobApplication().getJobDescription()
        );

        // Persist the answer and AI feedback
        question.setUserAnswer(request.getAnswer());
        question.setAiScore(evaluation.getScore());
        question.setAiFeedback(evaluation.getFeedback());
        question.setIdealAnswer(evaluation.getIdealAnswer());
        question.setAnsweredAt(Instant.now());
        questionRepository.save(question);

        // Update session counters and recompute overall score
        session.setAnsweredCount(session.getAnsweredCount() + 1);
        BigDecimal newOverallScore = recalculateOverallScore(sessionId, session.getAnsweredCount());
        session.setOverallScore(newOverallScore);

        // Auto-complete session if all questions are answered
        boolean sessionCompleted = session.getAnsweredCount() >= session.getTotalQuestions();
        if (sessionCompleted) {
            session.setStatus(SessionStatus.COMPLETED);
            log.info("Interview session completed: sessionId={}, overallScore={}", sessionId, newOverallScore);
        }

        sessionRepository.save(session);

        return InterviewDtos.AnswerResultDto.builder()
                .questionId(questionId)
                .aiScore(evaluation.getScore())
                .aiFeedback(evaluation.getFeedback())
                .idealAnswer(evaluation.getIdealAnswer())
                .sessionOverallScore(newOverallScore)
                .answeredCount(session.getAnsweredCount())
                .totalQuestions(session.getTotalQuestions())
                .sessionCompleted(sessionCompleted)
                .build();
    }

    // ----------------------------------------------------------------
    // Abandon a session
    // ----------------------------------------------------------------
    @Transactional
    public void abandonSession(Long userId, Long sessionId) {
        MockInterviewSession session = findSessionForUser(userId, sessionId);
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException("Only IN_PROGRESS sessions can be abandoned.");
        }
        session.setStatus(SessionStatus.ABANDONED);
        sessionRepository.save(session);
        log.info("Interview session abandoned: sessionId={}", sessionId);
    }

    // ----------------------------------------------------------------
    // Delete a session
    // ----------------------------------------------------------------
    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        MockInterviewSession session = findSessionForUser(userId, sessionId);
        sessionRepository.delete(session);
        log.info("Interview session deleted: sessionId={}, userId={}", sessionId, userId);
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------
    private BigDecimal recalculateOverallScore(Long sessionId, int answeredCount) {
        List<InterviewQuestion> answered = questionRepository
                .findBySessionIdOrderBySequenceOrderAsc(sessionId)
                .stream()
                .filter(q -> q.getAiScore() != null)
                .toList();

        if (answered.isEmpty()) return null;

        double avg = answered.stream()
                .mapToInt(InterviewQuestion::getAiScore)
                .average()
                .orElse(0.0);

        return BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
    }

    private MockInterviewSession findSessionForUser(Long userId, Long sessionId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("InterviewSession", "id", sessionId));
    }

    private JobApplication findJobForUser(Long userId, Long jobId) {
        return jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "id", jobId));
    }
}
