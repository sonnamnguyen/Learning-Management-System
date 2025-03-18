package com.example.quiz.repository;

import com.example.quiz.model.Answer;
import com.example.quiz.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    @Transactional
    void deleteAllByQuestion(Question question);
}
