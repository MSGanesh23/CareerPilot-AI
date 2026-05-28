package com.careerpilot.domain.user.service;

import com.careerpilot.common.exception.ResourceNotFoundException;
import com.careerpilot.domain.user.dto.UserDtos;
import com.careerpilot.domain.user.entity.User;
import com.careerpilot.domain.user.mapper.UserMapper;
import com.careerpilot.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserDtos.UserProfileDto getProfile(Long userId) {
        User user = findUserById(userId);
        return userMapper.toProfileDto(user);
    }

    @Transactional
    public UserDtos.UserProfileDto updateProfile(Long userId, UserDtos.UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getYearsExperience() != null) {
            user.setYearsExperience(request.getYearsExperience());
        }
        if (request.getSkills() != null) {
            user.setSkills(userMapper.toJsonList(request.getSkills()));
        }
        if (request.getTargetRoles() != null) {
            user.setTargetRoles(userMapper.toJsonList(request.getTargetRoles()));
        }

        userRepository.save(user);
        log.info("Profile updated for userId={}", userId);
        return userMapper.toProfileDto(user);
    }

    // ----------------------------------------------------------------
    // Package-visible helper (shared across services)
    // ----------------------------------------------------------------
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
