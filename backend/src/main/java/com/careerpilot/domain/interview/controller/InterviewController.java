package com.careerpilot.domain.interview.controller;

import com.careerpilot.common.response.ApiResponse;
import com.careerpilot.common.response.PagedResponse;
import com.careerpilot.domain.interview.dto.InterviewDtos;
import com.careerpilot.domain.interview.entity.SessionStatus;
import com.careerpilot.domain.interview.service.InterviewService;
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
@RequestMapping("/interviews")
@RequiredArgsConstructor
@Tag(name = "Mock Interviews", description = "AI-powered mock interview sessions")
@SecurityRequirement(name = "BearerAuth")
public class InterviewController {

    private final InterviewService interviewService;

    // ----------------------------------------------------------------
    // POST /interviews — start a new session
    // ----------------------------------------------------------------
    @PostMapping
    @Operation(summary = "Start a new mock interview session",
               description = "AI generates technical, behavioral, and project questions for the specified job.")
    public ResponseEntity<ApiResponse<InterviewDtos.SessionDetailDto>> startSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InterviewDtos.StartSessionRequest request
    ) {
        InterviewDtos.SessionDetailDto result = interviewService.startSession(principal.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("Interview session started with " + result.getTotalQuestions() + " questions", result));
    }

    // ----------------------------------------------------------------
    // GET /interviews — list sessions
    // ----------------------------------------------------------------
    @GetMapping
    @Operation(summary = "List all interview sessions",
               description = "Filterable by status: IN_PROGRESS, COMPLETED, ABANDONED")
    public ResponseEntity<ApiResponse<PagedResponse<InterviewDtos.SessionSummaryDto>>> listSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) SessionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                interviewService.listSessions(
                        principal.getId(), status, PageRequest.of(page, Math.min(size, 100))
                )
        ));
    }

    // ----------------------------------------------------------------
    // GET /interviews/{sessionId} — get session with all questions
    // ----------------------------------------------------------------
    @GetMapping("/{sessionId}")
    @Operation(summary = "Get a full interview session with all questions and answers")
    public ResponseEntity<ApiResponse<InterviewDtos.SessionDetailDto>> getSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(interviewService.getSession(principal.getId(), sessionId))
        );
    }

    // ----------------------------------------------------------------
    // POST /interviews/{sessionId}/questions/{questionId}/answer
    // ----------------------------------------------------------------
    @PostMapping("/{sessionId}/questions/{questionId}/answer")
    @Operation(summary = "Submit an answer to a question",
               description = "AI immediately evaluates the answer and returns score, feedback, and an ideal answer.")
    public ResponseEntity<ApiResponse<InterviewDtos.AnswerResultDto>> submitAnswer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @Valid @RequestBody InterviewDtos.SubmitAnswerRequest request
    ) {
        InterviewDtos.AnswerResultDto result = interviewService.submitAnswer(
                principal.getId(), sessionId, questionId, request
        );
        return ResponseEntity.ok(ApiResponse.ok("Answer evaluated", result));
    }

    // ----------------------------------------------------------------
    // PATCH /interviews/{sessionId}/abandon
    // ----------------------------------------------------------------
    @PatchMapping("/{sessionId}/abandon")
    @Operation(summary = "Abandon an in-progress interview session")
    public ResponseEntity<ApiResponse<Void>> abandonSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sessionId
    ) {
        interviewService.abandonSession(principal.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.ok("Session abandoned"));
    }

    // ----------------------------------------------------------------
    // DELETE /interviews/{sessionId}
    // ----------------------------------------------------------------
    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Delete an interview session and all its questions")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sessionId
    ) {
        interviewService.deleteSession(principal.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.ok("Interview session deleted"));
    }
}
