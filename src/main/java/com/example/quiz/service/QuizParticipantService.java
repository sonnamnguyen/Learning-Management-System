package com.example.quiz.service;

import com.example.quiz.model.Quiz;
import com.example.quiz.model.QuizParticipant;
import com.example.quiz.repository.QuizParticipantRepository;
import com.example.quiz.repository.QuizRepository;
import com.example.user.User;
import com.example.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuizParticipantService {

    @Autowired
    private QuizParticipantRepository quizParticipantRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private UserRepository userRepository;

    public QuizParticipant attemptQuiz(Long quizId, Long userId){
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<QuizParticipant> attempts = quizParticipantRepository.findAllByQuizAndUser(quiz, user);

        int currentAttempts = attempts.size();

        if(currentAttempts >= quiz.getAttemptLimit()){
            throw new RuntimeException("Attempt Limit reached");
        }

        QuizParticipant participant = new QuizParticipant();
        participant.setQuiz(quiz);
        participant.setUser(user);
        participant.setAttemptUsed(currentAttempts + 1);
        participant.setTimeStart(LocalDateTime.now());

        return quizParticipantRepository.save(participant);
    }

    public List<QuizParticipant> getParticipantsByQuiz(Long quizId) {
        return quizParticipantRepository.findAllByQuizId(quizId);
    }

//    public List<QuizParticipant> searchByName(String firstName, String lastName) {
//        return quizParticipantRepository.findByFirstNameOrLastName(firstName, lastName);
//    }
}
