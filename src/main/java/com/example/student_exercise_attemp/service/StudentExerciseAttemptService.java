package com.example.student_exercise_attemp.service;

import com.example.student_exercise_attemp.model.StudentExerciseAttempt;
import com.example.student_exercise_attemp.repository.StudentExerciseAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StudentExerciseAttemptService {
    @Autowired
    private StudentExerciseAttemptRepository studentExerciseAttemptRepository;

    public void save(StudentExerciseAttempt studentExerciseAttempt) {
        studentExerciseAttemptRepository.save(studentExerciseAttempt);
    }
}
