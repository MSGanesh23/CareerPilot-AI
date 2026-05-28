package com.careerpilot.domain.user.entity;

import com.careerpilot.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    /**
     * JSON-serialized list of skills.
     * Stored as TEXT, mapped/unmapped at service layer.
     * Future: can be moved to a separate skills table.
     */
    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    /**
     * JSON-serialized list of target role titles.
     */
    @Column(name = "target_roles", columnDefinition = "TEXT")
    private String targetRoles;

    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private String role = "USER";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
