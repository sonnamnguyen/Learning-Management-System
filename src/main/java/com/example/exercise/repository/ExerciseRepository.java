package com.example.exercise.repository;

import com.example.exercise.model.Exercise;
import com.example.testcase.TestCase;
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


    @Query("SELECT e.name FROM Exercise e")
    List<String> findAllName();
    @Query("SELECT e FROM Exercise e WHERE LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Exercise> findByDescriptionContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT e FROM Exercise e WHERE LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND e.language.id = :languageId")
    Page<Exercise> findByDescriptionContainingIgnoreCaseAndLanguageId(@Param("keyword") String keyword,
                                                                      @Param("languageId") Long languageId,
                                                                      Pageable pageable);

    @Query("SELECT e FROM Exercise e WHERE LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND e.level = :level")
    Page<Exercise> findByDescriptionContainingIgnoreCaseAndLevel(@Param("keyword") String keyword,
                                                                 @Param("level") Exercise.Level level,
                                                                 Pageable pageable);

    @Query("SELECT e FROM Exercise e WHERE LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND e.language.id = :languageId " +
            "AND e.level = :level")
    Page<Exercise> findByDescriptionContainingIgnoreCaseAndLanguageIdAndLevel(@Param("keyword") String keyword,
                                                                              @Param("languageId") Long languageId,
                                                                              @Param("level") Exercise.Level level,
                                                                              Pageable pageable);

    Page<Exercise> findByLanguageId(Long languageId, Pageable pageable);

}
