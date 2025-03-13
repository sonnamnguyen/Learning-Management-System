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

// son nam them


    @Query("Select COUNT(e) FROM Exercise e JOIN e.language WHERE e.level = 'EASY' and e.language.language like %:language% ")
    Integer countNumberEasyExercises(@Param("language") String language );

    @Query("Select COUNT(e) FROM Exercise e JOIN e.language WHERE e.level = 'HARD' and e.language.language like %:language%")
    Integer countNumberHardExercises(@Param("language") String language );

    @Query("Select COUNT(e) FROM Exercise e JOIN e.language WHERE e.level = 'MEDIUM' and e.language.language like %:language%")
    Integer countNumberMediumExercises(@Param("language") String language );

    @Query("Select COUNT(sea) FROM " +
            "StudentExerciseAttempt sea " +
            "JOIN sea.attendant_user " +
            "WHERE " +
            "sea.attendant_user.id = :id and sea.score_exercise >= :passingScore ")
    Integer countUserNumberExercises(@Param("id") Long userId,
                                     @Param("passingScore") double passingScore );

    @Query("SELECT COUNT(DISTINCT e.id) FROM Exercise e " +
            "JOIN e.studentExerciseAttempts sea " +
            "WHERE sea.attendant_user.id = :id AND sea.score_exercise >= :passingScore " +
            "AND sea.attemptDate = (SELECT MIN(sea2.attemptDate) FROM StudentExerciseAttempt sea2 " +
            "WHERE sea2.submitted_exercise.id = e.id AND sea2.attendant_user.id = sea.attendant_user.id)")
    Integer countUserNumberPerfectExercises(@Param("id") Long userId,
                                            @Param("passingScore") double passingScore);



    @Query("Select COUNT(DISTINCT e.id) FROM Exercise e " +
            "JOIN e.studentExerciseAttempts sea " +
            "JOIN e.language WHERE e.level  = 'EASY' " +
            "AND sea.attendant_user.id = :id  and e.language.language like %:language% and sea.score_exercise >= :passingScore ")
    Integer countUserNumberEasyExercises(@Param("id") Long userId,
                                         @Param("language") String language,
                                         @Param("passingScore") double passingScore );

    @Query("Select COUNT(DISTINCT e.id) FROM Exercise e " +
            "JOIN e.studentExerciseAttempts sea " +
            "JOIN e.language WHERE e.level =  'HARD' " +
            "AND sea.attendant_user.id =:id  and e.language.language like %:language% and sea.score_exercise >= :passingScore")
    Integer countUserNumberHardExercises(@Param("id") Long userId,
                                         @Param("language") String language,
                                         @Param("passingScore") double passingScore  );

    @Query("Select COUNT(DISTINCT e.id) FROM Exercise e " +
            "JOIN e.studentExerciseAttempts sea " +
            "JOIN e.language WHERE e.level = 'MEDIUM' " +
            "AND sea.attendant_user.id =:id  and e.language.language like %:language% and sea.score_exercise >= :passingScore")
    Integer countUserNumberMediumExercises(@Param("id") Long userId,
                                           @Param("language") String language,
                                           @Param("passingScore") double passingScore  );

    @Query("SELECT MONTH(sea.attemptDate) as month, COUNT(sea) as count " +
            "FROM StudentExerciseAttempt sea " +
            "WHERE YEAR(sea.attemptDate) = :year " +
            "AND sea.score_exercise >= :passingScore " +
            "AND sea.attendant_user.id = :userId " +
            "GROUP BY MONTH(sea.attemptDate) " +
            "ORDER BY MONTH(sea.attemptDate)")
    List<Object[]> countPassedTestsPerMonth(@Param("userId") Long userId,
                                            @Param("year") int year,
                                            @Param("passingScore") double passingScore);


    @Query("SELECT COUNT(DISTINCT e.id) FROM Exercise e " +
            "JOIN e.studentExerciseAttempts sea " +
            "WHERE sea.attendant_user.id = :userId " +
            "GROUP BY e.id " +
            "HAVING COUNT(sea.id) >= 5 AND SUM(CASE WHEN sea.score_exercise >= 70 THEN 1 ELSE 0 END) > 0")
    Integer countExercisesWithMoreThanFiveAttemptsAndAtLeastOneAbove70(@Param("userId") Long userId);

    @Query("SELECT COUNT(sea.id) FROM StudentExerciseAttempt sea " +
            "WHERE sea.attendant_user.id = :userId " +
            "AND HOUR(sea.attemptDate) BETWEEN :startHour AND :endHour")
    Integer countExercisesSubmittedBetweenHours(
            @Param("userId") Long userId,
            @Param("startHour") int startHour,
            @Param("endHour") int endHour);
}
