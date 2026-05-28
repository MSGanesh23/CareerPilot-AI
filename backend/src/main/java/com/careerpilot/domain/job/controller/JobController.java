package com.careerpilot.domain.job.controller;

import com.careerpilot.common.response.ApiResponse;
import com.careerpilot.common.response.PagedResponse;
import com.careerpilot.domain.job.dto.JobDtos;
import com.careerpilot.domain.job.entity.ApplicationStatus;
import com.careerpilot.domain.job.service.JobService;
import com.careerpilot.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Tag(name = "Job Applications", description = "Track and manage job applications with AI analysis")
@SecurityRequirement(name = "BearerAuth")
public class JobController {

    private final JobService jobService;

    // ----------------------------------------------------------------
    // POST /jobs — create a new application
    // ----------------------------------------------------------------
    @PostMapping
    @Operation(summary = "Create a new job application",
               description = "Saves the application and automatically triggers AI resume analysis in the background.")
    public ResponseEntity<ApiResponse<JobDtos.JobDetailDto>> createJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody JobDtos.CreateJobRequest request
    ) {
        JobDtos.JobDetailDto result = jobService.createJob(principal.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("Job application created. AI analysis is running in the background.", result));
    }

    // ----------------------------------------------------------------
    // GET /jobs — list with optional status filter
    // ----------------------------------------------------------------
    @GetMapping
    @Operation(summary = "List all job applications",
               description = "Paginated. Optionally filter by status: APPLIED, INTERVIEWING, OFFER, REJECTED, WITHDRAWN.")
    public ResponseEntity<ApiResponse<PagedResponse<JobDtos.JobSummaryDto>>> listJobs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PagedResponse<JobDtos.JobSummaryDto> result = jobService.listJobs(
                principal.getId(), status, PageRequest.of(page, Math.min(size, 100))
        );
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ----------------------------------------------------------------
    // GET /jobs/{id} — get full detail with AI analysis
    // ----------------------------------------------------------------
    @GetMapping("/{id}")
    @Operation(summary = "Get a job application with full AI analysis")
    public ResponseEntity<ApiResponse<JobDtos.JobDetailDto>> getJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(jobService.getJob(principal.getId(), id))
        );
    }

    // ----------------------------------------------------------------
    // PUT /jobs/{id} — update application
    // ----------------------------------------------------------------
    @PutMapping("/{id}")
    @Operation(summary = "Update a job application",
               description = "If job description is changed, AI analysis is automatically re-run.")
    public ResponseEntity<ApiResponse<JobDtos.JobDetailDto>> updateJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody JobDtos.UpdateJobRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok("Job application updated",
                        jobService.updateJob(principal.getId(), id, request))
        );
    }

    // ----------------------------------------------------------------
    // DELETE /jobs/{id}
    // ----------------------------------------------------------------
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a job application")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        jobService.deleteJob(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Job application deleted"));
    }

    // ----------------------------------------------------------------
    // POST /jobs/{id}/analyze — manually re-trigger AI analysis
    // ----------------------------------------------------------------
    @PostMapping("/{id}/analyze")
    @Operation(summary = "Manually trigger or re-run AI resume analysis for a job")
    public ResponseEntity<ApiResponse<JobDtos.SkillGapDto>> triggerAnalysis(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        JobDtos.SkillGapDto result = jobService.triggerAnalysis(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("AI analysis complete", result));
    }
}
