package com.careerpilot.domain.job.repository;

import com.careerpilot.domain.job.entity.SkillGapAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillGapAnalysisRepository extends JpaRepository<SkillGapAnalysis, Long> {

    Optional<SkillGapAnalysis> findByJobApplicationId(Long jobApplicationId);

    boolean existsByJobApplicationId(Long jobApplicationId);

    // Used in analytics — all analyses for a user's applications
    @Query("SELECT s FROM SkillGapAnalysis s " +
           "WHERE s.jobApplication.user.id = :userId")
    List<SkillGapAnalysis> findAllByUserId(Long userId);
}
