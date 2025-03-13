package com.example.quiz.repository;

import com.example.quiz.model.TestSession;
import com.example.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestSessionRepository extends JpaRepository<TestSession, Long> {
    TestSession findTopByUserOrderByStartTimeDesc(User user);

    Optional<TestSession> findByAssessmentIdAndUserId(Long assessmentId, Long userId);

}
