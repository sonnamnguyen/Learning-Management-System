package com.example.exercise.service;

import com.example.assessment.model.Assessment;
import com.example.assessment.model.StudentAssessmentAttempt;
import com.example.exercise.model.Exercise;
import com.example.exercise.model.ExerciseSession;
import com.example.exercise.model.StudentExerciseAttempt;
import com.example.exercise.repository.ExerciseSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ExerciseSessionService {
    @Autowired
    private ExerciseSessionRepository exerciseSessionRepository;

    @Autowired
    private StudentExerciseAttemptService studentExerciseAttemptService;

    public ExerciseSession createNewExerciseSession(ExerciseSession exerciseSession) {
        return exerciseSessionRepository.save(exerciseSession);
    }

    public ExerciseSession assessmentExerciseSession(Assessment assessment, LocalDateTime nowUtc, List<Exercise> exercises, StudentAssessmentAttempt attempt) {
        ExerciseSession exerciseSession = new ExerciseSession();
        exerciseSession.setAssessment(assessment);
        exerciseSession.setStartTime(nowUtc);
        this.createNewExerciseSession(exerciseSession);
        // create student_attempt_exercise for each exerciseSession
        for (Exercise exercise : exercises) {
            StudentExerciseAttempt studentExerciseAttempt = new StudentExerciseAttempt();
            studentExerciseAttempt.setAttemptDate(nowUtc);
            studentExerciseAttempt.setExercise_session(exerciseSession);
            studentExerciseAttempt.setSubmitted_exercise(exercise);
//            studentExerciseAttempt.setAttendant_user(attempt.getUser());
            studentExerciseAttempt.setAttendant_email(attempt.getEmail());
            studentExerciseAttempt.setSubmitted_code(exercise.getSetup());
            exerciseSession.getStudentExerciseAttempts().add(studentExerciseAttempt);
            studentExerciseAttemptService.save(studentExerciseAttempt);
        }
        return exerciseSession;
    }

    public double calculateAverageExerciseScoreInAssessment(ExerciseSession exerciseSession){
        List<StudentExerciseAttempt> studentExerciseAttempts = studentExerciseAttemptService.getStudentExerciseAttempts(exerciseSession);
        double averageScore = 0;
        double sum = 0;
        for(StudentExerciseAttempt studentExerciseAttempt : studentExerciseAttempts){
            sum += studentExerciseAttempt.getScore_exercise();
        }
        averageScore = sum / studentExerciseAttempts.size();
        exerciseSession.setAverageScore(averageScore);
        exerciseSession.setStudentExerciseAttempts(studentExerciseAttempts);
        exerciseSessionRepository.save(exerciseSession);
        return averageScore;
    }


    public List<StudentExerciseAttempt> findStudentExerciseAttemptsByAttemptDate(LocalDateTime attemptDate) {
        // Giả sử sai số cho phép là 1 giây
        LocalDateTime start = attemptDate.minusSeconds(1);
        LocalDateTime end = attemptDate.plusSeconds(1);
        Optional<ExerciseSession> exerciseSessionOpt = exerciseSessionRepository.findByStartTimeBetween(start, end);
        if (exerciseSessionOpt.isPresent()) {
            return exerciseSessionOpt.get().getStudentExerciseAttempts();
        }
        return new ArrayList<>();
    }




}
