package com.example.student_exercise_attemp.repository;

import com.example.student_exercise_attemp.model.StudentExerciseAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentExerciseAttemptRepository extends JpaRepository<StudentExerciseAttempt, Integer> {
    StudentExerciseAttempt save(StudentExerciseAttempt studentExerciseAttempt);
}
