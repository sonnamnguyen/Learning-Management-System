package com.example.quiz.repository;

import com.example.quiz.model.TestSession;
import com.example.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestSessionRepository extends JpaRepository<TestSession, Long> {
    TestSession findTopByUserOrderByStartTimeDesc(User user);
}
