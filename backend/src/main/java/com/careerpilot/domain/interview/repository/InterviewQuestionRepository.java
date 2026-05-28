package com.careerpilot.domain.interview.repository;

import com.careerpilot.domain.interview.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    List<InterviewQuestion> findBySessionIdOrderBySequenceOrderAsc(Long sessionId);

    Optional<InterviewQuestion> findByIdAndSessionId(Long questionId, Long sessionId);

    // Verify ownership: question belongs to a session owned by the user
    @Query("SELECT q FROM InterviewQuestion q " +
            "WHERE q.id = :questionId AND q.session.id = :sessionId AND q.session.user.id = :userId")
    Optional<InterviewQuestion> findByIdAndSessionIdAndUserId(Long questionId, Long sessionId, Long userId);

    // Count all answered questions across all sessions for a given user
    @Query("SELECT COUNT(q) FROM InterviewQuestion q WHERE q.session.user.id = :userId AND q.answeredAt IS NOT NULL")
    long countAnsweredByUserId(Long userId);
}
