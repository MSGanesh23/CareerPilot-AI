package com.careerpilot.domain.auth;

import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.domain.auth.dto.AuthDtos;
import com.careerpilot.domain.auth.service.AuthService;
import com.careerpilot.domain.user.entity.User;
import com.careerpilot.domain.user.repository.UserRepository;
import com.careerpilot.security.JwtTokenProvider;
import com.careerpilot.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private AuthDtos.RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new AuthDtos.RegisterRequest();
        registerRequest.setFullName("Jane Doe");
        registerRequest.setEmail("jane@example.com");
        registerRequest.setPassword("SecurePass123!");
    }

    @Test
    @DisplayName("Register - should succeed when email is not taken")
    void register_shouldSucceed_whenEmailNotTaken() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedpassword");

        User savedUser = User.builder()
                .id(1L).fullName("Jane Doe")
                .email("jane@example.com")
                .passwordHash("$2a$12$hashedpassword")
                .role("USER").active(true).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserPrincipal principal = UserPrincipal.from(savedUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("mock.jwt.token");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(savedUser));

        // Act
        AuthDtos.AuthResponse response = authService.register(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getEmail()).isEqualTo("jane@example.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Register - should throw BadRequestException when email already exists")
    void register_shouldThrowBadRequest_whenEmailAlreadyTaken() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login - should return token on valid credentials")
    void login_shouldReturnToken_whenCredentialsAreValid() {
        // Arrange
        AuthDtos.LoginRequest loginRequest = new AuthDtos.LoginRequest();
        loginRequest.setEmail("jane@example.com");
        loginRequest.setPassword("SecurePass123!");

        User user = User.builder()
                .id(1L).email("jane@example.com")
                .fullName("Jane Doe")
                .passwordHash("hashed")
                .role("USER").active(true).build();

        UserPrincipal principal = UserPrincipal.from(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("login.jwt.token");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        // Act
        AuthDtos.AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response.getAccessToken()).isEqualTo("login.jwt.token");
        assertThat(response.getRole()).isEqualTo("USER");
    }
}
