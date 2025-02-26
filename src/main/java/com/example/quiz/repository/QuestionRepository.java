package com.example.quiz.repository;

import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    // Find all questions belonging to a specific quiz
    List<Question> findByQuizzes_Id(Long quizId);

    // Custom query for searching questions by text
    @Query("SELECT q FROM Question q WHERE LOWER(q.questionText) LIKE LOWER(CONCAT('%', :searchQuery, '%'))")
    Page<Question> search(String searchQuery, Pageable pageable);

    // Find questions based on the name of their associated quiz
    @Query("SELECT q FROM Question q JOIN q.quizzes quiz WHERE quiz.id = :quizId")
    public abstract List<Question> findQuestionsByQuizId(@Param("quizId") Long quizId);

    List<Question> findByQuizzes_Name(String name);


    @Query("SELECT q FROM Assessment a JOIN a.questions q LEFT JOIN FETCH q.answerOptions WHERE a.id = :assessmentId")
    List<Question> findQuestionsWithAnswersByAssessmentId(@Param("assessmentId") Long assessmentId);

}