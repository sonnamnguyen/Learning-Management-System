package com.example.exercise.repository;


import com.example.exercise.model.ExerciseSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ExerciseSessionRepository extends JpaRepository<ExerciseSession, Long> {
    ExerciseSession save(ExerciseSession exerciseSession);

    @Query("SELECT es FROM ExerciseSession es WHERE es.startTime BETWEEN :start AND :end")
    Optional<ExerciseSession> findByStartTimeBetween(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);

}
