package com.careerpilot.domain.user.controller;

import com.careerpilot.common.response.ApiResponse;
import com.careerpilot.domain.user.dto.UserDtos;
import com.careerpilot.domain.user.service.UserService;
import com.careerpilot.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Manage user profile")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user's profile")
    public ResponseEntity<ApiResponse<UserDtos.UserProfileDto>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(userService.getProfile(principal.getId()))
        );
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user's profile")
    public ResponseEntity<ApiResponse<UserDtos.UserProfileDto>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserDtos.UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok("Profile updated successfully",
                        userService.updateProfile(principal.getId(), request))
        );
    }
}
