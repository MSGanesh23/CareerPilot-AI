package com.careerpilot.domain.job.dto;

import com.careerpilot.domain.job.entity.ApplicationStatus;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class JobDtos {

    private JobDtos() {}

    // ----------------------------------------------------------------
    // Create new job application
    // ----------------------------------------------------------------
    @Data
    public static class CreateJobRequest {

        @NotBlank(message = "Company name is required")
        @Size(max = 255)
        private String company;

        @NotBlank(message = "Role title is required")
        @Size(max = 255)
        private String roleTitle;

        @NotBlank(message = "Job description is required")
        @Size(min = 50, message = "Job description should be at least 50 characters for meaningful AI analysis")
        private String jobDescription;

        @Size(max = 255)
        private String location;

        @Size(max = 1000)
        private String jobUrl;

        @NotNull(message = "Applied date is required")
        private LocalDate appliedDate;

        @Size(max = 5000)
        private String notes;

        private Long resumeId; // Optional — uses active resume if not provided
    }

    // ----------------------------------------------------------------
    // Update job application
    // ----------------------------------------------------------------
    @Data
    public static class UpdateJobRequest {

        @Size(max = 255)
        private String company;

        @Size(max = 255)
        private String roleTitle;

        @Size(min = 50)
        private String jobDescription;

        @Size(max = 255)
        private String location;

        @Size(max = 1000)
        private String jobUrl;

        private ApplicationStatus status;

        private LocalDate appliedDate;

        @Size(max = 5000)
        private String notes;
    }

    // ----------------------------------------------------------------
    // Job application list item (no JD — keep payload small)
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class JobSummaryDto {
        private Long id;
        private String company;
        private String roleTitle;
        private String location;
        private ApplicationStatus status;
        private LocalDate appliedDate;
        private Integer aiMatchScore;
        private boolean hasAnalysis;
        private Instant createdAt;
    }

    // ----------------------------------------------------------------
    // Job application detail (includes JD + analysis)
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class JobDetailDto {
        private Long id;
        private String company;
        private String roleTitle;
        private String jobDescription;
        private String location;
        private String jobUrl;
        private ApplicationStatus status;
        private LocalDate appliedDate;
        private String notes;
        private Integer aiMatchScore;
        private Long resumeId;
        private SkillGapDto skillGapAnalysis;
        private Instant createdAt;
        private Instant updatedAt;
    }

    // ----------------------------------------------------------------
    // Skill gap analysis result DTO
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class SkillGapDto {
        private Long id;
        private int matchScore;
        private List<String> missingSkills;
        private List<String> strongSkills;
        private List<String> improvementSuggestions;
        private Instant createdAt;
    }
}
