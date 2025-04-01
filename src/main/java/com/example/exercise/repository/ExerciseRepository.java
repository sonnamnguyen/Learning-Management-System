package com.example.exercise.repository;

import com.example.exercise.model.Exercise;
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

    // ✅ Lấy dữ liệu tất cả ngôn ngữ (3 cột: assessed, unassessed, total)
    @Query("SELECT e.language.language, " +
            "SUM(CASE WHEN EXISTS (SELECT 1 FROM Assessment a JOIN a.exercises ex WHERE ex.id = e.id) THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN NOT EXISTS (SELECT 1 FROM Assessment a JOIN a.exercises ex WHERE ex.id = e.id) THEN 1 ELSE 0 END) " +
            "FROM Exercise e GROUP BY e.language.language")
    List<Object[]> countExercisesByLanguageWithAssessment();

    // ✅ Lấy dữ liệu theo languageId
    @Query("SELECT e.language.language, " +
            "SUM(CASE WHEN EXISTS (SELECT 1 FROM Assessment a JOIN a.exercises ex WHERE ex.id = e.id) THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN NOT EXISTS (SELECT 1 FROM Assessment a JOIN a.exercises ex WHERE ex.id = e.id) THEN 1 ELSE 0 END) " +
            "FROM Exercise e WHERE e.language.id = :languageId GROUP BY e.language.language")
    List<Object[]> countExercisesByLanguageWithAssessment(@Param("languageId") Long languageId);
    @Query("SELECT e FROM Exercise e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Exercise> searchByTitle(@Param("title") String title);


    @Query("SELECT e FROM Exercise e WHERE LOWER(TRIM(e.name)) LIKE LOWER(CONCAT('%', LOWER(:searchQuery), '%'))")
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

    @Query("SELECT COUNT(e) FROM Exercise e WHERE e.language.language = :language")
    Integer countTotalExercisesByLanguage(@Param("language") String language);

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
            "sea.attendant_user.id = :id AND sea.score_exercise >= :passingScore  AND sea.attendant_email is null")
    Integer countUserNumberExercises(@Param("id") Long userId,
                                     @Param("passingScore") double passingScore );

    @Query("SELECT COUNT(DISTINCT e.id) FROM Exercise e " +
            "JOIN e.studentExerciseAttempts sea " +
            "WHERE sea.attendant_user.id = :id AND sea.score_exercise >= :passingScore  AND sea.attendant_email is null " +
            "AND sea.attemptDate = (SELECT MIN(sea2.attemptDate) FROM StudentExerciseAttempt sea2 " +
            "WHERE sea2.submitted_exercise.id = e.id AND sea2.attendant_user.id = sea.attendant_user.id)")
    Integer countUserNumberPerfectExercises(@Param("id") Long userId,
                                            @Param("passingScore") double passingScore);



    @Query("Select COUNT(DISTINCT e.id) FROM Exercise e " +
            "JOIN e.studentExerciseAttempts sea " +
            "JOIN e.language WHERE e.level  = 'EASY'  AND sea.attendant_email is null " +
            "AND sea.attendant_user.id = :id  and e.language.language like %:language% and sea.score_exercise >= :passingScore")
    Integer countUserNumberEasyExercises(@Param("id") Long userId,
                                         @Param("language") String language,
                                         @Param("passingScore") double passingScore );

    @Query("Select COUNT(DISTINCT e.id) FROM Exercise e " +
            "JOIN e.studentExerciseAttempts sea " +
            "JOIN e.language WHERE sea.attendant_email is null " +
            "AND sea.attendant_user.id =:id  and e.language.language = :language and sea.score_exercise >= :passingScore")
    Integer countUserExercisesByLanguage(@Param("id") Long userId,
                                         @Param("language") String language,
                                         @Param("passingScore") double passingScore);

    @Query("Select COUNT(DISTINCT e.id) FROM Exercise e " +
            "JOIN e.studentExerciseAttempts sea " +
            "JOIN e.language WHERE e.level =  'HARD'  AND sea.attendant_email is null " +
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
            "AND sea.attendant_user.id = :userId  AND sea.attendant_email is null " +
            "GROUP BY MONTH(sea.attemptDate) " +
            "ORDER BY MONTH(sea.attemptDate)")
    List<Object[]> countPassedTestsPerMonth(@Param("userId") Long userId,
                                            @Param("year") int year);


    @Query("SELECT COUNT(DISTINCT e.id) FROM Exercise e " +
            "JOIN e.studentExerciseAttempts sea " +
            "WHERE sea.attendant_user.id = :userId  AND sea.attendant_email is null " +
            "GROUP BY e.id " +
            "HAVING COUNT(sea.id) >= 5 AND SUM(CASE WHEN sea.score_exercise >= 70 THEN 1 ELSE 0 END) > 0")
    Integer countExercisesWithMoreThanFiveAttemptsAndAtLeastOneAbove70(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT sea.submitted_exercise.id) FROM StudentExerciseAttempt sea " +
            "WHERE sea.attendant_user.id = :userId  AND sea.attendant_email is null " +
            "AND HOUR(sea.attemptDate) BETWEEN :startHour AND :endHour AND sea.score_exercise >= :passingScore")
    Integer countExercisesSubmittedBetweenHours(
            @Param("userId") Long userId,
            @Param("startHour") int startHour,
            @Param("endHour") int endHour,
            @Param("passingScore") double passingScore  );


    //-----------Duplicate
    @Query("SELECT e FROM Exercise e WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Exercise> findByNameContainingIgnoreCase(@Param("name") String name);

    @Query("SELECT e FROM Exercise e WHERE LOWER(e.description) LIKE LOWER(CONCAT('%', :description, '%'))")
    List<Exercise> findByDescriptionContainingIgnoreCase(@Param("description") String description);

    @Query("SELECT e FROM Exercise e WHERE LOWER(e.description) LIKE LOWER(CONCAT('%', :description, '%')) " +
            "AND e.language.id = :languageId")
    List<Exercise> findByDescriptionContainingIgnoreCaseAndLanguageId(@Param("description") String description,
                                                                      @Param("languageId") Long languageId);

    @Query("SELECT e FROM Exercise e WHERE LOWER(e.description) LIKE LOWER(CONCAT('%', :description, '%')) " +
            "AND e.level = :level")
    List<Exercise> findByDescriptionContainingIgnoreCaseAndLevel(@Param("description") String description,
                                                                 @Param("level") Exercise.Level level);

    @Query("SELECT e FROM Exercise e WHERE LOWER(e.description) LIKE LOWER(CONCAT('%', :description, '%')) " +
            "AND e.language.id = :languageId " +
            "AND e.level = :level")
    List<Exercise> findByDescriptionContainingIgnoreCaseAndLanguageIdAndLevel(@Param("description") String description,
                                                                              @Param("languageId") Long languageId,
                                                                              @Param("level") Exercise.Level level);

    @Query("SELECT e FROM Exercise e WHERE e.language.id = :languageId")
    List<Exercise> findByLanguageId(@Param("languageId") Long languageId);

    @Query("SELECT e FROM Exercise e WHERE e.language.id = :languageId AND e.level = :level")
    List<Exercise> findByLanguageIdAndLevel(@Param("languageId") Long languageId,
                                            @Param("level") Exercise.Level level);

    //--------------Dashboard
    @Query("SELECT e.level, COUNT(e) FROM Exercise e " +
            "WHERE (:languageId IS NULL OR e.language.id = :languageId) " +
            "GROUP BY e.level")
    List<Object[]> countExercisesByLevel(@Param("languageId") Long languageId);


    @Query("SELECT e.status, COUNT(e) FROM Exercise e WHERE (:languageId IS NULL OR e.language.id = :languageId) GROUP BY e.status")
    List<Object[]> countExercisesByStatus(@Param("languageId") Long languageId);


    @Query("SELECT COUNT(e) FROM Exercise e WHERE e.status = 'NEW' AND (:languageId IS NULL OR e.language.id = :languageId)")
    int countNewExercises(@Param("languageId") Long languageId);

    @Query("SELECT COUNT(e) FROM Exercise e WHERE e.status = 'COMPLETED' AND (:languageId IS NULL OR e.language.id = :languageId)")
    int countCompletedExercises(@Param("languageId") Long languageId);

    @Query("SELECT COUNT(e) FROM Exercise e WHERE (:languageId IS NULL OR e.language.id = :languageId)")
    int countTotalExercises(@Param("languageId") Long languageId);

    default double calculateCompletionRate(Long languageId) {
        int total = countTotalExercises(languageId);
        return total == 0 ? 0 : ((double) countCompletedExercises(languageId) / total) * 100;
    }

    @Query("SELECT COUNT(e) FROM Exercise e WHERE e.status = 'UPCOMING' AND (:languageId IS NULL OR e.language.id = :languageId)")
    int countUpcomingWorkouts(@Param("languageId") Long languageId);

    @Query("SELECT COUNT(e) FROM Exercise e WHERE e.status = 'MISSED' AND (:languageId IS NULL OR e.language.id = :languageId)")
    int countMissedWorkouts(@Param("languageId") Long languageId);

    @Query("SELECT e.language.language, COUNT(e) FROM Exercise e GROUP BY e.language.language")
    List<Object[]> countExercisesByLanguage();

    @Query("SELECT pl.language, COUNT(e) FROM Exercise e JOIN e.language pl " +
            "WHERE (:languageId IS NULL OR pl.id = :languageId) " +
            "GROUP BY pl.language")
    List<Object[]> countExercisesByLanguage(@Param("languageId") Long languageId);

    @Query("SELECT DISTINCT e FROM Exercise e " +
            "LEFT JOIN e.exerciseCategories ec " +
            "WHERE (:languageId IS NULL OR e.language.id = :languageId) " +
            "AND (:level IS NULL OR e.level = :level) " +
            "AND (COALESCE(:tagIds, NULL) IS NULL OR EXISTS (" +
            "    SELECT 1 FROM ExerciseCategory ec " +
            "    WHERE ec.exercise = e " +
            "    AND ec.category.id IN :tagIds))")
    Page<Exercise> findByFiltersAndTags(
            @Param("languageId") Long languageId,
            @Param("level") Exercise.Level level,
            @Param("tagIds") List<Long> tagIds,
            Pageable pageable);

    @Query("SELECT COUNT(e) > 0 FROM Exercise e WHERE e.name = :name AND e.language.id = :languageId AND e.id <> :id")
    boolean existsByNameAndLanguageExcludingId(@Param("name") String name, @Param("languageId") Integer languageId, @Param("id") Long id);

    @Query("SELECT COUNT(e) > 0 FROM Exercise e WHERE e.name = :name AND e.language.id = :languageId")
    boolean existsByNameAndLanguage(@Param("name") String name, @Param("languageId") Integer languageId);

    @Query("SELECT DISTINCT e FROM Exercise e " +
            "LEFT JOIN e.exerciseCategories ec " +
            "WHERE (:title IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :title, '%'))) " +
            "AND (:languageId IS NULL OR e.language.id = :languageId) " +
            "AND (:level IS NULL OR e.level = :level) " +
            "AND (COALESCE(:tagIds, NULL) IS NULL OR EXISTS (" +
            "    SELECT 1 FROM ExerciseCategory ec " +
            "    WHERE ec.exercise = e " +
            "    AND ec.category.id IN :tagIds))")
    Page<Exercise> findByTitleAndFiltersAndTags(
            @Param("title") String title,
            @Param("languageId") Long languageId,
            @Param("level") Exercise.Level level,
            @Param("tagIds") List<Long> tagIds,
            Pageable pageable);
}


