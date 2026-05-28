package com.careerpilot.domain.user.mapper;

import com.careerpilot.domain.user.dto.UserDtos;
import com.careerpilot.domain.user.entity.User;
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
public class UserMapper {

    private final ObjectMapper objectMapper;
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    public UserDtos.UserProfileDto toProfileDto(User user) {
        return UserDtos.UserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .yearsExperience(user.getYearsExperience())
                .skills(parseJsonList(user.getSkills()))
                .targetRoles(parseJsonList(user.getTargetRoles()))
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public String toJsonList(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize list to JSON", e);
            return null;
        }
    }

    public List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON list: {}", json, e);
            return Collections.emptyList();
        }
    }
}
