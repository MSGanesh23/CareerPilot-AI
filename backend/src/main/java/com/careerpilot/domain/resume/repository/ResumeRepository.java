package com.careerpilot.domain.resume.repository;

import com.careerpilot.domain.resume.entity.Resume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Page<Resume> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Resume> findByIdAndUserId(Long id, Long userId);

    Optional<Resume> findByUserIdAndActiveTrue(Long userId);

    int countByUserId(Long userId);

    @Modifying
    @Query("UPDATE Resume r SET r.active = false WHERE r.user.id = :userId AND r.active = true")
    void deactivateAllForUser(Long userId);
}
