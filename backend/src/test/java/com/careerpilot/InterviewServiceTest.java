package com.careerpilot.domain.interview;

import com.careerpilot.ai.AiService;
import com.careerpilot.ai.dto.AiDtos;
import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.domain.interview.dto.InterviewDtos;
import com.careerpilot.domain.interview.entity.*;
import com.careerpilot.domain.interview.mapper.InterviewMapper;
import com.careerpilot.domain.interview.repository.InterviewQuestionRepository;
import com.careerpilot.domain.interview.repository.MockInterviewSessionRepository;
import com.careerpilot.domain.interview.service.InterviewService;
import com.careerpilot.domain.job.entity.JobApplication;
import com.careerpilot.domain.job.repository.JobApplicationRepository;
import com.careerpilot.domain.user.entity.User;
import com.careerpilot.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewService Unit Tests")
class InterviewServiceTest {

    @Mock private MockInterviewSessionRepository sessionRepository;
    @Mock private InterviewQuestionRepository questionRepository;
    @Mock private JobApplicationRepository jobRepository;
    @Mock private UserService userService;
    @Mock private AiService aiService;
    @Mock private InterviewMapper interviewMapper;

    @InjectMocks
    private InterviewService interviewService;

    private User testUser;
    private JobApplication testJob;
    private MockInterviewSession testSession;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).email("test@example.com")
                .fullName("Test User").role("USER").active(true).build();

        testJob = JobApplication.builder()
                .id(10L).user(testUser)
                .company("Acme Corp").roleTitle("Backend Engineer")
                .jobDescription("We need a Java developer with Spring Boot and MySQL experience.")
                .appliedDate(LocalDate.now())
                .build();

        testSession = MockInterviewSession.builder()
                .id(100L).user(testUser).jobApplication(testJob)
                .status(SessionStatus.IN_PROGRESS)
                .totalQuestions(3).answeredCount(0)
                .build();
    }

    @Test
    @DisplayName("submitAnswer - should throw when session is already completed")
    void submitAnswer_shouldThrow_whenSessionIsCompleted() {
        testSession.setStatus(SessionStatus.COMPLETED);
        when(sessionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(testSession));

        InterviewDtos.SubmitAnswerRequest request = new InterviewDtos.SubmitAnswerRequest();
        request.setAnswer("My answer here");

        assertThatThrownBy(() ->
                interviewService.submitAnswer(1L, 100L, 1L, request)
        ).isInstanceOf(BadRequestException.class)
         .hasMessageContaining("already completed");
    }

    @Test
    @DisplayName("submitAnswer - should evaluate answer and auto-complete when all answered")
    void submitAnswer_shouldAutoComplete_whenAllQuestionsAnswered() {
        testSession.setTotalQuestions(1);
        testSession.setAnsweredCount(0);

        InterviewQuestion question = InterviewQuestion.builder()
                .id(1L).session(testSession)
                .questionType(QuestionType.TECHNICAL)
                .questionText("Explain SOLID principles")
                .sequenceOrder(1).build();

        when(sessionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(testSession));
        when(questionRepository.findByIdAndSessionIdAndUserId(1L, 100L, 1L))
                .thenReturn(Optional.of(question));

        AiDtos.AnswerEvaluationResult evalResult = AiDtos.AnswerEvaluationResult.builder()
                .score(8).feedback("Good answer").idealAnswer("Ideal answer here").build();
        when(aiService.evaluateAnswer(any(), any(), any(), any())).thenReturn(evalResult);

        // After save, return a list with the scored question for recalculation
        InterviewQuestion scoredQuestion = InterviewQuestion.builder()
                .id(1L).session(testSession).aiScore(8).build();
        when(questionRepository.findBySessionIdOrderBySequenceOrderAsc(100L))
                .thenReturn(List.of(scoredQuestion));
        when(sessionRepository.save(any())).thenReturn(testSession);
        when(questionRepository.save(any())).thenReturn(question);

        InterviewDtos.SubmitAnswerRequest request = new InterviewDtos.SubmitAnswerRequest();
        request.setAnswer("SOLID stands for Single Responsibility, Open/Closed...");

        InterviewDtos.AnswerResultDto result = interviewService.submitAnswer(1L, 100L, 1L, request);

        assertThat(result.getAiScore()).isEqualTo(8);
        assertThat(result.isSessionCompleted()).isTrue();
        assertThat(result.getAnsweredCount()).isEqualTo(1);
        verify(sessionRepository, times(1)).save(argThat(s -> s.getStatus() == SessionStatus.COMPLETED));
    }

    @Test
    @DisplayName("abandonSession - should throw when session is already completed")
    void abandonSession_shouldThrow_whenNotInProgress() {
        testSession.setStatus(SessionStatus.COMPLETED);
        when(sessionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(testSession));

        assertThatThrownBy(() ->
                interviewService.abandonSession(1L, 100L)
        ).isInstanceOf(BadRequestException.class)
         .hasMessageContaining("abandoned");
    }
}
