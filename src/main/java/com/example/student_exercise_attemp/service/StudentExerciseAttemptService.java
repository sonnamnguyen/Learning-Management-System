package com.example.student_exercise_attemp.service;

import com.example.student_exercise_attemp.model.Exercise;
import com.example.student_exercise_attemp.model.ExerciseSession;
import com.example.student_exercise_attemp.model.StudentExerciseAttempt;
import com.example.student_exercise_attemp.repository.StudentExerciseAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentExerciseAttemptService {
    @Autowired
    private StudentExerciseAttemptRepository studentExerciseAttemptRepository;

    public void save(StudentExerciseAttempt studentExerciseAttempt) {
        studentExerciseAttemptRepository.save(studentExerciseAttempt);
    }

    public Optional<StudentExerciseAttempt> getStudentExerciseAttemptBySessionAndExercise(ExerciseSession exerciseSession, Exercise exercise) {
        return studentExerciseAttemptRepository.findByExerciseSessionAndSubmittedExercise(exerciseSession, exercise);
    }

    public List<StudentExerciseAttempt> getStudentExerciseAttempts(ExerciseSession exerciseSession) {
        return studentExerciseAttemptRepository.findByExerciseSession(exerciseSession);
    }

        public Page<StudentExerciseAttempt> getStudentAttemptsByUser(Long userId, int page, int size) {
            Pageable pageable = PageRequest.of(page, size);
            return studentExerciseAttemptRepository.getStudentExerciseAttemptByUser(userId, pageable);
        }
    }


