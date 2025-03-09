package com.example.student_exercise_attemp.repository;


import com.example.student_exercise_attemp.model.ExerciseSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseSessionRepository extends JpaRepository<ExerciseSession, Long> {
    ExerciseSession save(ExerciseSession exerciseSession);
}
