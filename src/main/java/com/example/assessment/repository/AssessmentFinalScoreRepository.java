package com.example.assessment.repository;

import com.example.assessment.model.AssessmentFinalScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentFinalScoreRepository extends JpaRepository<AssessmentFinalScore, Long> {

    /**
     * Find final score by assessment_id and user_id
     */
    Optional<AssessmentFinalScore> findByAssessmentIdAndUserId(Long assessmentId, Long userId);

    /**
     * find all final score of 1 assessment
     */
    List<AssessmentFinalScore> findByAssessmentId(Long assessmentId);

    /**
     * get all final score of 1 user
     */
    List<AssessmentFinalScore> findByUserId(Long userId);


}
