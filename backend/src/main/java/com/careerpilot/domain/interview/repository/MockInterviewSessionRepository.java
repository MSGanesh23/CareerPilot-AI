package com.careerpilot.domain.interview.repository;

import com.careerpilot.domain.interview.entity.MockInterviewSession;
import com.careerpilot.domain.interview.entity.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MockInterviewSessionRepository extends JpaRepository<MockInterviewSession, Long> {

    Page<MockInterviewSession> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<MockInterviewSession> findByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId, SessionStatus status, Pageable pageable
    );

    Optional<MockInterviewSession> findByIdAndUserId(Long id, Long userId);

    // For analytics: compute average score per user
    @Query("SELECT AVG(s.overallScore) FROM MockInterviewSession s " +
           "WHERE s.user.id = :userId AND s.status = 'COMPLETED' AND s.overallScore IS NOT NULL")
    Double findAverageScoreByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, SessionStatus status);

    long countByUserId(Long userId);
}
