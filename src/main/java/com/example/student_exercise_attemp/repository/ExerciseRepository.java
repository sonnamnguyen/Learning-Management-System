package com.example.student_exercise_attemp.repository;

import com.example.student_exercise_attemp.model.Exercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseRepository extends PagingAndSortingRepository<Exercise, Long>, JpaRepository<Exercise, Long> {


    @Query("SELECT e FROM Exercise e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Exercise> searchByTitle(@Param("title") String title);


    @Query("SELECT e FROM Exercise e WHERE e.name LIKE %:searchQuery%")
    Page<Exercise> searchExercises(@Param("searchQuery") String searchQuery, Pageable pageable);

    @Query("SELECT e FROM Exercise e " +
            "WHERE (:languageId IS NULL OR e.language.id = :languageId) " +
            "AND (:level IS NULL OR e.level = :level)")
    Page<Exercise> findByFilters(@Param("languageId") Long languageId,
                                 @Param("level") Exercise.Level level,
                                 Pageable pageable);
    Optional<Exercise> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT COUNT(e) > 0 FROM Exercise e WHERE e.name = :name AND e.id <> :id")
    boolean existsByNameExcludingId(@Param("name") String name, @Param("id") Long id);

    Page<Exercise> findAll(Pageable pageable);

    List<Exercise> findAll();

    Optional<Exercise> findById(Long id);

    Exercise save(Exercise exercise);

    void deleteById(Long id);
    void deleteAllByIdIn(List<Long> ids);
    @Query("SELECT e FROM Assessment a JOIN a.exercises e WHERE a.id = :assessmentId")
    List<Exercise> findExercisesByAssessmentId(@Param("assessmentId") Long assessmentId);



}