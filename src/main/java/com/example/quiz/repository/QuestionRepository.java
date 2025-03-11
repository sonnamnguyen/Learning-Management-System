package com.example.quiz.repository;

import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    // Find all questions belonging to a specific quiz
    List<Question> findByQuiz(Quiz quiz);

    // Custom query for searching questions by text
    @Query("SELECT q FROM Question q WHERE LOWER(q.questionText) LIKE LOWER(CONCAT('%', :searchQuery, '%'))")
    Page<Question> search(String searchQuery, Pageable pageable);
}

