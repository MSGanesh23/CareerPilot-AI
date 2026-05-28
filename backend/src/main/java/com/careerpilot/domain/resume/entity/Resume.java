package com.careerpilot.domain.resume.entity;

import com.careerpilot.common.BaseEntity;
import com.careerpilot.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resumes", indexes = {
        @Index(name = "idx_resumes_user_id", columnList = "user_id"),
        @Index(name = "idx_resumes_user_active", columnList = "user_id, is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    /**
     * Plain text extracted from the uploaded resume file.
     * Used as input for AI analysis.
     */
    @Column(name = "parsed_text", columnDefinition = "LONGTEXT")
    private String parsedText;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private int version = 1;

    /**
     * Only one resume version is 'active' at a time per user.
     * The active resume is the default for new job applications.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Optional human-readable label e.g. "Backend-Senior-v2"
     */
    @Column(name = "label", length = 100)
    private String label;
}
