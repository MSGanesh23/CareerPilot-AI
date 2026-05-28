package com.careerpilot.domain.resume.service;

import com.careerpilot.common.exception.AccessDeniedException;
import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.common.exception.ResourceNotFoundException;
import com.careerpilot.common.response.PagedResponse;
import com.careerpilot.domain.resume.dto.ResumeDtos;
import com.careerpilot.domain.resume.entity.Resume;
import com.careerpilot.domain.resume.mapper.ResumeMapper;
import com.careerpilot.domain.resume.repository.ResumeRepository;
import com.careerpilot.domain.user.entity.User;
import com.careerpilot.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final int MAX_RESUME_VERSIONS = 10;

    private final ResumeRepository resumeRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final ResumeTextExtractor textExtractor;
    private final ResumeMapper resumeMapper;

    // ----------------------------------------------------------------
    // Upload a new resume version
    // ----------------------------------------------------------------
    @Transactional
    public ResumeDtos.ResumeDetailDto uploadResume(
            Long userId,
            MultipartFile file,
            ResumeDtos.UploadResumeRequest request
    ) {
        User user = userService.findUserById(userId);

        int existingCount = resumeRepository.countByUserId(userId);
        if (existingCount >= MAX_RESUME_VERSIONS) {
            throw new BadRequestException(
                    "Resume version limit reached (" + MAX_RESUME_VERSIONS + "). " +
                    "Please delete an older version before uploading a new one."
            );
        }

        // Deactivate current active resume if this one is set as active
        if (request.isSetAsActive()) {
            resumeRepository.deactivateAllForUser(userId);
        }

        // Store the file
        String filePath = fileStorageService.storeResumeFile(file, userId);

        // Extract text for AI analysis
        String parsedText = textExtractor.extractText(file);
        if (parsedText.isBlank()) {
            log.warn("Could not extract text from resume for userId={}, fileName={}",
                    userId, file.getOriginalFilename());
        }

        int nextVersion = existingCount + 1;

        Resume resume = Resume.builder()
                .user(user)
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .parsedText(parsedText)
                .version(nextVersion)
                .active(request.isSetAsActive())
                .label(request.getLabel())
                .build();

        resumeRepository.save(resume);
        log.info("Resume uploaded: userId={}, resumeId={}, version={}", userId, resume.getId(), nextVersion);

        return resumeMapper.toDetailDto(resume);
    }

    // ----------------------------------------------------------------
    // List all resumes for a user (paginated)
    // ----------------------------------------------------------------
    @Transactional(readOnly = true)
    public PagedResponse<ResumeDtos.ResumeSummaryDto> listResumes(Long userId, Pageable pageable) {
        Page<ResumeDtos.ResumeSummaryDto> page = resumeRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(resumeMapper::toSummaryDto);
        return PagedResponse.from(page);
    }

    // ----------------------------------------------------------------
    // Get single resume detail
    // ----------------------------------------------------------------
    @Transactional(readOnly = true)
    public ResumeDtos.ResumeDetailDto getResume(Long userId, Long resumeId) {
        Resume resume = findResumeForUser(userId, resumeId);
        return resumeMapper.toDetailDto(resume);
    }

    // ----------------------------------------------------------------
    // Get currently active resume
    // ----------------------------------------------------------------
    @Transactional(readOnly = true)
    public ResumeDtos.ResumeDetailDto getActiveResume(Long userId) {
        Resume resume = resumeRepository.findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active resume found. Please upload a resume first."
                ));
        return resumeMapper.toDetailDto(resume);
    }

    // ----------------------------------------------------------------
    // Update resume label / set as active
    // ----------------------------------------------------------------
    @Transactional
    public ResumeDtos.ResumeDetailDto updateResume(
            Long userId,
            Long resumeId,
            ResumeDtos.UpdateResumeRequest request
    ) {
        Resume resume = findResumeForUser(userId, resumeId);

        if (request.getLabel() != null) {
            resume.setLabel(request.getLabel());
        }

        if (Boolean.TRUE.equals(request.getSetAsActive())) {
            resumeRepository.deactivateAllForUser(userId);
            resume.setActive(true);
            log.info("Set resume {} as active for userId={}", resumeId, userId);
        }

        resumeRepository.save(resume);
        return resumeMapper.toDetailDto(resume);
    }

    // ----------------------------------------------------------------
    // Delete a resume version
    // ----------------------------------------------------------------
    @Transactional
    public void deleteResume(Long userId, Long resumeId) {
        Resume resume = findResumeForUser(userId, resumeId);

        // Don't allow deleting the only resume if it's being used by job applications
        // (FK is SET NULL so this is safe — just a UX guard)
        fileStorageService.deleteFile(resume.getFilePath());
        resumeRepository.delete(resume);

        // If we deleted the active resume, promote the most recent remaining one
        if (resume.isActive()) {
            resumeRepository.findByUserIdOrderByCreatedAtDesc(userId, Pageable.ofSize(1))
                    .getContent()
                    .stream()
                    .findFirst()
                    .ifPresent(latest -> {
                        latest.setActive(true);
                        resumeRepository.save(latest);
                        log.info("Auto-promoted resume {} as active after deletion", latest.getId());
                    });
        }

        log.info("Deleted resume: resumeId={}, userId={}", resumeId, userId);
    }

    // ----------------------------------------------------------------
    // Package-visible helper: used by JobService for AI analysis
    // ----------------------------------------------------------------
    public Optional<Resume> findActiveResumeEntity(Long userId) {
        return resumeRepository.findByUserIdAndActiveTrue(userId);
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------
    private Resume findResumeForUser(Long userId, Long resumeId) {
        return resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));
    }
}
