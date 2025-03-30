package com.example.assessment.repository;

import com.example.assessment.model.Assessment;
import com.example.assessment.model.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, Long> {
    List<AssessmentQuestion> findByAssessmentIdOrderByOrderIndex(Long assessmentId);

    /**
     * Deletes all questions associated with a specific assessment.
     *
     * This method removes all records from the assessment_question table
     * where the assessment ID matches the provided ID.
     *
     * @param assessmentId The ID of the assessment whose questions should be deleted.
     * @return The number of deleted records.
     */
    @Modifying
    @Query("DELETE FROM AssessmentQuestion aq WHERE aq.assessment.id = :assessmentId")
    int deleteByAssessmentId(@Param("assessmentId") Long assessmentId);

    /**
     * Retrieves all questions linked to a given assessment.
     *
     * @param assessmentId The ID of the assessment.
     * @return A list of AssessmentQuestion entities associated with the assessment.
     */
    List<AssessmentQuestion> findByAssessmentId(Long assessmentId);
}
