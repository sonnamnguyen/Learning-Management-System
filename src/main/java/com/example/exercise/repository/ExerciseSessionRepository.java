package com.example.exercise.repository;


import com.example.exercise.model.ExerciseSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseSessionRepository extends JpaRepository<ExerciseSession, Long> {
    ExerciseSession save(ExerciseSession exerciseSession);
}
