package com.careerpilot.domain.resume.service;

import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final AppProperties appProperties;
    private Path uploadRootPath;

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".pdf", ".doc", ".docx");

    @PostConstruct
    public void init() {
        uploadRootPath = Paths.get(appProperties.getFile().getUploadDir())
                .toAbsolutePath()
                .normalize();
        try {
            Files.createDirectories(uploadRootPath);
            log.info("File storage initialized at: {}", uploadRootPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadRootPath, e);
        }
    }

    /**
     * Validates and stores a resume file.
     * Returns the relative file path to store in the database.
     */
    public String storeResumeFile(MultipartFile file, Long userId) {
        validateFile(file);

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume"
        );
        String extension = getExtension(originalName);
        String storedFileName = "user_" + userId + "_" + UUID.randomUUID() + extension;

        // Store inside a per-user subdirectory to keep things organized
        Path userDir = uploadRootPath.resolve("user_" + userId);
        try {
            Files.createDirectories(userDir);
            Path targetPath = userDir.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored resume file: {} for userId={}", storedFileName, userId);
            // Return relative path for DB storage
            return "user_" + userId + "/" + storedFileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + storedFileName, e);
        }
    }

    /**
     * Deletes a stored file by its relative path.
     */
    public void deleteFile(String relativePath) {
        if (!StringUtils.hasText(relativePath)) return;
        try {
            Path filePath = uploadRootPath.resolve(relativePath).normalize();
            // Security: ensure resolved path is still inside upload root
            if (!filePath.startsWith(uploadRootPath)) {
                log.warn("Attempted path traversal on delete: {}", relativePath);
                return;
            }
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", relativePath);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", relativePath, e);
        }
    }

    // ----------------------------------------------------------------
    // Validation
    // ----------------------------------------------------------------
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }

        // Size check
        long maxBytes = appProperties.getFile().getMaxSizeBytes();
        if (file.getSize() > maxBytes) {
            throw new BadRequestException(
                    "File size exceeds limit. Maximum allowed: " + (maxBytes / 1024 / 1024) + "MB"
            );
        }

        // Extension check
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String extension = getExtension(originalName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException(
                    "Unsupported file type. Allowed types: " + ALLOWED_EXTENSIONS
            );
        }

        // Content-type check
        String contentType = file.getContentType();
        String allowedTypes = appProperties.getFile().getAllowedTypes();
        boolean contentTypeAllowed = Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .anyMatch(t -> t.equalsIgnoreCase(contentType));

        if (!contentTypeAllowed) {
            throw new BadRequestException("Invalid file content type: " + contentType);
        }
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex >= 0) ? fileName.substring(dotIndex) : "";
    }
}
