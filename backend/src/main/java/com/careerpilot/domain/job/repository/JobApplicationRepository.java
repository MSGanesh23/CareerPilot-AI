package com.careerpilot.domain.job.repository;

import com.careerpilot.domain.job.entity.ApplicationStatus;
import com.careerpilot.domain.job.entity.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    Page<JobApplication> findByUserIdOrderByAppliedDateDesc(Long userId, Pageable pageable);

    Page<JobApplication> findByUserIdAndStatusOrderByAppliedDateDesc(
            Long userId, ApplicationStatus status, Pageable pageable
    );

    Optional<JobApplication> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, ApplicationStatus status);

    @Query("SELECT j FROM JobApplication j WHERE j.user.id = :userId " +
           "AND j.status = :status ORDER BY j.appliedDate DESC")
    List<JobApplication> findByUserIdAndStatus(Long userId, ApplicationStatus status);

    // For analytics: applications grouped by status
    @Query("SELECT j.status, COUNT(j) FROM JobApplication j " +
           "WHERE j.user.id = :userId GROUP BY j.status")
    List<Object[]> countByUserIdGroupByStatus(Long userId);
}
