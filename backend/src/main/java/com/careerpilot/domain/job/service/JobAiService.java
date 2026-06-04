package com.careerpilot.domain.job.service;

import com.careerpilot.ai.AiService;
import com.careerpilot.ai.dto.AiDtos;
import com.careerpilot.domain.job.entity.JobApplication;
import com.careerpilot.domain.job.entity.SkillGapAnalysis;
import com.careerpilot.domain.job.mapper.JobMapper;
import com.careerpilot.domain.job.repository.JobApplicationRepository;
import com.careerpilot.domain.job.repository.SkillGapAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dedicated service for AI-powered job analysis.
 *
 * Extracted from JobService so that @Async can work correctly via the
 * Spring AOP proxy. Calling @Async methods on 'this' inside the same bean
 * bypasses the proxy and runs synchronously — a very common Spring pitfall.
 *
 * By keeping this in a separate bean, any call from JobService goes through
 * the proxy, and Spring correctly offloads it to the aiTaskExecutor thread pool.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobAiService {

    private final JobApplicationRepository jobRepository;
    private final SkillGapAnalysisRepository skillGapRepository;
    private final AiService aiService;
    private final JobMapper jobMapper;

    // ----------------------------------------------------------------
    // Async entry point — called from JobService after job create/update
    // Runs on the dedicated aiTaskExecutor thread pool (see AsyncConfig)
    // ----------------------------------------------------------------
    @Async("aiTaskExecutor")
    public void triggerAiAnalysisAsync(Long jobId, String resumeText, String jobDescription) {
        try {
            // Re-fetch the entity inside the async thread (the calling thread's
            // transaction has already committed, so we need a fresh context)
            JobApplication job = jobRepository.findById(jobId).orElse(null);
            if (job == null) {
                log.warn("AI analysis skipped: jobId={} not found (may have been deleted)", jobId);
                return;
            }
            runAiAnalysis(job, resumeText);
        } catch (Exception e) {
            log.error("Async AI analysis failed for jobId={}: {}", jobId, e.getMessage(), e);
        }
    }

    // ----------------------------------------------------------------
    // Core analysis logic — also callable synchronously from JobService
    // ----------------------------------------------------------------
    @Transactional
    public SkillGapAnalysis runAiAnalysis(JobApplication job, String resumeText) {
        log.info("Running AI analysis for jobId={}", job.getId());

        AiDtos.ResumeAnalysisResult result = aiService.analyzeResumeVsJob(
                resumeText, job.getJobDescription()
        );

        SkillGapAnalysis analysis = SkillGapAnalysis.builder()
                .jobApplication(job)
                .matchScore(result.getMatchScore())
                .missingSkills(jobMapper.toJson(result.getMissingSkills()))
                .strongSkills(jobMapper.toJson(result.getStrongSkills()))
                .improvementSuggestions(jobMapper.toJson(result.getImprovementSuggestions()))
                .rawAiResponse(result.getRawAiResponse())
                .build();

        skillGapRepository.save(analysis);

        // Denormalize match score on the job for fast list queries
        job.setAiMatchScore(result.getMatchScore());
        jobRepository.save(job);

        log.info("AI analysis complete for jobId={}, matchScore={}", job.getId(), result.getMatchScore());
        return analysis;
    }
}
