package com.careerpilot.domain.resume;

import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.config.AppProperties;
import com.careerpilot.domain.resume.dto.ResumeDtos;
import com.careerpilot.domain.resume.entity.Resume;
import com.careerpilot.domain.resume.mapper.ResumeMapper;
import com.careerpilot.domain.resume.repository.ResumeRepository;
import com.careerpilot.domain.resume.service.FileStorageService;
import com.careerpilot.domain.resume.service.ResumeService;
import com.careerpilot.domain.resume.service.ResumeTextExtractor;
import com.careerpilot.domain.user.entity.User;
import com.careerpilot.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeService Unit Tests")
class ResumeServiceTest {

    @Mock private ResumeRepository resumeRepository;
    @Mock private UserService userService;
    @Mock private FileStorageService fileStorageService;
    @Mock private ResumeTextExtractor textExtractor;
    @Mock private ResumeMapper resumeMapper;

    @InjectMocks
    private ResumeService resumeService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L).email("dev@test.com")
                .fullName("Dev User").role("USER").active(true).build();
    }

    @Test
    @DisplayName("Upload - should save resume and extract text")
    void upload_shouldSaveResumeAndExtractText() {
        // Arrange
        when(userService.findUserById(1L)).thenReturn(mockUser);
        when(resumeRepository.countByUserId(1L)).thenReturn(0);
        when(fileStorageService.storeResumeFile(any(), eq(1L))).thenReturn("user_1/resume.pdf");
        when(textExtractor.extractText(any())).thenReturn("Java Spring Boot developer with 5 years experience");

        Resume savedResume = Resume.builder()
                .id(10L).user(mockUser)
                .fileName("resume.pdf").filePath("user_1/resume.pdf")
                .parsedText("Java Spring Boot developer with 5 years experience")
                .version(1).active(true).build();

        when(resumeRepository.save(any(Resume.class))).thenReturn(savedResume);

        ResumeDtos.ResumeDetailDto expectedDto = ResumeDtos.ResumeDetailDto.builder()
                .id(10L).fileName("resume.pdf").version(1).active(true).build();
        when(resumeMapper.toDetailDto(any())).thenReturn(expectedDto);

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf",
                "resume content".getBytes()
        );
        ResumeDtos.UploadResumeRequest request = new ResumeDtos.UploadResumeRequest();
        request.setSetAsActive(true);

        // Act
        ResumeDtos.ResumeDetailDto result = resumeService.uploadResume(1L, file, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        verify(resumeRepository).save(any(Resume.class));
        verify(textExtractor).extractText(any());
        verify(fileStorageService).storeResumeFile(any(), eq(1L));
    }

    @Test
    @DisplayName("Upload - should reject when version limit is reached")
    void upload_shouldReject_whenVersionLimitReached() {
        // Arrange
        when(userService.findUserById(1L)).thenReturn(mockUser);
        when(resumeRepository.countByUserId(1L)).thenReturn(10); // At limit

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "content".getBytes()
        );

        // Act & Assert
        assertThatThrownBy(() ->
                resumeService.uploadResume(1L, file, new ResumeDtos.UploadResumeRequest())
        ).isInstanceOf(BadRequestException.class)
         .hasMessageContaining("limit reached");

        verify(resumeRepository, never()).save(any());
    }
}
