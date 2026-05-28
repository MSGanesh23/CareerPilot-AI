package com.careerpilot.domain.analytics.controller;

import com.careerpilot.common.response.ApiResponse;
import com.careerpilot.domain.analytics.dto.AnalyticsDtos;
import com.careerpilot.domain.analytics.service.AnalyticsService;
import com.careerpilot.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Performance trends and career dashboard statistics")
@SecurityRequirement(name = "BearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    @Operation(
            summary = "Get full analytics dashboard",
            description = "Returns application stats, interview performance, top missing skills, and monthly trend data for charts."
    )
    public ResponseEntity<ApiResponse<AnalyticsDtos.DashboardDto>> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(analyticsService.getDashboard(principal.getId()))
        );
    }
}
