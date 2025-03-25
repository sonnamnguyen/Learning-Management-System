package com.example.assessment.repository;

import com.example.assessment.model.StudentAssessmentAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentAssessmentAttemptRepository extends JpaRepository<StudentAssessmentAttempt, Long> {
    List<StudentAssessmentAttempt> findAll();
    // Lấy danh sách attempts theo assessment (không phân trang)
    List<StudentAssessmentAttempt> findByAssessment_Id(Long assessmentId);

    // Lấy danh sách attempts theo assessment với phân trang
    Page<StudentAssessmentAttempt> findByAssessment_Id(Long assessmentId, Pageable pageable);

    // Lấy danh sách attempts theo user id
    List<StudentAssessmentAttempt> findByUserId(Long userId);
    long countByAssessmentId(Long assessmentId);

}
