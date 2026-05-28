package com.careerpilot.domain.resume.controller;

import com.careerpilot.common.response.ApiResponse;
import com.careerpilot.common.response.PagedResponse;
import com.careerpilot.domain.resume.dto.ResumeDtos;
import com.careerpilot.domain.resume.service.ResumeService;
import com.careerpilot.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
@Tag(name = "Resume Management", description = "Upload and manage resume versions")
@SecurityRequirement(name = "BearerAuth")
public class ResumeController {

    private final ResumeService resumeService;

    // ----------------------------------------------------------------
    // POST /resumes — upload a new resume
    // ----------------------------------------------------------------
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a new resume version",
               description = "Accepts PDF, DOC, or DOCX. Max size 10MB. " +
                             "Optionally set as the active resume.")
    public ResponseEntity<ApiResponse<ResumeDtos.ResumeDetailDto>> uploadResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Resume file (PDF/DOC/DOCX)", required = true,
                       content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "label", required = false) String label,
            @RequestPart(value = "setAsActive", required = false) String setAsActive
    ) {
        ResumeDtos.UploadResumeRequest request = new ResumeDtos.UploadResumeRequest();
        request.setLabel(label);
        request.setSetAsActive(!"false".equalsIgnoreCase(setAsActive));

        ResumeDtos.ResumeDetailDto result = resumeService.uploadResume(
                principal.getId(), file, request
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("Resume uploaded successfully", result));
    }

    // ----------------------------------------------------------------
    // GET /resumes — list all resume versions
    // ----------------------------------------------------------------
    @GetMapping
    @Operation(summary = "List all resume versions for the current user")
    public ResponseEntity<ApiResponse<PagedResponse<ResumeDtos.ResumeSummaryDto>>> listResumes(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return ResponseEntity.ok(
                ApiResponse.ok(resumeService.listResumes(principal.getId(), pageable))
        );
    }

    // ----------------------------------------------------------------
    // GET /resumes/active — get the current active resume
    // ----------------------------------------------------------------
    @GetMapping("/active")
    @Operation(summary = "Get the currently active resume")
    public ResponseEntity<ApiResponse<ResumeDtos.ResumeDetailDto>> getActiveResume(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(resumeService.getActiveResume(principal.getId()))
        );
    }

    // ----------------------------------------------------------------
    // GET /resumes/{id} — get single resume detail
    // ----------------------------------------------------------------
    @GetMapping("/{id}")
    @Operation(summary = "Get a specific resume by ID")
    public ResponseEntity<ApiResponse<ResumeDtos.ResumeDetailDto>> getResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(resumeService.getResume(principal.getId(), id))
        );
    }

    // ----------------------------------------------------------------
    // PATCH /resumes/{id} — update label or set as active
    // ----------------------------------------------------------------
    @PatchMapping("/{id}")
    @Operation(summary = "Update resume label or set as active version")
    public ResponseEntity<ApiResponse<ResumeDtos.ResumeDetailDto>> updateResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ResumeDtos.UpdateResumeRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok("Resume updated",
                        resumeService.updateResume(principal.getId(), id, request))
        );
    }

    // ----------------------------------------------------------------
    // DELETE /resumes/{id} — delete a resume version
    // ----------------------------------------------------------------
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a resume version")
    public ResponseEntity<ApiResponse<Void>> deleteResume(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        resumeService.deleteResume(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Resume deleted successfully"));
    }
}
