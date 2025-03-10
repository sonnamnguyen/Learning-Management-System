package com.example.quiz.repository;

import com.example.quiz.model.Quiz;
import com.example.quiz.model.QuizParticipant;
import com.example.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizParticipantRepository extends JpaRepository<QuizParticipant, Long> {
    List<QuizParticipant> findByQuizId(Long quizId);
    List<QuizParticipant> findByUserId(Long userId);

    Optional<QuizParticipant> findByQuizAndUser(Quiz quiz, User user);

    List<QuizParticipant> findAllByQuizAndUser(Quiz quiz, User user);

    List<QuizParticipant> findAllByQuizId(Long quizId);
    QuizParticipant findByQuizIdAndUserId(Long quizId, Long userId);


//    @Query("SELECT qp FROM quiz_participants qp WHERE " +
//            "(:firstName IS NULL OR LOWER(qp.app_user.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) AND " +
//            "(:lastName IS NULL OR LOWER(qp.app_user.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))")
//    List<QuizParticipant> findByFirstNameOrLastName(
//            @Param("firstName") String firstName,
//            @Param("lastName") String lastName);

}
