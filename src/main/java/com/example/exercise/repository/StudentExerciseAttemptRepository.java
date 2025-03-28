package com.example.exercise.repository;

import com.example.exercise.model.Exercise;
import com.example.exercise.model.ExerciseSession;
import com.example.exercise.model.StudentExerciseAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentExerciseAttemptRepository extends JpaRepository<StudentExerciseAttempt, Integer> {
    StudentExerciseAttempt save(StudentExerciseAttempt studentExerciseAttempt);

    @Query("SELECT s FROM StudentExerciseAttempt s WHERE s.exercise_session = :exerciseSession")
    List<StudentExerciseAttempt> findByExerciseSession(@Param("exerciseSession") ExerciseSession exerciseSession);

    @Query("SELECT s FROM StudentExerciseAttempt s WHERE s.exercise_session = :exerciseSession AND s.submitted_exercise = :submittedExercise")
    Optional<StudentExerciseAttempt> findByExerciseSessionAndSubmittedExercise(
            @Param("exerciseSession") ExerciseSession exerciseSession,
            @Param("submittedExercise") Exercise submittedExercise);

    @Query("SELECT e FROM StudentExerciseAttempt e join e.submitted_exercise WHERE e.attendant_user.id =:userId AND e.attendant_email is NULL ORDER BY e.attemptDate DESC")
    List<StudentExerciseAttempt> getStudentExerciseAttemptByUser(@Param("userId") Long userId);

    @Query("SELECT e FROM StudentExerciseAttempt e  WHERE e.id =:id")
    StudentExerciseAttempt getStudentExerciseAttempt(@Param("id") Long id);
}
