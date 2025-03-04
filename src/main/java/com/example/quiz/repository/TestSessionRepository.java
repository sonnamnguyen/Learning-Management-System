package com.example.quiz.repository;

import com.example.quiz.model.TestSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestSessionRepository extends JpaRepository<TestSession, Long> {
}
