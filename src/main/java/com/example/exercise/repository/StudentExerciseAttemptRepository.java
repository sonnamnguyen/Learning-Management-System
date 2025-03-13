package com.example.exercise.repository;

<<<<<<< HEAD:src/main/java/com/example/exercise/repository/StudentExerciseAttemptRepository.java
import com.example.exercise.model.Exercise;
import com.example.exercise.model.ExerciseSession;
import com.example.exercise.model.StudentExerciseAttempt;
=======
import com.example.student_exercise_attemp.model.Exercise;
import com.example.student_exercise_attemp.model.ExerciseSession;
import com.example.student_exercise_attemp.model.StudentExerciseAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
>>>>>>> 6c22cd6 (feat: add Dashboard display feature for users):src/main/java/com/example/student_exercise_attemp/repository/StudentExerciseAttemptRepository.java
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

    @Query("SELECT e FROM StudentExerciseAttempt e join e.submitted_exercise WHERE e.attendant_user.id =:userId ORDER BY e.attemptDate DESC")
    Page<StudentExerciseAttempt> getStudentExerciseAttemptByUser(@Param("userId") Long userId, Pageable pageable);
}
