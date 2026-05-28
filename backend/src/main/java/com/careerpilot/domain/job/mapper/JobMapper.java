package com.careerpilot.domain.job.mapper;

import com.careerpilot.domain.job.dto.JobDtos;
import com.careerpilot.domain.job.entity.JobApplication;
import com.careerpilot.domain.job.entity.SkillGapAnalysis;
import com.careerpilot.domain.user.mapper.UserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobMapper {

    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    public JobDtos.JobSummaryDto toSummaryDto(JobApplication job) {
        return JobDtos.JobSummaryDto.builder()
                .id(job.getId())
                .company(job.getCompany())
                .roleTitle(job.getRoleTitle())
                .location(job.getLocation())
                .status(job.getStatus())
                .appliedDate(job.getAppliedDate())
                .aiMatchScore(job.getAiMatchScore())
                .hasAnalysis(job.getAiMatchScore() != null)
                .createdAt(job.getCreatedAt())
                .build();
    }

    public JobDtos.JobDetailDto toDetailDto(JobApplication job, SkillGapAnalysis analysis) {
        return JobDtos.JobDetailDto.builder()
                .id(job.getId())
                .company(job.getCompany())
                .roleTitle(job.getRoleTitle())
                .jobDescription(job.getJobDescription())
                .location(job.getLocation())
                .jobUrl(job.getJobUrl())
                .status(job.getStatus())
                .appliedDate(job.getAppliedDate())
                .notes(job.getNotes())
                .aiMatchScore(job.getAiMatchScore())
                .resumeId(job.getResume() != null ? job.getResume().getId() : null)
                .skillGapAnalysis(analysis != null ? toSkillGapDto(analysis) : null)
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    public JobDtos.SkillGapDto toSkillGapDto(SkillGapAnalysis analysis) {
        return JobDtos.SkillGapDto.builder()
                .id(analysis.getId())
                .matchScore(analysis.getMatchScore())
                .missingSkills(userMapper.parseJsonList(analysis.getMissingSkills()))
                .strongSkills(userMapper.parseJsonList(analysis.getStrongSkills()))
                .improvementSuggestions(userMapper.parseJsonList(analysis.getImprovementSuggestions()))
                .createdAt(analysis.getCreatedAt())
                .build();
    }

    public String toJson(List<String> list) {
        return userMapper.toJsonList(list);
    }
}
