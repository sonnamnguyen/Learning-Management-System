package com.example.quiz.repository;

import com.example.quiz.model.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    Optional<Quiz> findById(Long id);
    @Query("SELECT m FROM Quiz m WHERE m.name LIKE %:searchQuery%")
    Page<Quiz> searchQuizs(@Param("searchQuery") String searchQuery, Pageable pageable);
    Page<Quiz> findAll(Pageable pageable);
    List<Quiz> findAll();

    List<Quiz> findByIdNot(Long id);
    List<Quiz> findByCourseIdAndIdNot(Long courseId, Long id);


    Quiz save(Quiz quiz);
    boolean existsByName(String name);
    void deleteById(Long id);

    Quiz findByName(String name);

    Set<Quiz> findQuizByCourseName(String name);

    @Query(value = "SELECT attempt_count FROM quiz_participants WHERE quiz_id = :quizId AND user_id = :userId", nativeQuery = true)
    Integer getAttemptCountForUser(@Param("quizId") Long quizId, @Param("userId") Long userId);

    @Modifying
    @Query(value = "UPDATE quiz_participants SET attempt_count = attempt_count + 1 WHERE quiz_id = :quizId AND user_id = :userId", nativeQuery = true)
    void incrementAttemptCount(@Param("quizId") Long quizId, @Param("userId") Long userId);


}
