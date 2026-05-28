package com.careerpilot.domain.interview.mapper;

import com.careerpilot.domain.interview.dto.InterviewDtos;
import com.careerpilot.domain.interview.entity.InterviewQuestion;
import com.careerpilot.domain.interview.entity.MockInterviewSession;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InterviewMapper {

    public InterviewDtos.SessionSummaryDto toSummaryDto(MockInterviewSession session) {
        return InterviewDtos.SessionSummaryDto.builder()
                .id(session.getId())
                .jobApplicationId(session.getJobApplication().getId())
                .company(session.getJobApplication().getCompany())
                .roleTitle(session.getJobApplication().getRoleTitle())
                .status(session.getStatus())
                .overallScore(session.getOverallScore())
                .totalQuestions(session.getTotalQuestions())
                .answeredCount(session.getAnsweredCount())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    public InterviewDtos.SessionDetailDto toDetailDto(
            MockInterviewSession session,
            List<InterviewQuestion> questions
    ) {
        return InterviewDtos.SessionDetailDto.builder()
                .id(session.getId())
                .jobApplicationId(session.getJobApplication().getId())
                .company(session.getJobApplication().getCompany())
                .roleTitle(session.getJobApplication().getRoleTitle())
                .status(session.getStatus())
                .overallScore(session.getOverallScore())
                .totalQuestions(session.getTotalQuestions())
                .answeredCount(session.getAnsweredCount())
                .questions(questions.stream().map(this::toQuestionDto).toList())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    public InterviewDtos.QuestionDto toQuestionDto(InterviewQuestion q) {
        return InterviewDtos.QuestionDto.builder()
                .id(q.getId())
                .questionType(q.getQuestionType())
                .questionText(q.getQuestionText())
                .sequenceOrder(q.getSequenceOrder())
                .answered(q.getUserAnswer() != null)
                .userAnswer(q.getUserAnswer())
                .aiScore(q.getAiScore())
                .aiFeedback(q.getAiFeedback())
                .idealAnswer(q.getIdealAnswer())
                .answeredAt(q.getAnsweredAt())
                .build();
    }
}
