package com.careerpilot.domain.auth.service;

import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.domain.auth.dto.AuthDtos;
import com.careerpilot.domain.user.entity.User;
import com.careerpilot.domain.user.repository.UserRepository;
import com.careerpilot.security.JwtTokenProvider;
import com.careerpilot.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final EntityManager entityManager;

    // ----------------------------------------------------------------
    // Register
    // ----------------------------------------------------------------
    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email address is already registered: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .active(true)
                .build();

        userRepository.save(user);
        // Flush to DB so the re-authentication query can see the new user
        // within the same transaction (avoids UsernameNotFoundException on
        // busy systems with strict isolation)
        entityManager.flush();
        log.info("New user registered: email={}", user.getEmail());

        // Auto-login after registration
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String token = jwtTokenProvider.generateAccessToken(authentication);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        return buildAuthResponse(token, principal, user);
    }

    // ----------------------------------------------------------------
    // Login
    // ----------------------------------------------------------------
    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        String token = jwtTokenProvider.generateAccessToken(authentication);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findByEmail(principal.getEmail())
                .orElseThrow(); // won't happen — authentication succeeded

        log.info("User logged in: email={}", user.getEmail());
        return buildAuthResponse(token, principal, user);
    }

    // ----------------------------------------------------------------
    // Internal
    // ----------------------------------------------------------------
    private AuthDtos.AuthResponse buildAuthResponse(String token, UserPrincipal principal, User user) {
        return new AuthDtos.AuthResponse(
                token,
                principal.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }
}
