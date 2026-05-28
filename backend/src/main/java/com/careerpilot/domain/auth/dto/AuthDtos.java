package com.careerpilot.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class AuthDtos {

    private AuthDtos() {}

    // ----------------------------------------------------------------
    // Register
    // ----------------------------------------------------------------
    @Data
    public static class RegisterRequest {

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        private String fullName;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
        private String password;
    }

    // ----------------------------------------------------------------
    // Login
    // ----------------------------------------------------------------
    @Data
    public static class LoginRequest {

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    // ----------------------------------------------------------------
    // Auth Response (token + basic user info)
    // ----------------------------------------------------------------
    @Data
    public static class AuthResponse {
        private final String accessToken;
        private final String tokenType = "Bearer";
        private final Long userId;
        private final String email;
        private final String fullName;
        private final String role;
    }
}
