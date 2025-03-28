package com.example.exercise.service;

import com.example.assessment.model.StudentAssessmentAttempt;
import com.example.exercise.model.Exercise;
import com.example.exercise.model.ExerciseSession;
import com.example.exercise.model.StudentExerciseAttempt;
import com.example.exercise.model.StudentExerciseAttemptResponse;
import com.example.exercise.repository.StudentExerciseAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StudentExerciseAttemptService {
    @Autowired
    private StudentExerciseAttemptRepository studentExerciseAttemptRepository;

    public List<StudentExerciseAttempt> getListAttempt() {
        return studentExerciseAttemptRepository.findAll();
    }

    public void save(StudentExerciseAttempt studentExerciseAttempt) {
        studentExerciseAttemptRepository.save(studentExerciseAttempt);
    }

    public Optional<StudentExerciseAttempt> getStudentExerciseAttemptBySessionAndExercise(ExerciseSession exerciseSession, Exercise exercise) {
        return studentExerciseAttemptRepository.findByExerciseSessionAndSubmittedExercise(exerciseSession, exercise);
    }

    public List<StudentExerciseAttempt> getStudentExerciseAttempts(ExerciseSession exerciseSession) {
        return studentExerciseAttemptRepository.findByExerciseSession(exerciseSession);
    }

    public List<StudentExerciseAttemptResponse> getStudentAttemptsByUser(Long userId) {
        List<StudentExerciseAttempt> attempts = studentExerciseAttemptRepository.getStudentExerciseAttemptByUser(userId);
        List<StudentExerciseAttemptResponse> responseList = new ArrayList<>();

        for (StudentExerciseAttempt attempt : attempts) {
            responseList.add(new StudentExerciseAttemptResponse(
                    attempt.getId(),
                    attempt.getSubmitted_exercise().getName(),
                    attempt.getAttemptDate()
            ));
        }

        return responseList;
    }

    public StudentExerciseAttempt getStudentAttemptById(Long studentAttemptId) {
            StudentExerciseAttempt attempt = studentExerciseAttemptRepository.getStudentExerciseAttempt(studentAttemptId);
            if (attempt == null) {
                throw new NullPointerException("Student attempt not found for ID: " + studentAttemptId);
            }
            return attempt;
    }
}


