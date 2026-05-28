package com.careerpilot.domain.resume.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

public final class ResumeDtos {

    private ResumeDtos() {}

    // ----------------------------------------------------------------
    // Resume summary (list view)
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class ResumeSummaryDto {
        private Long id;
        private String fileName;
        private String label;
        private int version;
        private boolean active;
        private Long fileSize;
        private String contentType;
        private Instant createdAt;
        private boolean hasParsedText;
    }

    // ----------------------------------------------------------------
    // Resume detail (includes parsed text)
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class ResumeDetailDto {
        private Long id;
        private String fileName;
        private String label;
        private int version;
        private boolean active;
        private Long fileSize;
        private String contentType;
        private String parsedText;
        private Instant createdAt;
        private Instant updatedAt;
    }

    // ----------------------------------------------------------------
    // Upload metadata (multipart form fields alongside the file)
    // ----------------------------------------------------------------
    @Data
    public static class UploadResumeRequest {

        @Size(max = 100, message = "Label cannot exceed 100 characters")
        private String label;

        private boolean setAsActive = true;
    }

    // ----------------------------------------------------------------
    // Update label / active flag
    // ----------------------------------------------------------------
    @Data
    public static class UpdateResumeRequest {

        @Size(max = 100, message = "Label cannot exceed 100 characters")
        private String label;

        private Boolean setAsActive;
    }
}
