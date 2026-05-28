package com.careerpilot.domain.resume.mapper;

import com.careerpilot.domain.resume.dto.ResumeDtos;
import com.careerpilot.domain.resume.entity.Resume;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ResumeMapper {

    public ResumeDtos.ResumeSummaryDto toSummaryDto(Resume resume) {
        return ResumeDtos.ResumeSummaryDto.builder()
                .id(resume.getId())
                .fileName(resume.getFileName())
                .label(resume.getLabel())
                .version(resume.getVersion())
                .active(resume.isActive())
                .fileSize(resume.getFileSize())
                .contentType(resume.getContentType())
                .createdAt(resume.getCreatedAt())
                .hasParsedText(StringUtils.hasText(resume.getParsedText()))
                .build();
    }

    public ResumeDtos.ResumeDetailDto toDetailDto(Resume resume) {
        return ResumeDtos.ResumeDetailDto.builder()
                .id(resume.getId())
                .fileName(resume.getFileName())
                .label(resume.getLabel())
                .version(resume.getVersion())
                .active(resume.isActive())
                .fileSize(resume.getFileSize())
                .contentType(resume.getContentType())
                .parsedText(resume.getParsedText())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }
}
