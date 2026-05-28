package com.careerpilot.domain.user.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

public final class UserDtos {

    private UserDtos() {}

    // ----------------------------------------------------------------
    // Public profile response
    // ----------------------------------------------------------------
    @Data
    @Builder
    public static class UserProfileDto {
        private Long id;
        private String email;
        private String fullName;
        private Integer yearsExperience;
        private List<String> skills;
        private List<String> targetRoles;
        private String role;
        private Instant createdAt;
    }

    // ----------------------------------------------------------------
    // Update profile request
    // ----------------------------------------------------------------
    @Data
    public static class UpdateProfileRequest {

        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        private String fullName;

        @Min(value = 0, message = "Years of experience cannot be negative")
        @Max(value = 50, message = "Years of experience seems too high")
        private Integer yearsExperience;

        @Size(max = 50, message = "Cannot exceed 50 skills")
        private List<String> skills;

        @Size(max = 20, message = "Cannot exceed 20 target roles")
        private List<String> targetRoles;
    }
}
