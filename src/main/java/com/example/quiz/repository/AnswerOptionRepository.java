package com.example.quiz.repository;

import com.example.quiz.model.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {
    @Query("SELECT ao FROM AnswerOption ao WHERE ao.question.id = :questionId AND ao.isCorrect = true")
    AnswerOption findCorrectAnswerByQuestionId(@Param("questionId") Long questionId);

    List<AnswerOption> findByQuestionId(long id);
}
