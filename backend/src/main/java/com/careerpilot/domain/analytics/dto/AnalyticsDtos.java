package com.careerpilot.domain.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

public final class AnalyticsDtos {

    private AnalyticsDtos() {}

    // ----------------------------------------------------------------
    // Main dashboard response — everything in one call
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class DashboardDto {
        private ApplicationStatsDto applicationStats;
        private InterviewStatsDto interviewStats;
        private List<SkillFrequencyDto> topMissingSkills;
        private List<ApplicationTrendDto> applicationTrend;
        private List<ScoreTrendDto> scoreTrend;
    }

    // ----------------------------------------------------------------
    // Job application stats
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class ApplicationStatsDto {
        private long totalApplications;
        private long applied;
        private long interviewing;
        private long offers;
        private long rejections;
        private long withdrawn;
        private double offerRate;        // offers / total * 100
        private double interviewRate;    // interviewing / total * 100
        private Double averageMatchScore; // avg AI match score across all jobs
    }

    // ----------------------------------------------------------------
    // Mock interview stats
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class InterviewStatsDto {
        private long totalSessions;
        private long completedSessions;
        private long inProgressSessions;
        private Double averageScore;     // avg overall score across completed sessions
        private long totalQuestionsAnswered;
    }

    // ----------------------------------------------------------------
    // Most frequently missing skill
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class SkillFrequencyDto {
        private String skill;
        private long count;
    }

    // ----------------------------------------------------------------
    // Application count per month (for trend chart)
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class ApplicationTrendDto {
        private String month;   // e.g. "2024-11"
        private long count;
    }

    // ----------------------------------------------------------------
    // Average mock interview score per month (for trend chart)
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class ScoreTrendDto {
        private String month;
        private double averageScore;
    }
}
