package com.example.quiz.repository;

import com.example.quiz.model.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends PagingAndSortingRepository<Quiz, Long> {
    @Query("SELECT m FROM Quiz m WHERE m.name LIKE %:searchQuery%")
    Page<Quiz> searchQuizs(@Param("searchQuery") String searchQuery, Pageable pageable);
    Page<Quiz> findAll(Pageable pageable);
    List<Quiz> findAll();
    Optional<Quiz> findById(Long id);

    Quiz save(Quiz quiz);
    boolean existsByName(String name);
    void deleteById(Long id);

    Quiz findByName(String name);
}
