package com.careerpilot;

import com.careerpilot.domain.analytics.dto.AnalyticsDtos;
import com.careerpilot.domain.analytics.service.AnalyticsService;
import com.careerpilot.domain.interview.entity.MockInterviewSession;
import com.careerpilot.domain.interview.entity.SessionStatus;
import com.careerpilot.domain.interview.repository.InterviewQuestionRepository;
import com.careerpilot.domain.interview.repository.MockInterviewSessionRepository;
import com.careerpilot.domain.job.entity.ApplicationStatus;
import com.careerpilot.domain.job.entity.JobApplication;
import com.careerpilot.domain.job.entity.SkillGapAnalysis;
import com.careerpilot.domain.job.repository.JobApplicationRepository;
import com.careerpilot.domain.job.repository.SkillGapAnalysisRepository;
import com.careerpilot.domain.user.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService Unit Tests")
class AnalyticsServiceTest {

    @Mock private JobApplicationRepository jobRepository;
    @Mock private SkillGapAnalysisRepository skillGapRepository;
    @Mock private MockInterviewSessionRepository sessionRepository;
    @Mock private InterviewQuestionRepository questionRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("getDashboard - should compute correct offer and interview rates")
    void getDashboard_shouldComputeCorrectRates() {
        // Arrange: 10 total, 3 offers, 4 interviewing
        when(jobRepository.countByUserId(1L)).thenReturn(10L);
        when(jobRepository.countByUserIdAndStatus(1L, ApplicationStatus.APPLIED)).thenReturn(2L);
        when(jobRepository.countByUserIdAndStatus(1L, ApplicationStatus.INTERVIEWING)).thenReturn(4L);
        when(jobRepository.countByUserIdAndStatus(1L, ApplicationStatus.OFFER)).thenReturn(3L);
        when(jobRepository.countByUserIdAndStatus(1L, ApplicationStatus.REJECTED)).thenReturn(1L);
        when(jobRepository.countByUserIdAndStatus(1L, ApplicationStatus.WITHDRAWN)).thenReturn(0L);

        when(skillGapRepository.findAllByUserId(1L)).thenReturn(List.of());
        when(sessionRepository.countByUserId(1L)).thenReturn(0L);
        when(sessionRepository.countByUserIdAndStatus(1L, SessionStatus.COMPLETED)).thenReturn(0L);
        when(sessionRepository.countByUserIdAndStatus(1L, SessionStatus.IN_PROGRESS)).thenReturn(0L);
        when(sessionRepository.findAverageScoreByUserId(1L)).thenReturn(null);
        when(questionRepository.countAnsweredByUserId(1L)).thenReturn(0L);
        when(jobRepository.findByUserIdOrderByAppliedDateDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(sessionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                eq(1L), eq(SessionStatus.COMPLETED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        AnalyticsDtos.DashboardDto dashboard = analyticsService.getDashboard(1L);

        // Assert
        AnalyticsDtos.ApplicationStatsDto stats = dashboard.getApplicationStats();
        assertThat(stats.getTotalApplications()).isEqualTo(10L);
        assertThat(stats.getOfferRate()).isEqualTo(30.0);       // 3/10 * 100
        assertThat(stats.getInterviewRate()).isEqualTo(40.0);   // 4/10 * 100
        assertThat(stats.getOffers()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getDashboard - should aggregate top missing skills correctly")
    void getDashboard_shouldAggregateTopMissingSkillsCorrectly() {
        // Arrange
        when(jobRepository.countByUserId(1L)).thenReturn(0L);
        when(jobRepository.countByUserIdAndStatus(any(), any())).thenReturn(0L);
        when(sessionRepository.countByUserId(1L)).thenReturn(0L);
        when(sessionRepository.countByUserIdAndStatus(any(), any())).thenReturn(0L);
        when(sessionRepository.findAverageScoreByUserId(1L)).thenReturn(null);
        when(questionRepository.countAnsweredByUserId(1L)).thenReturn(0L);
        when(jobRepository.findByUserIdOrderByAppliedDateDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(sessionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // Two analyses — kubernetes appears twice, kafka once
        SkillGapAnalysis a1 = new SkillGapAnalysis();
        a1.setMissingSkills("[\"kubernetes\",\"kafka\"]");
        SkillGapAnalysis a2 = new SkillGapAnalysis();
        a2.setMissingSkills("[\"kubernetes\",\"grpc\"]");

        when(skillGapRepository.findAllByUserId(1L)).thenReturn(List.of(a1, a2));
        when(userMapper.parseJsonList("[\"kubernetes\",\"kafka\"]")).thenReturn(List.of("kubernetes", "kafka"));
        when(userMapper.parseJsonList("[\"kubernetes\",\"grpc\"]")).thenReturn(List.of("kubernetes", "grpc"));

        // Act
        AnalyticsDtos.DashboardDto dashboard = analyticsService.getDashboard(1L);

        // Assert — kubernetes should be first with count=2
        List<AnalyticsDtos.SkillFrequencyDto> skills = dashboard.getTopMissingSkills();
        assertThat(skills).isNotEmpty();
        assertThat(skills.get(0).getSkill()).isEqualTo("kubernetes");
        assertThat(skills.get(0).getCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getDashboard - should return averageMatchScore from skill gap analyses")
    void getDashboard_shouldComputeAverageMatchScore() {
        when(jobRepository.countByUserId(1L)).thenReturn(2L);
        when(jobRepository.countByUserIdAndStatus(any(), any())).thenReturn(0L);
        when(sessionRepository.countByUserId(1L)).thenReturn(0L);
        when(sessionRepository.countByUserIdAndStatus(any(), any())).thenReturn(0L);
        when(sessionRepository.findAverageScoreByUserId(1L)).thenReturn(null);
        when(questionRepository.countAnsweredByUserId(1L)).thenReturn(0L);
        when(jobRepository.findByUserIdOrderByAppliedDateDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(sessionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        SkillGapAnalysis a1 = new SkillGapAnalysis();
        a1.setMatchScore(80);
        a1.setMissingSkills("[]");
        SkillGapAnalysis a2 = new SkillGapAnalysis();
        a2.setMatchScore(60);
        a2.setMissingSkills("[]");

        when(skillGapRepository.findAllByUserId(1L)).thenReturn(List.of(a1, a2));
        when(userMapper.parseJsonList("[]")).thenReturn(List.of());

        AnalyticsDtos.DashboardDto dashboard = analyticsService.getDashboard(1L);

        assertThat(dashboard.getApplicationStats().getAverageMatchScore()).isEqualTo(70.0);
    }
}
