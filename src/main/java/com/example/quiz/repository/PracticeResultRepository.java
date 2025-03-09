package com.example.quiz.repository;

import com.example.quiz.model.PracticeResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeResultRepository extends JpaRepository<PracticeResult, Long> {
}