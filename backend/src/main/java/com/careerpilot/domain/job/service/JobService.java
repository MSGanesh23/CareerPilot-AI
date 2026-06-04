package com.careerpilot.domain.job.service;

import com.careerpilot.common.exception.ResourceNotFoundException;
import com.careerpilot.common.response.PagedResponse;
import com.careerpilot.domain.job.dto.JobDtos;
import com.careerpilot.domain.job.entity.ApplicationStatus;
import com.careerpilot.domain.job.entity.JobApplication;
import com.careerpilot.domain.job.entity.SkillGapAnalysis;
import com.careerpilot.domain.job.mapper.JobMapper;
import com.careerpilot.domain.job.repository.JobApplicationRepository;
import com.careerpilot.domain.job.repository.SkillGapAnalysisRepository;
import com.careerpilot.domain.resume.entity.Resume;
import com.careerpilot.domain.resume.repository.ResumeRepository;
import com.careerpilot.domain.resume.service.ResumeService;
import com.careerpilot.domain.user.entity.User;
import com.careerpilot.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobApplicationRepository jobRepository;
    private final SkillGapAnalysisRepository skillGapRepository;
    private final ResumeRepository resumeRepository;
    private final UserService userService;
    private final ResumeService resumeService;
    private final JobAiService jobAiService;  // injected — ensures @Async proxy is used
    private final JobMapper jobMapper;

    // ----------------------------------------------------------------
    // Create new job application + trigger AI analysis async
    // ----------------------------------------------------------------
    @Transactional
    public JobDtos.JobDetailDto createJob(Long userId, JobDtos.CreateJobRequest request) {
        User user = userService.findUserById(userId);

        // Resolve resume: use specified ID, or fall back to active resume
        Resume resume = resolveResume(userId, request.getResumeId());

        JobApplication job = JobApplication.builder()
                .user(user)
                .resume(resume)
                .company(request.getCompany())
                .roleTitle(request.getRoleTitle())
                .jobDescription(request.getJobDescription())
                .location(request.getLocation())
                .jobUrl(request.getJobUrl())
                .appliedDate(request.getAppliedDate())
                .notes(request.getNotes())
                .status(ApplicationStatus.APPLIED)
                .build();

        jobRepository.save(job);
        log.info("Job application created: jobId={}, userId={}, company={}", job.getId(), userId, job.getCompany());

        // Trigger async AI analysis if resume text is available.
        // jobAiService is a separate Spring bean so the @Async proxy is honoured.
        if (resume != null && resume.getParsedText() != null && !resume.getParsedText().isBlank()) {
            jobAiService.triggerAiAnalysisAsync(job.getId(), resume.getParsedText(), request.getJobDescription());
        } else {
            log.warn("Skipping AI analysis for jobId={}: no resume text available", job.getId());
        }

        return jobMapper.toDetailDto(job, null);
    }

    // ----------------------------------------------------------------
    // List jobs (with optional status filter)
    // ----------------------------------------------------------------
    @Transactional(readOnly = true)
    public PagedResponse<JobDtos.JobSummaryDto> listJobs(
            Long userId, ApplicationStatus statusFilter, Pageable pageable
    ) {
        Page<JobDtos.JobSummaryDto> page;
        if (statusFilter != null) {
            page = jobRepository
                    .findByUserIdAndStatusOrderByAppliedDateDesc(userId, statusFilter, pageable)
                    .map(jobMapper::toSummaryDto);
        } else {
            page = jobRepository
                    .findByUserIdOrderByAppliedDateDesc(userId, pageable)
                    .map(jobMapper::toSummaryDto);
        }
        return PagedResponse.from(page);
    }

    // ----------------------------------------------------------------
    // Get single job detail (includes skill gap analysis)
    // ----------------------------------------------------------------
    @Transactional(readOnly = true)
    public JobDtos.JobDetailDto getJob(Long userId, Long jobId) {
        JobApplication job = findJobForUser(userId, jobId);
        SkillGapAnalysis analysis = skillGapRepository
                .findByJobApplicationId(jobId)
                .orElse(null);
        return jobMapper.toDetailDto(job, analysis);
    }

    // ----------------------------------------------------------------
    // Update job application fields
    // ----------------------------------------------------------------
    @Transactional
    public JobDtos.JobDetailDto updateJob(Long userId, Long jobId, JobDtos.UpdateJobRequest request) {
        JobApplication job = findJobForUser(userId, jobId);

        boolean jobDescriptionChanged = false;

        if (request.getCompany() != null) job.setCompany(request.getCompany());
        if (request.getRoleTitle() != null) job.setRoleTitle(request.getRoleTitle());
        if (request.getLocation() != null) job.setLocation(request.getLocation());
        if (request.getJobUrl() != null) job.setJobUrl(request.getJobUrl());
        if (request.getStatus() != null) job.setStatus(request.getStatus());
        if (request.getAppliedDate() != null) job.setAppliedDate(request.getAppliedDate());
        if (request.getNotes() != null) job.setNotes(request.getNotes());

        if (request.getJobDescription() != null &&
                !request.getJobDescription().equals(job.getJobDescription())) {
            job.setJobDescription(request.getJobDescription());
            job.setAiMatchScore(null); // Invalidate old score
            jobDescriptionChanged = true;
        }

        jobRepository.save(job);
        log.info("Job application updated: jobId={}, userId={}", jobId, userId);

        // Re-run AI analysis async if JD changed
        if (jobDescriptionChanged) {
            Optional<Resume> resume = resumeService.findActiveResumeEntity(userId);
            resume.filter(r -> r.getParsedText() != null && !r.getParsedText().isBlank())
                    .ifPresent(r -> {
                        // Delete old analysis before re-running
                        skillGapRepository.findByJobApplicationId(jobId)
                                .ifPresent(skillGapRepository::delete);
                        jobAiService.triggerAiAnalysisAsync(job.getId(), r.getParsedText(), job.getJobDescription());
                    });
        }

        SkillGapAnalysis analysis = skillGapRepository.findByJobApplicationId(jobId).orElse(null);
        return jobMapper.toDetailDto(job, analysis);
    }

    // ----------------------------------------------------------------
    // Delete a job application
    // ----------------------------------------------------------------
    @Transactional
    public void deleteJob(Long userId, Long jobId) {
        JobApplication job = findJobForUser(userId, jobId);
        jobRepository.delete(job);
        log.info("Job application deleted: jobId={}, userId={}", jobId, userId);
    }

    // ----------------------------------------------------------------
    // Manually re-trigger AI analysis (synchronous — returns result)
    // ----------------------------------------------------------------
    @Transactional
    public JobDtos.SkillGapDto triggerAnalysis(Long userId, Long jobId) {
        JobApplication job = findJobForUser(userId, jobId);
        Resume resume = resumeService.findActiveResumeEntity(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active resume found. Please upload a resume before running AI analysis."
                ));

        if (resume.getParsedText() == null || resume.getParsedText().isBlank()) {
            throw new ResourceNotFoundException(
                    "Resume text could not be extracted. Please re-upload your resume."
            );
        }

        // Delete existing analysis if present
        skillGapRepository.findByJobApplicationId(jobId)
                .ifPresent(skillGapRepository::delete);

        // Run synchronously (user explicitly requested result now)
        SkillGapAnalysis analysis = jobAiService.runAiAnalysis(job, resume.getParsedText());
        log.info("Manual AI analysis triggered for jobId={}", jobId);
        return jobMapper.toSkillGapDto(analysis);
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------
    private JobApplication findJobForUser(Long userId, Long jobId) {
        return jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "id", jobId));
    }

    private Resume resolveResume(Long userId, Long requestedResumeId) {
        if (requestedResumeId != null) {
            return resumeRepository.findByIdAndUserId(requestedResumeId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", requestedResumeId));
        }
        // Fall back to active resume silently — analysis will be skipped if none exists
        return resumeService.findActiveResumeEntity(userId).orElse(null);
    }
}
