package com.careerpilot.domain.analytics.service;

import com.careerpilot.domain.analytics.dto.AnalyticsDtos;
import com.careerpilot.domain.interview.entity.SessionStatus;
import com.careerpilot.domain.interview.repository.InterviewQuestionRepository;
import com.careerpilot.domain.interview.repository.MockInterviewSessionRepository;
import com.careerpilot.domain.job.entity.ApplicationStatus;
import com.careerpilot.domain.job.entity.SkillGapAnalysis;
import com.careerpilot.domain.job.repository.JobApplicationRepository;
import com.careerpilot.domain.job.repository.SkillGapAnalysisRepository;
import com.careerpilot.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JobApplicationRepository jobRepository;
    private final SkillGapAnalysisRepository skillGapRepository;
    private final MockInterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final UserMapper userMapper;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter
            .ofPattern("yyyy-MM")
            .withZone(ZoneOffset.UTC);

    @Transactional(readOnly = true)
    public AnalyticsDtos.DashboardDto getDashboard(Long userId) {
        log.debug("Generating analytics dashboard for userId={}", userId);
        return AnalyticsDtos.DashboardDto.builder()
                .applicationStats(buildApplicationStats(userId))
                .interviewStats(buildInterviewStats(userId))
                .topMissingSkills(buildTopMissingSkills(userId))
                .applicationTrend(buildApplicationTrend(userId))
                .scoreTrend(buildScoreTrend(userId))
                .build();
    }

    private AnalyticsDtos.ApplicationStatsDto buildApplicationStats(Long userId) {
        long total        = jobRepository.countByUserId(userId);
        long applied      = jobRepository.countByUserIdAndStatus(userId, ApplicationStatus.APPLIED);
        long interviewing = jobRepository.countByUserIdAndStatus(userId, ApplicationStatus.INTERVIEWING);
        long offers       = jobRepository.countByUserIdAndStatus(userId, ApplicationStatus.OFFER);
        long rejections   = jobRepository.countByUserIdAndStatus(userId, ApplicationStatus.REJECTED);
        long withdrawn    = jobRepository.countByUserIdAndStatus(userId, ApplicationStatus.WITHDRAWN);

        double offerRate     = total > 0 ? round((double) offers      / total * 100) : 0.0;
        double interviewRate = total > 0 ? round((double) interviewing / total * 100) : 0.0;

        List<SkillGapAnalysis> analyses = skillGapRepository.findAllByUserId(userId);
        OptionalDouble avgMatch = analyses.stream().mapToInt(SkillGapAnalysis::getMatchScore).average();

        return AnalyticsDtos.ApplicationStatsDto.builder()
                .totalApplications(total)
                .applied(applied)
                .interviewing(interviewing)
                .offers(offers)
                .rejections(rejections)
                .withdrawn(withdrawn)
                .offerRate(offerRate)
                .interviewRate(interviewRate)
                .averageMatchScore(avgMatch.isPresent() ? round(avgMatch.getAsDouble()) : null)
                .build();
    }

    private AnalyticsDtos.InterviewStatsDto buildInterviewStats(Long userId) {
        long total       = sessionRepository.countByUserId(userId);
        long completed   = sessionRepository.countByUserIdAndStatus(userId, SessionStatus.COMPLETED);
        long inProgress  = sessionRepository.countByUserIdAndStatus(userId, SessionStatus.IN_PROGRESS);
        Double avgScore  = sessionRepository.findAverageScoreByUserId(userId);
        long totalAnswered = questionRepository.countAnsweredByUserId(userId);

        return AnalyticsDtos.InterviewStatsDto.builder()
                .totalSessions(total)
                .completedSessions(completed)
                .inProgressSessions(inProgress)
                .averageScore(avgScore != null ? round(avgScore) : null)
                .totalQuestionsAnswered(totalAnswered)
                .build();
    }

    private List<AnalyticsDtos.SkillFrequencyDto> buildTopMissingSkills(Long userId) {
        List<SkillGapAnalysis> analyses = skillGapRepository.findAllByUserId(userId);

        Map<String, Long> skillCounts = analyses.stream()
                .flatMap(a -> userMapper.parseJsonList(a.getMissingSkills()).stream())
                .map(s -> s.toLowerCase().trim())
                .filter(s -> !s.isBlank())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        return skillCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> AnalyticsDtos.SkillFrequencyDto.builder()
                        .skill(e.getKey())
                        .count(e.getValue())
                        .build())
                .toList();
    }

    private List<AnalyticsDtos.ApplicationTrendDto> buildApplicationTrend(Long userId) {
        return jobRepository
                .findByUserIdOrderByAppliedDateDesc(userId, Pageable.unpaged())
                .getContent()
                .stream()
                .collect(Collectors.groupingBy(
                        j -> MONTH_FMT.format(j.getAppliedDate().atStartOfDay().toInstant(ZoneOffset.UTC)),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> AnalyticsDtos.ApplicationTrendDto.builder()
                        .month(e.getKey())
                        .count(e.getValue())
                        .build())
                .toList();
    }

    private List<AnalyticsDtos.ScoreTrendDto> buildScoreTrend(Long userId) {
        return sessionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, SessionStatus.COMPLETED, Pageable.unpaged())
                .getContent()
                .stream()
                .filter(s -> s.getOverallScore() != null)
                .collect(Collectors.groupingBy(
                        s -> MONTH_FMT.format(s.getCreatedAt()),
                        Collectors.averagingDouble(s -> s.getOverallScore().doubleValue())
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> AnalyticsDtos.ScoreTrendDto.builder()
                        .month(e.getKey())
                        .averageScore(round(e.getValue()))
                        .build())
                .toList();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
